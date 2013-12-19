package de.uniaugsburg.isse.experiments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import de.uniaugsburg.isse.RandomManager;
import de.uniaugsburg.isse.abstraction.AvppCreator;
import de.uniaugsburg.isse.abstraction.AvppGraph;
import de.uniaugsburg.isse.abstraction.AvppLeafNode;
import de.uniaugsburg.isse.abstraction.CplexAvppGraphExporter;
import de.uniaugsburg.isse.abstraction.CplexExporter;
import de.uniaugsburg.isse.abstraction.GeneralAbstraction;
import de.uniaugsburg.isse.abstraction.SamplingAbstraction;
import de.uniaugsburg.isse.abstraction.TemporalAbstraction;
import de.uniaugsburg.isse.abstraction.types.Interval;
import de.uniaugsburg.isse.data.PowerplantReader;
import de.uniaugsburg.isse.data.ResidualLoadReader;
import de.uniaugsburg.isse.experiments.ExperimentParameterLiterals.PowerplantType;
import de.uniaugsburg.isse.powerplants.PowerPlantData;
import de.uniaugsburg.isse.powerplants.PowerPlantState;
import de.uniaugsburg.isse.powerplants.RandomConstraintBuilder;
import de.uniaugsburg.isse.solver.AbstractModel;
import de.uniaugsburg.isse.solver.AbstractSolver;
import de.uniaugsburg.isse.solver.AbstractSolverFactory;
import de.uniaugsburg.isse.solver.SolverFacade;
import de.uniaugsburg.isse.timer.Timer;
import de.uniaugsburg.isse.timer.TimerCategory;
import de.uniaugsburg.isse.util.AbstractionParameterLiterals;
import de.uniaugsburg.isse.util.Utils;

/**
 * This class represents one particular experiment of scheduling an AVPP (the top level AVPP) - can be used to set the
 * balancing level; After an AVPP is formed, new P_min and P_max level for each concrete plant are found by looking at
 * the maximal and minimal residual load to meed
 * 
 * Different possible experiments include
 * 
 * -> Sum(P_Max) >= Max_Residual_Load && Sum(P_min) <= Min_Residual_Load i.e. all residual loads can in principle be met
 * by the AVPP; this should be achieved by selecting P_max values with µ = Max(Load) / #plants
 * 
 * @author Alexander Schiendorfer
 * 
 */
public class Experiment {
	private int timeHorizon;
	private int experimentHorizon;
	private int samplingPoints = 10;
	private Double[] residualLoad;
	private ExperimentParameterLiterals.NumberPlants numberOfPlants;
	private int plantsPerAvpp;
	private int avppsPerAvpp;
	private AvppGraph graph;
	private Map<String, PowerPlantState> allStates;
	private Map<String, PowerPlantState> concretePlantStates;
	private Map<String, PowerPlantState> initialStates;
	private SolverFacade solverFacade;
	private AbstractSolverFactory solverFactory;
	private List<PowerPlantData> allPlants;
	private double maxProduction;
	private StringBuilder bookmarkBuilder;
	private boolean useSamplingAbstraction;
	private Timer timer;
	private ExperimentStatistics statistics;

	public ExperimentStatistics getStatistics() {
		return statistics;
	}

	public void setStatistics(ExperimentStatistics statistics) {
		this.statistics = statistics;
	}

	private Map<String, Double> loadInputs; // desired target
	private Map<String, Double> actualLoads; // actual produced load

	private long HierarchyRandomSeed = 1337;
	private long AvppsRandomSeed = 1337;
	private long InitialStatesSeed = 1337;
	private boolean useIsoSplit;
	private double maxLoad;
	private double minLoad;
	private final double jitter = 0.001; // matches CPLEX file

	private Properties originatingProperties; // all parameters set here
	private long timestamp;

	public Properties getOriginatingProperties() {
		return originatingProperties;
	}

	public void setOriginatingProperties(Properties originatingProperties) {
		this.originatingProperties = originatingProperties;
	}

