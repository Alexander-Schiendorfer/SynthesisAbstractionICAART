package de.uniaugsburg.isse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import de.uniaugsburg.isse.abstraction.CplexExporter;
import de.uniaugsburg.isse.abstraction.GeneralAbstraction;
import de.uniaugsburg.isse.abstraction.SamplingAbstraction;
import de.uniaugsburg.isse.abstraction.TemporalAbstraction;
import de.uniaugsburg.isse.abstraction.types.Interval;
import de.uniaugsburg.isse.abstraction.types.PiecewiseLinearFunction;
import de.uniaugsburg.isse.constraints.BoundsConstraint;
import de.uniaugsburg.isse.constraints.Constraint;
import de.uniaugsburg.isse.constraints.GraduallyOffConstraint;
import de.uniaugsburg.isse.constraints.RateOfChangeConstraint;
import de.uniaugsburg.isse.cplex.CPLEXSolverFacade;
import de.uniaugsburg.isse.powerplants.PowerPlantData;
import de.uniaugsburg.isse.powerplants.PowerPlantState;
import de.uniaugsburg.isse.solver.AbstractModel;
import de.uniaugsburg.isse.solver.AbstractSolver;
import de.uniaugsburg.isse.solver.CplexModel;
import de.uniaugsburg.isse.solver.CplexSolver;
import de.uniaugsburg.isse.solver.SolverFacade;
import de.uniaugsburg.isse.util.AbstractionParameterLiterals;
import de.uniaugsburg.isse.util.PowerPlantUtil;
import de.uniaugsburg.isse.util.Utils;

public class SamplingAbstractionTest {

	private Collection<PowerPlantData> children;
	private PowerPlantData avpp;
	private GeneralAbstraction ga;
	private AbstractSolver solver;
	private Map<String, PowerPlantState> singletonState;

	private Map<String, PowerPlantState> allStates;

