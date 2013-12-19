package de.uniaugsburg.isse.abstraction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;

import de.uniaugsburg.isse.abstraction.types.Interval;
import de.uniaugsburg.isse.constraints.BoundsConstraint;
import de.uniaugsburg.isse.constraints.Constraint;
import de.uniaugsburg.isse.constraints.FixedChangeConstraint;
import de.uniaugsburg.isse.constraints.ForceOnConstraint;
import de.uniaugsburg.isse.constraints.GraduallyOffConstraint;
import de.uniaugsburg.isse.constraints.RateOfChangeConstraint;
import de.uniaugsburg.isse.constraints.StartWithMinConstraint;
import de.uniaugsburg.isse.constraints.StopTimeConstraint;
import de.uniaugsburg.isse.powerplants.PowerPlantData;
import de.uniaugsburg.isse.powerplants.PowerPlantState;
import de.uniaugsburg.isse.util.AbstractionParameterLiterals;
import de.uniaugsburg.isse.util.Utils;

public class CplexExporter {
	private int timeHorizon;
	private Double[] residualLoad;
	private double numericalEps = 0.1; // tries to avoid numerical issues with
										// sampling abstraction
	private static final String powerVar = "energyProduction";

	private static final String runningVar = "running";

	// flags to customize model export for actual optimization or sampling
	// abstraction
	private boolean useSamplingAbstraction = true;
	private boolean useTemporalAbstraction = true;
	private boolean useInitialStateConstraints = true;
	private boolean useResidualLoad = true;

	// TODO this will be part of the organization model - now implemented as
	// default
	private String optimizationFunction = "minimize sum(t in TIMERANGE) abs(totalProduction[t] - energyConsumption[t]);";
	private String avppString; // contains child avpps

	// specifies that some decision variable (or expression) is set to a fixed
	// value -> necessary for sampling abstraction
	private Collection<String> constraints;
	private Collection<String> additionalDecExprs;
	// used to shrink the search space, feasible regions of the
	// AVPP represented by this CPLEX model, affect totalProduction
	private SortedSet<Interval<Double>> generalFeasibleRegions;
	private Collection<Interval<Double>> generalHoles;
	private boolean useGeneralFeasibleRegions = false;
	private double maxProduction = 0.0;
	private boolean useCompleteRange; // specifies if deltaPlus constraints
										// start at 1 or 0
	private boolean useCosts;
	private boolean useSoftConstraints; // includes a decision expression for penalties as well as soft-CR formulations
	
	// use 1 for load assignment problem as transition from t=0 to t=1 is
	// correctly done by temporal abstraction

	public boolean isUseCompleteRange() {
		return useCompleteRange;
	}

	public void setUseCompleteRange(boolean useCompleteRange) {
		this.useCompleteRange = useCompleteRange;
	}

	public CplexExporter() {
		constraints = new ArrayList<String>(2);
		additionalDecExprs = new LinkedList<String>();
	}

	private void writePreamble(StringBuilder sb) {
		sb.append("/* Generated CPLEX model */\n\n");
		sb.append("// Prediction horizon\nint timeHorizon = " + getTimeHorizon() + ";\nrange TIMERANGE = 1 .. timeHorizon;\n");
		sb.append("range COMPLETE_TIMERANGE = 0 .. timeHorizon;\nrange DEF_TIMERANGE = 0 .. timeHorizon+1;\n\n");
	}