	public void prepare() {
		RandomManager.initialize(AvppsRandomSeed);
		// 1. load pps from file
		String fileNameGas = "data/" + ExperimentParameterLiterals.getFileName(numberOfPlants, PowerplantType.GAS);
		String fileNameBio = "data/" + ExperimentParameterLiterals.getFileName(numberOfPlants, PowerplantType.BIO);

		PowerplantReader reader = new PowerplantReader();
		Collection<PowerPlantData> bioPlants = reader.readPlants(fileNameBio);
		Collection<PowerPlantData> gasPlants = reader.readPlants(fileNameGas);

		// 2. equip power plants with random constraints/ change rate has to be
		// set
		RandomConstraintBuilder.addChangeConstraints(bioPlants);
		RandomConstraintBuilder.addChangeConstraints(gasPlants);

		// add stop time constraints to bio plants
		RandomConstraintBuilder.addStopTimeConstraints(bioPlants);

		allPlants = new ArrayList<PowerPlantData>(bioPlants.size() + gasPlants.size());
		allPlants.addAll(bioPlants);
		allPlants.addAll(gasPlants);
		// shuffle plants
		Collections.shuffle(allPlants, RandomManager.getRandom());

		RandomManager.initialize(HierarchyRandomSeed);
		AvppCreator ac = new AvppCreator();
		ac.setPlantsPerAvpp(plantsPerAvpp);
		ac.setAvppsPerAvpp(avppsPerAvpp);
		ac.setIsoSplit(useIsoSplit);
		graph = ac.createGraph(allPlants);
		ac.printGraph(graph);

		// 4. load residual load from file
		ResidualLoadReader rlr = new ResidualLoadReader();
		residualLoad = rlr.readLoad(ExperimentParameterLiterals.residualLoadFile);

		maxProduction = 0.0;
		for (PowerPlantData pd : allPlants) {
			maxProduction += pd.getPowerBoundaries().max;
		}

		// norming residual load
		maxLoad = Double.NEGATIVE_INFINITY;
		minLoad = Double.POSITIVE_INFINITY;
		for (int i = 0; i < residualLoad.length; ++i) {
			if (residualLoad[i] < minLoad)
				minLoad = residualLoad[i];
			if (residualLoad[i] > maxLoad)
				maxLoad = residualLoad[i];
		}

		if (maxProduction < minLoad) {
			// let max production be 10% greater than maxLoad
			double newMax = maxProduction / 1.1;
			double percMin = minLoad / maxLoad;
			double newMin = newMax * percMin;
			double rangeOrig = maxLoad - minLoad, rangeNew = newMax - newMin;

			for (int i = 0; i < residualLoad.length; ++i) {
				residualLoad[i] = newMin + rangeNew * ((residualLoad[i] - minLoad) / rangeOrig);
			}
			minLoad = newMin;
			maxLoad = newMax;
		}

		// residual load length must be ge than experiment horizon
		if (residualLoad.length < experimentHorizon)
			throw new RuntimeException("HALT! Experiment horizon is longer than available consumption data");

		RandomManager.initialize(InitialStatesSeed);
		initialStates = new HashMap<String, PowerPlantState>();
		for (PowerPlantData pd : allPlants) {
			initialStates.put(pd.getName(), getRandomInitialState(pd));
		}

		// put some initial states also for AVPPs
		initializeStates(graph);
	}

	private void initializeStates(AvppGraph node) {
		if (node instanceof AvppLeafNode)
			return;

		double production = 0.0;
		for (AvppGraph child : node.getChildren()) {
			initializeStates(child);
			PowerPlantState childState = initialStates.get(child.getPowerPlant().getName());
			production += childState.getPower().min;
		}

		PowerPlantState avppState = new PowerPlantState();
		avppState.setPower(new Interval<Double>(production));
		boolean isRunning = production > 0;
		avppState.setRunning(new Interval<Boolean>(production > 0));
		avppState.setConsRunning(new Interval<Integer>(isRunning ? 1 : 0));
		avppState.setConsStopping(new Interval<Integer>(isRunning ? 0 : 1));
		avppState.setData(node.getPowerPlant());
		initialStates.put(node.getPowerPlant().getName(), avppState);
	}