	@Before
	public void setUp() throws Exception {
		avpp = new PowerPlantData("AVPP1");
		avpp.setAVPP(true);
		avpp.put(AbstractionParameterLiterals.POWER_INIT, "265.0");
		avpp.put(AbstractionParameterLiterals.CONSRUNNING_INIT, "1");
		avpp.put(AbstractionParameterLiterals.CONSSTOPPING_INIT, "0");

		PowerPlantState avppState = new PowerPlantState();
		avppState.setConsRunning(new Interval<Integer>(1));
		avppState.setConsStopping(new Interval<Integer>(0));
		avppState.setPower(new Interval<Double>(265.0));

		// has three plants available
		allStates = new HashMap<String, PowerPlantState>();
		PowerPlantData pp1 = PowerPlantUtil.getPowerPlant("PP1", 50.0, 100.0, 0.15);
		PowerPlantUtil.addState(allStates, pp1);
		
		// pp1 gets two soft constraints
		// c1_rateOfChangeOpt 7%
		Constraint c1RateOpt = new RateOfChangeConstraint(pp1,0.07);
		// c2_rateOfChangePref 10%
		Constraint c2RatePref = new RateOfChangeConstraint(pp1,0.1);
		
		// TODO use cr
		c1RateOpt.setWeight(2); c2RatePref.setWeight(1);
		
		c1RateOpt.setSoft(true); c2RatePref.setSoft(true);
		pp1.addConstraint(c1RateOpt);pp1.addConstraint(c2RatePref);
		
		PowerPlantData pp2 = PowerPlantUtil.getPowerPlant("PP2", 15.0, 35.0, 0.125);
		PowerPlantUtil.addState(allStates, pp2);
		
		// pp2 gets three soft constraints
		//c1_economically_optimal: production[t] >= 22 && production[t] <= 25;

	    //c2_economically_good: production[t] >= 20 && production[t] <= 30;
	    //c3_economically_acc: production[t] >= 18 && production[t] <= 33;
	    
		Constraint c1EconomicallyOptimal = new BoundsConstraint(pp2, 22.0, 25.0);
		Constraint c2EconomicallyGood = new BoundsConstraint(pp2, 20.0, 30.0);
		Constraint c3EconomicallyAcc = new BoundsConstraint(pp2, 18.0, 33.0);
		
		c1EconomicallyOptimal.setSoft(true); c2EconomicallyGood.setSoft(true); c3EconomicallyAcc.setSoft(true);
		// TODO use CRs
		c1EconomicallyOptimal.setWeight(4);
		c2EconomicallyGood.setWeight(2);
		c3EconomicallyAcc.setWeight(1);
		
		pp2.addConstraint(c1EconomicallyOptimal); pp2.addConstraint(c2EconomicallyGood); pp2.addConstraint(c3EconomicallyAcc);
		
		
		PowerPlantData pp3 = PowerPlantUtil.getPowerPlant("PP3", 200.0, 400.0, 0.2);
		PowerPlantUtil.addState(allStates, pp3);
		
		// pp3 gets three soft constraints
		// c1_economically_optimal: production[t] >= 300.0 && production[t] <= 350.0;
		// c2_economically_good: production[t] >= 280.0 && production[t] <= 370.0;
		// c3_rate_of_change_opt: (running[t] == 1) => abs(production[t] - production[t+1]) <= production[t]  * 0.1;
		Constraint c1EconomicallyOptimalP3 = new BoundsConstraint(pp3, 300, 350.0);
		Constraint c2EconomicallyGoodP3 = new BoundsConstraint(pp3, 280.0, 370.0);
		Constraint c3RateOfChangeOpt = new RateOfChangeConstraint(pp3, 0.1);
		
		c1EconomicallyOptimalP3.setSoft(true); c2EconomicallyGoodP3.setSoft(true); c3RateOfChangeOpt.setSoft(true);
		c1EconomicallyOptimalP3.setWeight(3);
		c2EconomicallyGoodP3.setWeight(1);
		c3RateOfChangeOpt.setWeight(1);
		
		pp3.addConstraint(c1EconomicallyOptimalP3); pp3.addConstraint(c2EconomicallyGoodP3); pp3.addConstraint(c3RateOfChangeOpt);
		
		// not really needed any more
		pp1.put(AbstractionParameterLiterals.COSTS_PER_KWH, "13.0");
		pp2.put(AbstractionParameterLiterals.COSTS_PER_KWH, "70.0");
		pp3.put(AbstractionParameterLiterals.COSTS_PER_KWH, "5.0");

		pp1.setCostFunction(new PiecewiseLinearFunction(50.0, 100.0, 13.0));
		pp2.setCostFunction(new PiecewiseLinearFunction(15.0, 35.0, 70.0));
		pp3.setCostFunction(new PiecewiseLinearFunction(200.0, 400.0, 5.0));

		children = new ArrayList<PowerPlantData>(3);
		children.add(pp1);
		children.add(pp2);
		children.add(pp3);

		ga = new GeneralAbstraction();
		ga.setPowerPlants(children);
		ga.perform();
		ga.print();

		TemporalAbstraction ta = new TemporalAbstraction();
		ta.setPowerPlants(children);
		ta.setGeneralFeasibleRegions(ga.getFeasibleRegions());
		ta.setGeneralHoles(ga.getHoles());
		ta.perform(3);

		avpp.setFeasibleRegions(ga.getFeasibleRegions());
		avpp.setAllFeasibleRegions(ta.getAllFeasibleRegions());
		avpp.setAllHoles(ta.getAllHoles());

		singletonState = new HashMap<String, PowerPlantState>();
		singletonState.put(avpp.getName(), avppState);

		solver = new CplexSolver();
		AbstractModel model = new CplexModel();
		model.setUseSoftConstraints(true);
		
		PowerPlantUtil.populateDefaultSamplingModel(solver, model, avpp, children);
		// use soft constraint penalty as dexpr
		
		Collection<String> dexpr = new ArrayList<String>(1);
		dexpr.add(AbstractionParameterLiterals.PENALTY_SUM+ "Init = " + AbstractionParameterLiterals.PENALTY_SUM+ "[0]");
		
		model.addDecisionExpressions(dexpr);
	}