	public String createModel(Collection<PowerPlantData> plants) {
		StringBuilder sb = new StringBuilder();
		writePreamble(sb);

		sb.append("tuple PowerPlantState {	float pInit; int runningInit; };\n");

		sb.append("/* type for intervals */\n");
		sb.append("tuple IntervalType {\n");
		sb.append("float lower;\n");
		sb.append("float upper;\n");
		sb.append("};\n\n");
		sb.append("float jitter = 0.001;\n");

		sb.append("{string} ControllablePlants = " + getStringSet(toStringSet(plants)) + ";\n");
		sb.append("dvar float " + powerVar + "[ControllablePlants][DEF_TIMERANGE];\n");
		sb.append("dvar boolean " + runningVar + "[ControllablePlants][DEF_TIMERANGE];\n");

		if(useSoftConstraints) {
			sb.append("{string} softConstraints = "+ getStringSet(toStringSetConstraints(getAllSoftConstraints(plants))) +";\n");
						 //dvar int+ penalties[softConstraints][TIMERANGE];
			sb.append("dvar int+ penalties[softConstraints][COMPLETE_TIMERANGE];\n");
			
			sb.append("dexpr float "+AbstractionParameterLiterals.PENALTY_SUM+"[t in COMPLETE_TIMERANGE] = sum(c in softConstraints) penalties[c][t];\n");
		}
		
		sb.append("dexpr float totalProduction[t in DEF_TIMERANGE] = sum(p in ControllablePlants) "
				+ powerVar + "[p][t] ;\n");

		if (useCosts) {

			sb.append("int maxBps = ...;\n");
			sb.append("tuple powerPlantData { float slopesPrice[1..maxBps+1]; float breakpoints[1..maxBps];");
			sb.append("int numBps; float firstCostFunctionInput;  float firstCostFunctionOutput;};");

			sb.append("powerPlantData ControllablePlant[ControllablePlants] = ...;");
			sb.append("pwlFunction costFunction[p in ControllablePlants] = piecewise(i in 1..ControllablePlant[p].numBps) {\n");
			sb.append("ControllablePlant[p].slopesPrice[i]->ControllablePlant[p].breakpoints[i]; ControllablePlant[p].slopesPrice[ControllablePlant[p].numBps+1]\n");
			sb.append("} (ControllablePlant[p].firstCostFunctionInput, ControllablePlant[p].firstCostFunctionOutput);");
			sb.append("dexpr float costsPerPlant[p in ControllablePlants][t in DEF_TIMERANGE] = costFunction[p](" + powerVar + "[p][t]);\n");
			sb.append("dexpr float totalCost[t in DEF_TIMERANGE] = sum(p in ControllablePlants) costsPerPlant[p][t];\n");
		}

		if (!additionalDecExprs.isEmpty()) {
			for (String dexpr : additionalDecExprs) {
				sb.append("dexpr float " + dexpr + ";\n");
			}
		}
		if (useInitialStateConstraints) {
			sb.append("PowerPlantState initialData[ControllablePlants] = ...;\n");
			sb.append("float initProduction[p in ControllablePlants][DEF_TIMERANGE] = initialData[p].pInit;\n");
			sb.append("int initRunning[p in ControllablePlants][DEF_TIMERANGE] = initialData[p].runningInit;\n");
		}
		
		if (useResidualLoad)
			sb.append("float energyConsumption[TIMERANGE] = ...;\n");

		if (anyAvpp(plants)) {
			avppString = getStringSet(getAvpps(plants));
			writeAbstractionVariables(sb);
		}

		if (generalFeasibleRegions != null || useGeneralFeasibleRegions) {
			// general abstraction of AVPP itself - referring to totalProduction
			sb.append("{IntervalType} totalGeneralBounds = ...;\n");
			sb.append("{IntervalType} totalGeneralHoles = ...;\n");
		}

		sb.append(optimizationFunction + "\n");
		// constraint area
		sb.append("\nsubject to {\n");

		if (anyNonAvpp(plants)) {
			sb.append("forall(t in COMPLETE_TIMERANGE){\n");
			for (PowerPlantData pd : plants) {
				if (!pd.isAVPP()) {
					for (Constraint c : pd.getAssociatedConstraints()) {
						appendConstraint(c, sb, pd);
					}
				}
			}
			sb.append("\n }\n");
		}

		if (!constraints.isEmpty()) {
			for (String constraint : constraints) {
				sb.append(constraint + "\n");
			}
		}

		if (anyAvpp(plants)) {
			writeAbstractionConstraints(sb);
		}

		if (generalFeasibleRegions != null) {
			writeTotalProductionConstraints(sb);
		}

		if (maxProduction > 0.0) {
			writeTotalUpperBound(sb);
		}

		if (useInitialStateConstraints)
			writeInitConstraints(sb);
		sb.append("\n}\n");
		return sb.toString();
	}

	private Collection<Constraint> getAllSoftConstraints(
			Collection<PowerPlantData> plants) {
		Collection<Constraint> allSoftConstraints = new ArrayList<Constraint>(plants.size()*3);
		for(PowerPlantData pd : plants){
			for(Constraint c: pd.getAssociatedConstraints()) {
				if(c.isSoft()){
					allSoftConstraints.add(c);
				}
			}
		}
		return allSoftConstraints;
	}

	private void writeTotalUpperBound(StringBuilder sb) {
		sb.append("forall (t in COMPLETE_TIMERANGE) {\n");
		sb.append("  totalProduction[t] <= " + maxProduction + ";\n");
		sb.append("}\n");
	}