	private void updateStates(AvppGraph node) {
		if (node instanceof AvppLeafNode)
			return;

		double production = 0.0;
		for (AvppGraph child : node.getChildren()) {
			updateStates(child);
			PowerPlantState childState = allStates.get(child.getPowerPlant().getName());
			production += childState.getPower().min;
			PowerPlantData pdata = childState.getData();

			if (!pdata.isAVPP()) {
				pdata.put(AbstractionParameterLiterals.CONSRUNNING_INIT, childState.getConsRunning().min.toString());
				pdata.put(AbstractionParameterLiterals.CONSSTOPPING_INIT, childState.getConsStopping().min.toString());
				pdata.put(AbstractionParameterLiterals.POWER_INIT, Double.toString(childState.getPower().min));
			}
		}

		PowerPlantState avppState = allStates.get(node.getPowerPlant().getName());
		avppState.setPower(new Interval<Double>(production));
		avppState.setRunning(new Interval<Boolean>(production > 0));
	}

	private HashMap<String, PowerPlantState> getDeepStateCopy(Map<String, PowerPlantState> initialStates2) {
		HashMap<String, PowerPlantState> deepCopy = new HashMap<String, PowerPlantState>(initialStates2);
		for (Entry<String, PowerPlantState> entry : initialStates2.entrySet()) {
			PowerPlantState state = entry.getValue().copy();
			deepCopy.put(entry.getKey(), state);
		}
		return deepCopy;
	}

	private PowerPlantState getRandomInitialState(PowerPlantData pd) {
		PowerPlantState ps = new PowerPlantState();
		if (RandomManager.getBoolean(.5)) {
			ps.setRunning(new Interval<Boolean>(true, true));
			ps.setConsRunning(new Interval<Integer>(1));
			ps.setConsStopping(new Interval<Integer>(0));
			double initPower = RandomManager.getDouble(pd.getPowerBoundaries().min, pd.getPowerBoundaries().max);
			ps.setPower(new Interval<Double>(initPower));
		} else {
			ps.setRunning(new Interval<Boolean>(false));
			ps.setConsRunning(new Interval<Integer>(0));
			ps.setConsStopping(new Interval<Integer>(1));
			ps.setPower(new Interval<Double>(0.0));
		}
		ps.setData(pd);
		return ps;
	}

	public void run() {
		timestamp = new Date().getTime();
		timer = new Timer();
		statistics.addTimeStamp(timestamp);

		prepare();
		String s = preanalyze();
		System.out.println(s);

		reset();
		timer.tick(TimerCategory.TOTAL_RUNTIME_REGIOCENTRAL.id);
		runRegioCentral();
		long elapsed = timer.tock(TimerCategory.TOTAL_RUNTIME_REGIOCENTRAL.id);
		statistics.addTotalRuntimeRegioCentral(elapsed);
		reportRegioCentral();
		reset();
		timer.tick(TimerCategory.TOTAL_RUNTIME_CENTRAL.id);
		runCentralized();
		elapsed = timer.tock(TimerCategory.TOTAL_RUNTIME_CENTRAL.id);
		statistics.addTotalRuntimeCentral(elapsed);
		reportCentralized();

		String report = s + "\n" + statistics.writeStatistics() + "\n" + writeProperties(originatingProperties);
		System.out.println(report);

		java.io.File statsFile = new java.io.File("results/stats" + (timestamp));
		Utils.writeFile(statsFile.getAbsolutePath(), report);
		statistics.reset(); // prepare for another run() call
	}

	private String writeProperties(Properties originatingProperties2) {
		StringBuilder sb = new StringBuilder();
		for (Object key : originatingProperties2.keySet()) {
			String strKey = (String) key;
			sb.append(strKey + "=" + originatingProperties2.getProperty(strKey) + "\n");
		}
		return sb.toString();
	}

	private String preanalyze() {

		StringBuilder sb = new StringBuilder();
		sb.append("-----------------------\n");
		sb.append("Minimal residual load: " + minLoad + "\n");
		sb.append("Maximal residual load: " + maxLoad + "\n");
		sb.append("Maximal production: " + maxProduction + "\n");
		sb.append("Number of plants: " + allPlants.size() + "\n");
		sb.append("AVPP graph depth: " + graph.getHeight() + "\n");
		sb.append("-----------------------\n");
		return sb.toString();
	}

	private void reportRegioCentral() {
		String fileName = "results/regio-central-" + timestamp + ".csv";
		Utils.writeFile(fileName, bookmarkBuilder.toString());
	}

	private void reportCentralized() {
		String fileName = "results/central-" + timestamp + ".csv";
		Utils.writeFile(fileName, bookmarkBuilder.toString());
	}