	@Test
	public void test() {
		System.out.println("STARTING TEST:::: ");
		ga.print();
		SamplingAbstraction sa = new SamplingAbstraction(ga.getFeasibleRegions(), ga.getHoles());

		Collection<String> objectives = new ArrayList<String>(1);
		objectives.add(AbstractionParameterLiterals.DEXP_POWER + "Succ");

		Collection<String> minimizationObjectives = new ArrayList<String>(2);
		minimizationObjectives.addAll(objectives);
		minimizationObjectives.add(AbstractionParameterLiterals.DEXP_COSTS + "Init");

		minimizationObjectives.add(AbstractionParameterLiterals.PENALTY_SUM+"Init");
		
		sa.setMaximizationDecisionExpressions(objectives);
		sa.setMinimizationDecisionExpressions(minimizationObjectives);
		sa.setSolver(solver);
		int samplingPoints = 20;

		sa.perform(samplingPoints);
		PiecewiseLinearFunction positiveDelta = sa.getPiecewiseLinearFunction(AbstractionParameterLiterals.DEXP_POWER + "Succ", false);
		avpp.setPositiveDelta(positiveDelta);
		PiecewiseLinearFunction negativeDelta = sa.getPiecewiseLinearFunction(AbstractionParameterLiterals.DEXP_POWER + "Succ", true);
		avpp.setNegativeDelta(negativeDelta);
		PiecewiseLinearFunction costFunction = sa.getPiecewiseLinearFunction(AbstractionParameterLiterals.DEXP_COSTS + "Init", true);
		
		PiecewiseLinearFunction penaltyFunction = sa.getPiecewiseLinearFunction(AbstractionParameterLiterals.PENALTY_SUM + "Init", true);
		writeData("PositiveDelta_" + samplingPoints + ".csv", positiveDelta);
		writeData("NegativeDelta_" + samplingPoints + ".csv", negativeDelta);
		writeData("Costs" + samplingPoints + ".csv", costFunction); 
		writeData("Penalty" + samplingPoints + ".csv", penaltyFunction);
		
		CplexExporter exporter = new CplexExporter();
		exporter.setUseSamplingAbstraction(true);
		Collection<PowerPlantData> singleton = new ArrayList<PowerPlantData>(1);
		singleton.add(avpp);

		exporter.setTimeHorizon(3);
		System.out.println("=================================== SYNTHESIZED MODEL ================================= ");
		printSynthesizedModel(exporter);
		System.out.println("=================================== ABSTRACTED MODEL ================================= ");
		printAbstractedModel(exporter, singleton);
	}

	private void writeData(String fileName, PiecewiseLinearFunction pwlFunction) {
		StringBuilder content = new StringBuilder();
		double[] ins = pwlFunction.getIns(), outs = pwlFunction.getOuts();

		for (int i = 0; i < pwlFunction.getNumberInputOutputPairs(); ++i) {
			content.append(Double.toString(ins[i]) + ";" + Double.toString(outs[i]) + "\n");
		}
		Utils.writeFile(fileName, content.toString());
	}

	private void printSynthesizedModel(CplexExporter exporter) {
		String synthesizedModel = exporter.createModel(children);
		TemporalAbstraction ta = new TemporalAbstraction();
		GeneralAbstraction ga = new GeneralAbstraction();
		ga.setPowerPlants(children);
		ga.perform();

		ta.setGeneralFeasibleRegions(ga.getFeasibleRegions());
		ta.setGeneralHoles(ga.getHoles());
		ta.setPowerPlants(children);
		ta.perform(3);

		Double[] residualLoad = new Double[] { 50.0, 70.0, 120.0 };

		String load = exporter.createResidualLoad(residualLoad);

		String initStateData = exporter.createInitStateData(allStates);

		System.out.println("=========================================================");
		System.out.println(synthesizedModel);
		System.out.println("==============================");
		System.out.println(load + "\n" + initStateData);
	}

	private void printAbstractedModel(CplexExporter exporter, Collection<PowerPlantData> singleton) {
		String abstractedModel = exporter.createModel(singleton);
		String functionalData = exporter.writePiecewiseLinearData(singleton);
		TemporalAbstraction ta = new TemporalAbstraction();
		GeneralAbstraction ga = new GeneralAbstraction();
		ga.setPowerPlants(singleton);
		ga.perform();

		ta.setGeneralFeasibleRegions(ga.getFeasibleRegions());
		ta.setGeneralHoles(ga.getHoles());
		ta.setPowerPlants(singleton);
		ta.perform(3);
		avpp.setAllFeasibleRegions(ta.getAllFeasibleRegions());
		avpp.setAllHoles(ta.getAllHoles());
		avpp.setHoles(ga.getHoles());
		avpp.setFeasibleRegions(ga.getFeasibleRegions());

		Double[] residualLoad = new Double[] { 50.0, 70.0, 120.0 };

		String load = exporter.createResidualLoad(residualLoad);

		String initStateData = exporter.createInitStateData(singletonState);
		String abstractionData = exporter.getGeneralAbstractionData(singleton) + "\n" + exporter.getTemporalAbstractionData(singleton);

		System.out.println("=========================================================");
		System.out.println(abstractedModel);
		System.out.println("=========================================================");
		System.out.println(functionalData + "\n" + load + "\n" + initStateData + "\n" + abstractionData);
	}

	@Test
	public void testExtractingDecisionExpression() {
		SolverFacade facade = new CPLEXSolverFacade();
		facade.solve("ExtractDecisionExpression.mod", null);
		double val = facade.getTotalProduction(1);
		System.out.println("VALUE? : " + val);
	}
}