	private void writeTotalProductionConstraints(StringBuilder sb) {
		if (generalFeasibleRegions != null) {
			sb.append("forall (t in COMPLETE_TIMERANGE) {\n");
			sb.append("  productionTotalHoles : forall ( h in totalGeneralHoles) {\n");
			sb.append("  !(totalProduction[t] >= h.lower + jitter && totalProduction[t] <= h.upper-jitter);\n");
			sb.append("  }\n");
			sb.append("  productionTotalRange : forall ( h in totalGeneralBounds) {\n");
			sb.append("    (totalProduction[t] >= h.lower && totalProduction[t] <= h.upper);\n}\n");
			sb.append("}\n");
		}
	}

	public void addEqualityConstraint(String name, String decExpr, double value, double tolerance) {
		// TODO could be more than one
		constraints.clear();

		// add some delta for numerical stability
		String constraint = "abs(" + decExpr + " - " + value + ") <= " + Math.max(tolerance, numericalEps) + ";";
		constraints.add(constraint);
	}

	protected Collection<String> getAvpps(Collection<PowerPlantData> plants) {
		Collection<String> avpps = new ArrayList<String>(plants.size());
		for (PowerPlantData pd : plants) {
			if (pd.isAVPP())
				avpps.add(pd.getName());
		}
		return avpps;
	}

	protected boolean anyAvpp(Collection<PowerPlantData> plants) {
		for (PowerPlantData plant : plants)
			if (plant.isAVPP())
				return true;
		return false;
	}

	private boolean anyNonAvpp(Collection<PowerPlantData> plants) {
		for (PowerPlantData pd : plants)
			if (!pd.isAVPP())
				return true;

		return false;
	}

	private void writeAbstractionConstraints(StringBuilder sb) {
		sb.append("forall (p in ControllablePlants, t in COMPLETE_TIMERANGE) {\n");
		sb.append("  productionHoles : forall ( h in generalHoles[p]) {\n");
		sb.append("  !(energyProduction[p][t] >= h.lower + jitter && energyProduction[p][t] <= h.upper - jitter);\n");
		sb.append("  }\n");
		sb.append("  productionRange : forall ( h in generalBounds[p]) {\n");
		sb.append("    (energyProduction[p][t] >= h.lower && energyProduction[p][t] <= h.upper);\n}\n");
		sb.append("}\n");

		if (useTemporalAbstraction) {
			sb.append("forall (p in ControllablePlants, t in CONSTRAINED_TIMERANGE) {\n");
			sb.append("   PowerBoundsTemporalConstraint : forall(b in temporalBounds[p][t]) {\n");
			sb.append("     energyProduction[p][t] >= b.lower;\n");
			sb.append("     energyProduction[p][t] <= b.upper;\n");
			sb.append("}\n");
			sb.append("   PowerHolesTemporalConstraint : forall ( h in temporalHoles[p][t]) {\n");
			sb.append("     !(energyProduction[p][t] >= h.lower + jitter && energyProduction[p][t] <= h.upper - jitter);\n");
			sb.append("  }\n");
			sb.append("}\n");
		}
		if (useSamplingAbstraction) {
			String usedRange = useCompleteRange ? "COMPLETE_TIMERANGE" : "CONSTRAINED_TIMERANGE";
			sb.append("forall(p in Avpps, t in " + usedRange + ") {\n");
			sb.append("  energyProduction[p][t+1] >= energyProduction[p][t] => (energyProduction[p][t+1] <= deltaPlus[p](energyProduction[p][t]));\n");
			sb.append("  energyProduction[p][t+1] <= energyProduction[p][t] => (energyProduction[p][t+1] >= deltaNeg[p](energyProduction[p][t]));\n");
			sb.append("}\n");
		}
	}

	private void writeInitConstraints(StringBuilder sb) {
		sb.append("forall(p in ControllablePlants) {\n");
		sb.append("  " + powerVar + "[p][0] == initialData[p].pInit;\n");
		sb.append("  running[p][0] == (initialData[p].runningInit == 1);\n");
		sb.append("}\n");
	}