	private void reset() {
		allStates = getDeepStateCopy(initialStates);
		bookmarkBuilder = new StringBuilder();
	}

	/**
	 * Recursively performs abstraction of constraint models
	 * 
	 * @param node
	 */
	private void performAbstraction(AvppGraph node) {
		if (node instanceof AvppLeafNode) // nothing to do
			return;

		// postfix traversal - first abstract children
		List<PowerPlantData> childPlants = new ArrayList<PowerPlantData>(node.getChildren().size());
		for (AvppGraph child : node.getChildren()) {
			performAbstraction(child);
			childPlants.add(child.getPowerPlant());
		}

		// now children have all their feasible regions etc -> do that for me as
		// well
		GeneralAbstraction ga = new GeneralAbstraction();
		node.setGeneralAbstraction(ga);
		ga.setPowerPlants(childPlants);
		ga.perform();
		PowerPlantData nodeData = node.getPowerPlant();
		nodeData.setFeasibleRegions(ga.getFeasibleRegions());
		nodeData.setHoles(ga.getHoles());

		if (useSamplingAbstraction) {
			SamplingAbstraction sa = new SamplingAbstraction(ga.getFeasibleRegions(), ga.getHoles());

			// get concrete cplex solver
			AbstractSolver solver = solverFactory.createSolver();
			AbstractModel model = solverFactory.createModel();
			model.setPlantData(node.getPowerPlant(), node.getChildrenPlantData());
			Collection<String> dexprs = new ArrayList<String>(2);

			dexprs.add(AbstractionParameterLiterals.DEXP_POWER + "Init = " + AbstractionParameterLiterals.DEXP_POWER + "[0]");
			dexprs.add(AbstractionParameterLiterals.DEXP_POWER + "Succ = " + AbstractionParameterLiterals.DEXP_POWER + "[1]");
			model.addDecisionExpressions(dexprs);
			solver.setModel(model);

			Collection<String> objectives = new ArrayList<String>(1);
			objectives.add(AbstractionParameterLiterals.DEXP_POWER + "Succ");

			sa.setMaximizationDecisionExpressions(objectives);
			sa.setMinimizationDecisionExpressions(objectives);
			sa.setSolver(solver);

			sa.perform(samplingPoints);

			// reintegrate sampling points
			node.getPowerPlant().setPositiveDelta(sa.getPiecewiseLinearFunction(AbstractionParameterLiterals.DEXP_POWER + "Succ", false));

			node.getPowerPlant().setNegativeDelta(sa.getPiecewiseLinearFunction(AbstractionParameterLiterals.DEXP_POWER + "Succ", true));
		}

	}

	private void performTemporalAbstraction(AvppGraph node) {
		if (node instanceof AvppLeafNode) // nothing to do
			return;

		List<PowerPlantData> childPlants = new ArrayList<PowerPlantData>(node.getChildren().size());
		for (AvppGraph child : node.getChildren()) {
			performTemporalAbstraction(child);
			childPlants.add(child.getPowerPlant());
		}

		TemporalAbstraction ta = new TemporalAbstraction();
		node.setTemporalAbstraction(ta);
		ta.setPowerPlants(childPlants);
		ta.setGeneralHoles(node.getGeneralAbstraction().getHoles());
		ta.setGeneralFeasibleRegions(node.getGeneralAbstraction().getFeasibleRegions());

		ta.perform(timeHorizon);
		PowerPlantData pd = node.getPowerPlant();
		pd.setAllFeasibleRegions(ta.getAllFeasibleRegions());
		pd.setAllHoles(ta.getAllHoles());
	}

