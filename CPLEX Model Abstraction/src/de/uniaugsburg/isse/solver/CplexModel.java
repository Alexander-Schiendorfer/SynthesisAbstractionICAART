package de.uniaugsburg.isse.solver;

import java.util.Collection;
import java.util.SortedSet;

import de.uniaugsburg.isse.abstraction.CplexExporter;
import de.uniaugsburg.isse.abstraction.types.Interval;
import de.uniaugsburg.isse.powerplants.PowerPlantData;

public class CplexModel implements AbstractModel {
	private PowerPlantData avpp;
	private Collection<PowerPlantData> children;
	private CplexExporter exporter;
	private int timeHorizon;
	private SortedSet<Interval<Double>> feasibleRegions;
	private Collection<Interval<Double>> generalHoles;
	private boolean objectiveMinimize;
	private String objectiveExpr;
	private String inputExpr;
	private boolean useCosts;
	private boolean useSoftConstraints;

	public CplexModel(int timeHorizon) {
		this.timeHorizon = timeHorizon;
		exporter = new CplexExporter();
		exporter.setInitialStateConstraints(false);
		exporter.setUseResidualLoad(false);
		exporter.setUseTemporalAbstraction(false);
		exporter.setTimeHorizon(timeHorizon);
		exporter.setUseCompleteRange(true); // important for sampling
											// abstraction
	}

	public CplexModel() {
		this(10);
	}

	@Override
	public void addEqualityConstraint(String name, String decExpr,
			double value, double tolerance) {
		exporter.addEqualityConstraint(name, decExpr, value, tolerance);

	}

	@Override
	public void setObjective(String decExpr, boolean minimize) {
		this.objectiveExpr = decExpr;
		this.objectiveMinimize = minimize;
		exporter.setOptimizationFunction(decExpr, minimize);
	}

	@Override
	public void setPlantData(PowerPlantData avpp,
			Collection<PowerPlantData> children) {
		this.avpp = avpp;
		this.children = children;
	}

	@Override
	public PowerPlantData getAvpp() {
		return avpp;
	}

	public void setAvpp(PowerPlantData avpp) {
		this.avpp = avpp;
	}

	@Override
	public Collection<PowerPlantData> getChildren() {
		return children;
	}

	public void setChildren(Collection<PowerPlantData> children) {
		this.children = children;
	}

	public String getCplexString() {
		return exporter.createModel(children);
	}

	public String getCplexDataString() {
		String cplexDataString = exporter.getTotalProductionAbstractionString();

		if (useCosts) {
			cplexDataString = cplexDataString + "\n"
					+ exporter.writeCostsString(children);
		}

		if (anyAvpp()) {
			String generalAbstractionData = exporter
					.getGeneralAbstractionData(children);
			String pwLinerData = exporter.writePiecewiseLinearData(children);
			return cplexDataString + "\n" + generalAbstractionData + "\n"
					+ pwLinerData;
		} else {
			return cplexDataString;
		}
	}

	private boolean anyAvpp() {
		for (PowerPlantData pd : children)
			if (pd.isAVPP())
				return true;

		return false;
	}

	@Override
	public int getTimeHorizon() {
		return timeHorizon;
	}

	@Override
	public void setTimeHorizon(int timeHorizon) {
		this.timeHorizon = timeHorizon;
	}

	@Override
	public void addDecisionExpressions(Collection<String> dexprs) {
		this.exporter.addDExpr(dexprs);

	}

	@Override
	public boolean isUseSoftConstraints() {
		return useSoftConstraints;
	}

	@Override
	public void setUseSoftConstraints(boolean useSoftConstraints) {
		this.useSoftConstraints = useSoftConstraints;
		exporter.setUseSoftConstraints(useSoftConstraints);
	}

	@Override
	public void setFeasibleRegions(
			SortedSet<Interval<Double>> generalFeasibleRegions) {
		this.feasibleRegions = generalFeasibleRegions;
		exporter.setGeneralFeasibleRegions(feasibleRegions);
	}

	@Override
	public Collection<Interval<Double>> getGeneralHoles() {
		return generalHoles;
	}

	@Override
	public void setGeneralHoles(Collection<Interval<Double>> generalHoles) {
		this.generalHoles = generalHoles;
		exporter.setGeneralHoles(generalHoles);
	}

	/**
	 * This method requires that the input consitutes a lower (for optimization)
	 * or upper (for minimization) bound for the output i.e. f(x) <= x or f(x)
	 * >= x
	 */
	@Override
	public void requireEqualBound() {
		String constraint = null;
		if (objectiveMinimize) {
			constraint = inputExpr + " >= " + objectiveExpr + ";";
		} else {
			constraint = inputExpr + " <= " + objectiveExpr + ";";
		}
		exporter.addConstraint(constraint);
	}

	@Override
	public void setInputExpression(String inputExpr) {
		this.inputExpr = inputExpr;
	}

	@Override
	public void addInputLowerBoundConstraint(double lowerBound) {
		String constraint = inputExpr + " >= " + lowerBound + ";";
		exporter.addConstraint(constraint);
	}

	@Override
	public void addOutputLowerBound(String outputExpr, double lowerBound) {
		String constraint = outputExpr + " >= " + lowerBound + ";";
		exporter.addConstraint(constraint);
	}

	@Override
	public void setCosts(boolean b) {
		exporter.setCosts(b);
		this.useCosts = b;
	}

}