	private void writeAbstractionVariables(StringBuilder sb) {
		// General and temporal abstraction
		sb.append("range CONSTRAINED_TIMERANGE = 1 .. timeHorizon;\n");
		sb.append("{IntervalType} generalBounds[ControllablePlants] = ...;\n");
		sb.append("{IntervalType} generalHoles[ControllablePlants] = ...;\n");
		if (useTemporalAbstraction) {
			sb.append("{IntervalType} temporalBounds[ControllablePlants][CONSTRAINED_TIMERANGE] = ...;\n");
			sb.append("{IntervalType} temporalHoles[ControllablePlants][CONSTRAINED_TIMERANGE] = ...;\n");
		}
		// sampling abstraction
		if (useSamplingAbstraction) {
			sb.append("int maxBps = ...;\n");
			sb.append("{string} Avpps = " + avppString + ";\n");
			sb.append("tuple AvppData {\n");
			sb.append("int numBPsPos; int numBPsNeg; float firstInPos; float firstInNeg; float dPlAtFirst; float dNegAtFirst;\n");
			sb.append("};\n");

			sb.append("float slopesPlus[Avpps][1..maxBps+1] = ...;\n");
			sb.append("float breakpointsPlus[Avpps][1..maxBps] = ...;\n");
			sb.append("float slopesNeg[Avpps][1..maxBps+1] = ...;\n");
			sb.append("float breakpointsNeg[Avpps][1..maxBps] = ...;\n");

			sb.append("AvppData avppData[Avpps] = ...;\n");
			sb.append("pwlFunction deltaPlus[p in Avpps] = piecewise(i in 1..avppData[p].numBPsPos) {\n");
			sb.append(" slopesPlus[p][i]->breakpointsPlus[p][i]; slopesPlus[p][avppData[p].numBPsPos+1] \n");
			sb.append("} (avppData[p].firstInPos, avppData[p].dPlAtFirst);\n");

			sb.append("pwlFunction deltaNeg[p in Avpps] = piecewise(i in 1..avppData[p].numBPsNeg) {\n");
			sb.append(" slopesNeg[p][i]->breakpointsNeg[p][i]; slopesNeg[p][avppData[p].numBPsNeg+1]  \n");
			sb.append("} (avppData[p].firstInNeg, avppData[p].dNegAtFirst);\n");
		}
	}

	public String getPowerString(String ident, int time) {
		return powerVar + "[\"" + ident + "\"][" + time + "]";
	}

	public String getRunningString(String ident, int time) {
		return runningVar + "[\"" + ident + "\"][" + time + "]";
	}

	private String exportLoads(Double[] residualLoad2) {
		StringBuilder sb = new StringBuilder("[");
		boolean first = true;
		int i = 0;
		for (Double d : residualLoad2) {
			if (!first)
				sb.append(", ");
			else
				first = false;
			sb.append(d);

			if (++i >= timeHorizon)
				break;
		}
		sb.append("]");
		return sb.toString();
	}

	private void appendConstraint(Constraint c, StringBuilder sb, PowerPlantData pd) {
		// TODO fix min stop time constraint
		String powerString = "energyProduction[\"" + pd.getName() + "\"][t]";
		String constraintString = "";
		if (c instanceof BoundsConstraint) {
			BoundsConstraint bc = (BoundsConstraint) c;
			StringBuilder cb = new StringBuilder();
			
			if(c.isSoft()) {
				cb.append("("+powerString + " >= " + bc.getBoundaries().min + " && " + powerString + " <= " + bc.getBoundaries().max+")");
			} else {
			cb.append("(running[\"" + pd.getName() + "\"][t] == true => (");
			cb.append(powerString + " >= " + bc.getBoundaries().min + " && ");
			cb.append(powerString + " <= " + bc.getBoundaries().max + ")) && (");
			cb.append("(running[\"" + pd.getName() + "\"][t] == false) => ("
					+ powerString + " == 0.0))");
			}
			constraintString = cb.toString();
		} else if (c instanceof RateOfChangeConstraint) {
			RateOfChangeConstraint roc = (RateOfChangeConstraint) c;
			String energyPred = "energyProduction[\"" + pd.getName() + "\"][t]";
			String energySucc = "energyProduction[\"" + pd.getName()
					+ "\"][t+1]";
			
			String rateOfChangeString = "running[\"" + pd.getName()
					+ "\"][t] == true && running[\"" + pd.getName()
					+ "\"][t+1] == true => abs(" + energySucc + " - "
					+ energyPred + ") <= " + energyPred + " * "
					+ roc.getRateOfChange();
			
			constraintString = rateOfChangeString;
		} else if (c instanceof FixedChangeConstraint) {
			FixedChangeConstraint fcc = (FixedChangeConstraint) c;
			String energyPred = "energyProduction[\"" + pd.getName() + "\"][t]";
			String energySucc = "energyProduction[\"" + pd.getName()
					+ "\"][t+1]";
			
			String fixedChangeString = "running[\"" + pd.getName()
					+ "\"][t] == true && running[\"" + pd.getName()
					+ "\"][t+1] == true => abs(" + energySucc + " - "
					+ energyPred + ") <= " + fcc.getMaxProductionChange();
			constraintString = fixedChangeString;
		} else if (c instanceof ForceOnConstraint) {
			sb.append("running[\"" + pd.getName() + "\"][t] == true;\n");	
		} else if (c instanceof StopTimeConstraint) {

		} else if (c instanceof StartWithMinConstraint) {
			StringBuilder cb = new StringBuilder();
			cb.append("(running[\"" + pd.getName()
					+ "\"][t] == false) && (running[\"" + pd.getName()
					+ "\"][t+1] == true) => energyProduction[\"" + pd.getName()
					+ "\"][t+1] == " + pd.getPowerBoundaries().min );
			constraintString = cb.toString();
		} else if (c instanceof GraduallyOffConstraint) {
			StringBuilder cb = new StringBuilder();
			cb.append("(running[\"" + pd.getName()
					+ "\"][t] == true) && (running[\"" + pd.getName()
					+ "\"][t+1] == false) => energyProduction[\""
					+ pd.getName() + "\"][t] == " + pd.getPowerBoundaries().min);
			constraintString = cb.toString();
		}
		
		if(c.isSoft()) {
			String penaltyC = "penalties[\""+c.getIdent()+"\"][t]";
			String s = "(("+constraintString+") && (" + penaltyC + " == 0)) || (!("+constraintString+") && ("+penaltyC + " == "+c.getWeight()+"));\n";
			sb.append(s);
		} else {
			sb.append(constraintString + ";\n");
		}
	}