	private void runRegioCentral() {
		System.out.println("========================= REGIO CENTRAL ===================");
		CplexExporter exporter = new CplexExporter();
		CplexAvppGraphExporter graphExporter = new CplexAvppGraphExporter(exporter);
		exporter.setTimeHorizon(getTimeHorizon());
		exporter.setResidualLoad(residualLoad);
		exporter.setUseSamplingAbstraction(true);
		exporter.setUseGeneralFeasibleRegions(true);
		exporter.setUseCompleteRange(false); // starts at 1
		graphExporter.createRegionalModels(graph);

		extractConcretePlants();
		// perform general abstraction and sampling abstraction bottom up
		timer.tick(TimerCategory.ABSTRACTION_RUNTIME.id);
		performAbstraction(graph);
		long elapsed = timer.tock(TimerCategory.ABSTRACTION_RUNTIME.id);

		solverFacade.setTimeLimit(60);
		solverFacade.setUseInitialSolution(false);
		statistics.setAbstractionRuntime(statistics.getAbstractionRuntime() + elapsed);
		statistics.setFixedAbstractionRuntime(elapsed);
		// solve model by creating decentralized models using avpps
		for (int t = 0; t < experimentHorizon - timeHorizon; ++t) {
			// update states from concrete power plants
			updateStates(graph);
			resetStatsMaps();

			timer.tick(TimerCategory.RUNTIME_REGIOCENTRAL_TS.id);
			// perform temporal abstraction with current state
			timer.tick(TimerCategory.ABSTRACTION_RUNTIME.id);
			performTemporalAbstraction(graph);
			elapsed = timer.tock(TimerCategory.ABSTRACTION_RUNTIME.id);
			statistics.setAbstractionRuntime(statistics.getAbstractionRuntime() + elapsed);
			statistics.addVariableAbstractionTime(elapsed);

			Double[] residualLoadPiece = getResidualLoad(residualLoad, t, timeHorizon);
			// calls recursive solving algorithm
			solveRecursively(graph, graphExporter, t, residualLoadPiece, 0);

			// get aggregated production and report it
			double totalProduction = 0.0;
			for (PowerPlantState state : concretePlantStates.values()) {
				totalProduction += state.getPower().min;
			}
			long elapsedStep = timer.tock(TimerCategory.RUNTIME_REGIOCENTRAL_TS.id);
			statistics.addRegioCentralRuntimePerStep(elapsedStep);
			// manage abstraction error
			statistics.reportAbstractionError(graph.getPowerPlant().getName(), loadInputs, actualLoads);
			bookmark(t, residualLoad[t], totalProduction);
			statistics.addToplevelViolationRegioCentral(totalProduction, residualLoad[t]);
		}
		// compare overall performance
	}

	private void resetStatsMaps() {
		loadInputs = new HashMap<String, Double>();
		actualLoads = new HashMap<String, Double>();
	}

	private void solveRecursively(AvppGraph node, CplexAvppGraphExporter graphExporter, int t, Double[] residualLoadPerNode, long elapsedUntil) {
		// first solve, then call recursively for children
		timer.tick(TimerCategory.AVPP_TIME.id);
		loadInputs.put(node.getPowerPlant().getName(), residualLoadPerNode[0]);

		Map<String, PowerPlantState> localStates = getStates(node, allStates);

		// print top level model
		String modelFile = "generated/" + node.getPowerPlant().getName() + ".mod";
		Utils.writeFile(modelFile, node.getCplexModel());
		String generalAbstractionData = graphExporter.getGeneralAbstractionData(node);
		String temporalAbstractionData = graphExporter.getTemporalAbstractionData(node);

		String piecewiseData = "";
		if (useSamplingAbstraction) {
			piecewiseData = graphExporter.getExporter().writePiecewiseLinearData(node.getChildrenPlantData());
		}

		String generalSelfAbstraction = graphExporter.getExporter().getTotalProductionAbstractionString(node.getGeneralAbstraction().getHoles(),
				node.getGeneralAbstraction().getFeasibleRegions());
		String residualLoadStr = graphExporter.getExporter().createResidualLoad(residualLoadPerNode);

		String initState = graphExporter.getExporter().createInitStateData(localStates);

		String dataFile = "generated/" + node.getPowerPlant().getName() + "_" + t + ".dat";
		String dataContent = generalAbstractionData + "\n" + temporalAbstractionData + "\n" + residualLoadStr + "\n" + piecewiseData + "\n" + initState + "\n"
				+ generalSelfAbstraction;
		Utils.writeFile(dataFile, dataContent);

		solverFacade.solve(modelFile, dataFile);
		// extract values for t=1 to be the next init
		if (!solverFacade.isSolved()) {
			// try simplified
			solverFacade.setSimplified(true);
			solverFacade.cleanup();
			solverFacade.solve(modelFile, dataFile);
			solverFacade.setSimplified(false);
			if (!solverFacade.isSolved())
				throw new RuntimeException("Model " + modelFile + " / " + dataFile + " could not be solved!");
		}
		for (Entry<String, PowerPlantState> state : localStates.entrySet()) {
			double power = solverFacade.getProduction(state.getKey(), 1);
			if (power < jitter)
				power = 0;
			boolean running = solverFacade.getRunning(state.getKey(), 1);

			state.getValue().setPower(new Interval<Double>(power));
			state.getValue().setRunning(new Interval<Boolean>(running));
		}

		// get total production for comparison with input
		double totalPower = solverFacade.getTotalProduction(1);
		actualLoads.put(node.getPowerPlant().getName(), totalPower);
		// solve children
		HashMap<String, Double[]> childLoads = new HashMap<String, Double[]>(node.getChildren().size() * 2);

		// first iterate to get residual loads, then call solver again
		for (AvppGraph childNode : node.getChildren()) {
			if (!(childNode instanceof AvppLeafNode)) {
				// extract powers
				Double[] remainingLoads = new Double[timeHorizon];
				for (int t_ = 0; t_ < timeHorizon; ++t_) {
					remainingLoads[t_] = solverFacade.getProduction(childNode.getPowerPlant().getName(), t_ + 1);
				}
				childLoads.put(childNode.getPowerPlant().getName(), remainingLoads);

			}
		}

		long elapsed = timer.tock(TimerCategory.AVPP_TIME.id);

		statistics.addAvppRuntime(elapsed);
		// solve for child TODO maybe with new thread here
		// cleanup solver here
		solverFacade.cleanup();

		// delete model and dat file
		Utils.deleteFile(modelFile);
		Utils.deleteFile(dataFile);
		for (AvppGraph childNode : node.getChildren()) {
			if (!(childNode instanceof AvppLeafNode)) {
				solveRecursively(childNode, graphExporter, t, childLoads.get(childNode.getPowerPlant().getName()), elapsedUntil + elapsed);
			}
		}
		// report serial path
		statistics.reportSerialPath(elapsed + elapsedUntil);

	}

	private Map<String, PowerPlantState> getStates(AvppGraph node, Map<String, PowerPlantState> allStates2) {
		Map<String, PowerPlantState> localStates = new HashMap<String, PowerPlantState>(node.getChildren().size() * 2);

		for (AvppGraph child : node.getChildren()) {
			localStates.put(child.getPowerPlant().getName(), allStates2.get(child.getPowerPlant().getName()));
		}
		return localStates;
	}

	private Double[] getResidualLoad(Double[] residualLoadParam, int t, int timeHorizon) {
		Double[] residualLoad2 = new Double[timeHorizon];
		for (int inc = 0; inc < timeHorizon; ++inc) {
			residualLoad2[inc] = residualLoadParam[t + inc];
		}
		return residualLoad2;
	}

	private void runCentralized() {
		CplexExporter exporter = new CplexExporter();
		exporter.setTimeHorizon(getTimeHorizon());
		exporter.setResidualLoad(residualLoad);
		exporter.setMaximalUpperBound(maxProduction);
		CplexAvppGraphExporter graphExporter = new CplexAvppGraphExporter(exporter);
		extractConcretePlants();
		String s = graphExporter.createSingleModel(graph);

		String modelFile = "generated/file001.mod";
		Utils.writeFile(modelFile, s);
		solverFacade.setTimeLimit(1800); // 30 min initial time limit
		solverFacade.setPresolve(true);
		solverFacade.setUseInitialSolution(false);
		// main loop
		for (int t = 0; t < experimentHorizon - timeHorizon; ++t) {
			timer.tick(TimerCategory.RUNTIME_CENTRAL_TS.id);
			// create new .dat file with the current states
			String initState = exporter.createInitStateData(concretePlantStates);
			Double[] residualLoadPiece = getResidualLoad(residualLoad, t, timeHorizon);
			String residualLoadStr = "\n" + exporter.createResidualLoad(residualLoadPiece);
			String dataFile = "generated/central_state" + t + ".mod";

			Utils.writeFile(dataFile, initState + residualLoadStr);

			// run model with new .dat file
			solverFacade.solve(modelFile, dataFile);
			// extract values for t=1 to be the next init
			if (solverFacade.isSolved()) {
				for (Entry<String, PowerPlantState> state : concretePlantStates.entrySet()) {
					double power = solverFacade.getProduction(state.getKey(), 1);
					boolean running = solverFacade.getRunning(state.getKey(), 1);

					if (power < jitter)
						power = 0;
					state.getValue().setPower(new Interval<Double>(power));
					state.getValue().setRunning(new Interval<Boolean>(running));
				}
				statistics.reportUnsolvedAllocation(0.0);
			} // else everything stays the same
			else {
				statistics.reportUnsolvedAllocation(1.0);
			}
			// store quantities of interest (cost, violation ...) for evaluation
			double totalProduction = getTotalProduction(concretePlantStates);
			long elapsed = timer.tock(TimerCategory.RUNTIME_CENTRAL_TS.id);
			statistics.addCentralRuntimePerStep(elapsed);
			bookmark(t, residualLoad[t], totalProduction);
			statistics.addToplevelViolationCentral(totalProduction, residualLoad[t]);
		}
	}