	private Collection<String> toStringSet(Collection<PowerPlantData> allPlants) {
		Collection<String> stringSet = new ArrayList<String>(allPlants.size());
		for (PowerPlantData pd : allPlants)
			stringSet.add(pd.getName());
		return stringSet;
	}
	
	private Collection<String> toStringSetConstraints(Collection<Constraint> constraints) {
		Collection<String> stringSet = new ArrayList<String>(constraints.size());
		for (Constraint c : constraints)
			stringSet.add(c.getIdent());
		return stringSet;
	}

	private String getStringSet(Collection<String> allPlants) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		sb.append("{ ");
		for (String plant : allPlants) {
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			sb.append("\"" + plant + "\"");
		}
		sb.append("}");
		return sb.toString();
	}

	public int getTimeHorizon() {
		return timeHorizon;
	}

	public void setTimeHorizon(int timeHorizon) {
		this.timeHorizon = timeHorizon;
	}

	public Double[] getResidualLoad() {
		return residualLoad;
	}

	public void setResidualLoad(Double[] residualLoad) {
		this.residualLoad = residualLoad;
	}

	public String writePiecewiseLinearData(Collection<PowerPlantData> children) {

		if (!anyAvpp(children))
			return "";

		StringBuilder sb = new StringBuilder();
		int maxBPs = 0;
		Collection<String> stringContainer = new ArrayList<String>(children.size());

		Map<String, String> slopesDPos = new HashMap<String, String>();
		Map<String, String> slopesDNeg = new HashMap<String, String>();
		Map<String, String> breakpointsPos = new HashMap<String, String>();
		Map<String, String> breakpointsNeg = new HashMap<String, String>();

		// first avppData tuples
		sb.append("avppData = #[\n");
		for (PowerPlantData pd : children) {
			if (pd.isAVPP()) { // this one has data
				;
				int bpsPos = pd.getPositiveDelta().getBPs();
				int bpsNeg = pd.getNegativeDelta().getBPs();
				maxBPs = Math.max(maxBPs, pd.getPositiveDelta().getBPs());
				maxBPs = Math.max(maxBPs, bpsNeg);

				StringBuilder singleAvppBuilder = new StringBuilder();
				singleAvppBuilder.append("\"" + pd.getName() + "\" : #<");
				singleAvppBuilder.append("numBPsPos : " + bpsPos + ",\n");
				singleAvppBuilder.append("numBPsNeg : " + bpsNeg + ",\n");
				singleAvppBuilder.append("firstInPos : " + pd.getPositiveDelta().getFirstInput() + ",\n");
				singleAvppBuilder.append("firstInNeg : " + pd.getNegativeDelta().getFirstInput() + ",\n");
				singleAvppBuilder.append("dPlAtFirst : " + pd.getPositiveDelta().getFirstOutput() + ",\n");
				singleAvppBuilder.append("dNegAtFirst : " + pd.getNegativeDelta().getFirstOutput() + "\n");
				singleAvppBuilder.append(">#");
				stringContainer.add(singleAvppBuilder.toString());

				slopesDPos.put(pd.getName(), pd.getPositiveDelta().getSlopesString());
				slopesDNeg.put(pd.getName(), pd.getNegativeDelta().getSlopesString());
				breakpointsPos.put(pd.getName(), pd.getPositiveDelta().getBreakpointString());
				breakpointsNeg.put(pd.getName(), pd.getNegativeDelta().getBreakpointString());
			}
		}
		sb.append(Utils.getSeparatedListOfStrings(stringContainer, ",\n"));
		sb.append("]#;\n");

		// now slopes and breakpoints
		sb.append(writeMap("slopesPlus", slopesDPos));
		sb.append(writeMap("slopesNeg", slopesDNeg));
		sb.append(writeMap("breakpointsPlus", breakpointsPos));
		sb.append(writeMap("breakpointsNeg", breakpointsNeg));
		return "maxBps = " + maxBPs + ";\n" + sb.toString();
	}

	private String writeMap(String ident, Map<String, String> identItems) {
		StringBuilder sb = new StringBuilder();
		Collection<String> stringContainer = new ArrayList<String>(identItems.size());
		// first avppData tuples
		sb.append(ident + " = #[\n");
		for (Entry<String, String> entry : identItems.entrySet()) {
			stringContainer.add("\"" + entry.getKey() + "\" : " + entry.getValue());

		}
		sb.append(Utils.getSeparatedListOfStrings(stringContainer, ",\n"));
		sb.append("]#;\n");
		return sb.toString();
	}

	public String createInitStateData(Map<String, PowerPlantState> allStates2) {
		StringBuilder sb = new StringBuilder("initialData = #[\n");
		boolean first = true;
		for (Entry<String, PowerPlantState> entry : allStates2.entrySet()) {
			if (!first)
				sb.append(",\n");
			else
				first = false;
			sb.append("\"" + entry.getKey() + "\" : #< pInit : " + entry.getValue().getPower().min + ",\n");
			sb.append("runningInit: " + (entry.getValue().getPower().min > 0 ? "1" : "0") + ">#");
		}
		sb.append("]#;\n");
		return sb.toString();
	}

	public String createResidualLoad(Double[] residualLoadParam) {
		String export = "energyConsumption = " + exportLoads(residualLoadParam) + ";\n";
		return export;
	}

	private String writeIntervalSet(Collection<Interval<Double>> intervalSet) {
		StringBuilder newEntry = new StringBuilder();
		newEntry.append("{\n");
		Collection<String> intervalStrings = new ArrayList<String>(intervalSet.size());
		for (Interval<Double> interval : intervalSet) {
			intervalStrings.add(" <" + interval.min + "," + interval.max + ">");
		}
		newEntry.append(Utils.getSeparatedListOfStrings(intervalStrings, ",\n"));
		newEntry.append(" \n}\n");
		return newEntry.toString();
	}

	public String writeIntervalSets(String name, Map<String, Collection<Interval<Double>>> holesMap) {
		StringBuilder sb = new StringBuilder();
		Collection<String> stringContainer = new ArrayList<String>(100);
		sb.append(" " + name + " = #[");
		stringContainer.clear();
		for (Entry<String, Collection<Interval<Double>>> entry : holesMap.entrySet()) {
			Collection<Interval<Double>> intervalSet = entry.getValue();
			if (!intervalSet.isEmpty()) {
				StringBuilder newEntry = new StringBuilder();
				newEntry.append(" " + entry.getKey() + " : " + writeIntervalSet(intervalSet));
				stringContainer.add(newEntry.toString());
			}
			// }");
		}
		sb.append(Utils.getSeparatedListOfStrings(stringContainer, ",\n"));
		sb.append("\n]#;\n");
		return sb.toString();
	}

	public String writeIntervalSetsList(String name, Map<String, List<Collection<Interval<Double>>>> intervalSetList) {
		StringBuilder sb = new StringBuilder();
		Collection<String> stringContainer = new ArrayList<String>(100);
		sb.append(" " + name + " = #[");
		stringContainer.clear();
		String emptySets = "[" + Utils.getEmptySets(timeHorizon) + "]";

		for (Entry<String, List<Collection<Interval<Double>>>> intervalSetEntry : intervalSetList.entrySet()) {
			String plantName = intervalSetEntry.getKey();
			List<Collection<Interval<Double>>> intervalList = intervalSetEntry.getValue();
			if (!intervalList.isEmpty()) {
				stringContainer.add(" " + plantName + " : [" + Utils.getPaddedSet(intervalList, timeHorizon) + "]");
			} else {
				stringContainer.add(" " + plantName + " : " + emptySets);
			}
		}

		sb.append(Utils.getSeparatedListOfStrings(stringContainer, ",\n"));
		sb.append("\n]#;\n");
		return sb.toString();
	}

	public void setInitialStateConstraints(boolean useInitialStateConstraints) {
		this.useInitialStateConstraints = useInitialStateConstraints;
	}

	public String getOptimizationFunction() {
		return optimizationFunction;
	}

	public void setOptimizationFunction(String optimizationFunction) {
		this.optimizationFunction = optimizationFunction;
	}

	public boolean isUseResidualLoad() {
		return useResidualLoad;
	}

	public void setUseResidualLoad(boolean useResidualLoad) {
		this.useResidualLoad = useResidualLoad;
	}

	public boolean isUseSamplingAbstraction() {
		return useSamplingAbstraction;
	}

	public void setUseSamplingAbstraction(boolean useSamplingAbstraction) {
		this.useSamplingAbstraction = useSamplingAbstraction;
	}

	public void setOptimizationFunction(String decExpr, boolean minimize) {
		String prefix = minimize ? "minimize" : "maximize";
		setOptimizationFunction(prefix + " " + decExpr + ";");
	}

	public void addDExpr(Collection<String> dexprs) {
		for (String dexpr : dexprs)
			additionalDecExprs.add(dexpr);

	}

	public void setGeneralFeasibleRegions(SortedSet<Interval<Double>> feasibleRegions) {
		this.generalFeasibleRegions = feasibleRegions;

	}

	public String getTotalProductionAbstractionString() {
		return getTotalProductionAbstractionString(generalHoles, generalFeasibleRegions);
	}

	public String getTotalProductionAbstractionString(Collection<Interval<Double>> generalHoles, SortedSet<Interval<Double>> generalFeasibleRegions) {
		StringBuilder sb = new StringBuilder();
		sb.append("totalGeneralHoles = " + writeIntervalSet(generalHoles) + ";\n");
		double min = generalFeasibleRegions.first().min, max = generalFeasibleRegions.last().max;
		Collection<Interval<Double>> boundsSet = new ArrayList<Interval<Double>>(1);
		boundsSet.add(new Interval<Double>(min, max));
		sb.append("totalGeneralBounds =  " + writeIntervalSet(boundsSet) + ";\n");
		return sb.toString();
	}

	public Collection<Interval<Double>> getGeneralHoles() {
		return generalHoles;
	}

	public void setGeneralHoles(Collection<Interval<Double>> generalHoles) {
		this.generalHoles = generalHoles;
	}

	public String getGeneralAbstractionData(Collection<PowerPlantData> children) {
		StringBuilder sb = new StringBuilder();

		/* =========== now general bounds =============== */
		if (anyAvpp(children)) {
			Map<String, Collection<Interval<Double>>> holesMap = getHolesSet(children);
			sb.append(writeIntervalSets("generalHoles", holesMap));

			Map<String, Collection<Interval<Double>>> regionsMap = getRegionsMap(children);
			sb.append(writeIntervalSets("generalBounds", regionsMap));
		}
		return sb.toString();
	}

	public Map<String, List<Collection<Interval<Double>>>> getAllRegionsMap(Collection<PowerPlantData> children) {
		Map<String, List<Collection<Interval<Double>>>> allRegions = new HashMap<String, List<Collection<Interval<Double>>>>(children.size() * 2);

		for (PowerPlantData child : children) {
			if (child.isAVPP()) {
				List<SortedSet<Interval<Double>>> feasRegions = child.getAllFeasibleRegions();

				if (!feasRegions.isEmpty()) {
					// create min/max set
					List<Collection<Interval<Double>>> singletons = new ArrayList<Collection<Interval<Double>>>(feasRegions.size());
					for (SortedSet<Interval<Double>> feasRegion : feasRegions) {

						Interval<Double> avppBounds = new Interval<Double>(feasRegion.first().min, feasRegion.last().max);
						Collection<Interval<Double>> singletonSet = new ArrayList<Interval<Double>>(1);
						singletonSet.add(avppBounds);
						singletons.add(singletonSet);

					}
					allRegions.put(child.getName(), singletons);
				}
			}
		}

		return allRegions;
	}

	public Map<String, List<Collection<Interval<Double>>>> getAllHolesMap(Collection<PowerPlantData> children) {
		Map<String, List<Collection<Interval<Double>>>> allHoles = new HashMap<String, List<Collection<Interval<Double>>>>();
		for (PowerPlantData child : children) {
			if (child.isAVPP()) {
				allHoles.put(child.getName(), child.getAllHoles());
			}
		}
		return allHoles;
	}

	public Map<String, Collection<Interval<Double>>> getRegionsMap(Collection<PowerPlantData> children) {
		Map<String, Collection<Interval<Double>>> feasRegions = new HashMap<String, Collection<Interval<Double>>>();
		for (PowerPlantData plantData : children) {
			String plantName = plantData.getName();

			if (plantData.isAVPP()) {
				SortedSet<Interval<Double>> generalRegions = plantData.getFeasibleRegions();
				// create min/max set
				Interval<Double> avppBounds = new Interval<Double>(generalRegions.first().min, generalRegions.last().max);
				Collection<Interval<Double>> singletonSet = new ArrayList<Interval<Double>>(1);
				singletonSet.add(avppBounds);
				feasRegions.put(plantName, singletonSet);
			}
		}
		return feasRegions;
	}

	public Map<String, Collection<Interval<Double>>> getHolesSet(Collection<PowerPlantData> children) {
		Map<String, Collection<Interval<Double>>> holesMap = new HashMap<String, Collection<Interval<Double>>>();

		for (PowerPlantData child : children) {
			String plantName = child.getName();

			if (child.isAVPP()) {
				Collection<Interval<Double>> generalHoles = child.getHoles();
				holesMap.put(plantName, generalHoles);
			}
		}
		return holesMap;
	}

	public String getTemporalAbstractionData(Collection<PowerPlantData> children) {
		StringBuilder sb = new StringBuilder();

		/* =========== now general bounds =============== */
		if (anyAvpp(children)) {
			Map<String, List<Collection<Interval<Double>>>> holesMap = getAllHolesMap(children);
			sb.append(writeIntervalSetsList("temporalHoles", holesMap));

			Map<String, List<Collection<Interval<Double>>>> regionsMap = getAllRegionsMap(children);
			sb.append(writeIntervalSetsList("temporalBounds", regionsMap));
		}
		return sb.toString();

	}

	public boolean isUseTemporalAbstraction() {
		return useTemporalAbstraction;
	}

	public void setUseTemporalAbstraction(boolean useTemporalAbstraction) {
		this.useTemporalAbstraction = useTemporalAbstraction;
	}

	public boolean isUseGeneralFeasibleRegions() {
		return useGeneralFeasibleRegions;
	}

	public void setUseGeneralFeasibleRegions(boolean useGeneralFeasibleRegions) {
		this.useGeneralFeasibleRegions = useGeneralFeasibleRegions;
	}

	public void setMaximalUpperBound(double maxProduction) {
		this.maxProduction = maxProduction;
	}

	public void addConstraint(String constraint) {
		constraints.add(constraint);
	}

	public void setCosts(boolean b) {
		this.useCosts = b;
	}

	public String writeCostsString(Collection<PowerPlantData> children) {
		Map<String, String> breakpointsMap = new HashMap<String, String>();
		Map<String, String> slopesMap = new HashMap<String, String>();
		int maxBps = 0;
		StringBuilder buffer = new StringBuilder();
		boolean first = true;
		buffer.append("ControllablePlant = ");
		buffer.append("#[\n");
		for (PowerPlantData plantData : children) {
			if (plantData.getCostFunction().getBPs() > maxBps)
				maxBps = plantData.getCostFunction().getBPs();
		}

		for (PowerPlantData plantData : children) {

			if (!first) {
				buffer.append(",\n");
			} else
				first = false;

			buffer.append(plantData.getName());
			buffer.append(" : \n");
			buffer.append("    #< slopesPrice : ");
			double [] costs = plantData.getCostFunction().getSlopes(maxBps);
			for(int i = 0; i < costs.length; ++i) {
				costs[i] *= 100.0;
			}
			buffer.append(Arrays.toString(costs));
			buffer.append(", \n");
			buffer.append("       breakpoints : ");
			buffer.append(plantData.getCostFunction().getBreakpointString(maxBps));
			buffer.append(", \n");
			buffer.append("       numBps : ");
			buffer.append(plantData.getCostFunction().getBPs());
			buffer.append(", \n");
			buffer.append("       firstCostFunctionInput : ");
			buffer.append(plantData.getCostFunction().getFirstInput());
			buffer.append(", \n");
			buffer.append("       firstCostFunctionOutput : ");
			buffer.append(plantData.getCostFunction().getFirstOutput()*100.0);
			buffer.append("    >#\n");

		}
		buffer.append("]#;\n");

		StringBuilder costsString = new StringBuilder();
		costsString.append("maxBps = " + maxBps + ";\n");
		costsString.append(buffer.toString());
		return costsString.toString();
	}

	public boolean isUseSoftConstraints() {
		return useSoftConstraints;
	}

	public void setUseSoftConstraints(boolean useSoftConstraints) {
		this.useSoftConstraints = useSoftConstraints;
	}
}