	private void extractConcretePlants() {
		concretePlantStates = new HashMap<String, PowerPlantState>(allStates.size());
		for (Entry<String, PowerPlantState> state : allStates.entrySet())
			if (!state.getValue().getData().isAVPP())
				concretePlantStates.put(state.getKey(), state.getValue());

	}

	private void bookmark(int t, Double residualLoad, double totalProduction) {
		bookmarkBuilder.append(t + ";" + residualLoad + ";" + totalProduction + "\n");
	}

	private double getTotalProduction(Map<String, PowerPlantState> concretePlantStates2) {
		double production = 0.0;
		for (PowerPlantState state : concretePlantStates2.values()) {
			production += state.getPower().min;
		}
		return production;
	}

	public int getTimeHorizon() {
		return timeHorizon;
	}

	public void setTimeHorizon(int timeHorizon) {
		this.timeHorizon = timeHorizon;
	}

	public ExperimentParameterLiterals.NumberPlants getNumberOfPlants() {
		return numberOfPlants;
	}

	public void setNumberOfPlants(ExperimentParameterLiterals.NumberPlants numberOfPlants) {
		this.numberOfPlants = numberOfPlants;
	}

	public int getPlantsPerAvpp() {
		return plantsPerAvpp;
	}

	public void setPlantsPerAvpp(int plantsPerAvpp) {
		this.plantsPerAvpp = plantsPerAvpp;
	}

	public int getExperimentHorizon() {
		return experimentHorizon;
	}

	public void setExperimentHorizon(int experimentHorizon) {
		this.experimentHorizon = experimentHorizon;
	}

	public SolverFacade getSolverFacade() {
		return solverFacade;
	}

	public void setSolverFacade(SolverFacade solver) {
		this.solverFacade = solver;
	}

	public AbstractSolverFactory getSolverFactory() {
		return solverFactory;
	}

	public void setSolverFactory(AbstractSolverFactory solverFactory) {
		this.solverFactory = solverFactory;
	}

	public boolean isUseSamplingAbstraction() {
		return useSamplingAbstraction;
	}

	public void setUseSamplingAbstraction(boolean useSamplingAbstraction) {
		this.useSamplingAbstraction = useSamplingAbstraction;
	}

	public int getSamplingPoints() {
		return samplingPoints;
	}

	public void setSamplingPoints(int samplingPoints) {
		this.samplingPoints = samplingPoints;
	}

	public void setIsoSplit(boolean useIsoSplit) {
		this.useIsoSplit = useIsoSplit;
	}

	public long getHierarchyRandomSeed() {
		return HierarchyRandomSeed;
	}

	public void setHierarchyRandomSeed(long hierarchyRandomSeed) {
		HierarchyRandomSeed = hierarchyRandomSeed;
	}

	public long getAvppsRandomSeed() {
		return AvppsRandomSeed;
	}

	public void setAvppsRandomSeed(long avppsRandomSeed) {
		AvppsRandomSeed = avppsRandomSeed;
	}

	public long getInitialStatesSeed() {
		return InitialStatesSeed;
	}

	public void setInitialStatesSeed(long initialStatesSeed) {
		InitialStatesSeed = initialStatesSeed;
	}

	public int getAvppsPerAvpp() {
		return avppsPerAvpp;
	}

	public void setAvppsPerAvpp(int avppsPerAvpp) {
		this.avppsPerAvpp = avppsPerAvpp;
	}
}
