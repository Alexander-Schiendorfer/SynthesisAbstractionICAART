package de.uniaugsburg.isse.solver;

import java.util.Collection;
import java.util.SortedSet;

import de.uniaugsburg.isse.abstraction.types.Interval;
import de.uniaugsburg.isse.powerplants.PowerPlantData;

/**
 * Represents a Constraint Model that is to be solved by an AbstractSolver
 * instance -> includes optimization functions and additional constraints
 * 
 * @author Alexander Schiendorfer
 * 
 */
public interface AbstractModel {
	void setPlantData(PowerPlantData avpp, Collection<PowerPlantData> children);

	void addEqualityConstraint(String name, String decExpr, double value,
			double tolerance);

	void setObjective(String decExpr, boolean maximize);

	public abstract Collection<PowerPlantData> getChildren();

	public abstract PowerPlantData getAvpp();

	public abstract void setTimeHorizon(int timeHorizon);

	public abstract int getTimeHorizon();

	void setFeasibleRegions(SortedSet<Interval<Double>> generalFeasibleRegions);

	public abstract void setGeneralHoles(
			Collection<Interval<Double>> generalHoles);

	public abstract Collection<Interval<Double>> getGeneralHoles();

	public abstract void addDecisionExpressions(Collection<String> dexprs);

	void requireEqualBound();

	void setInputExpression(String inputExpr);

	void addInputLowerBoundConstraint(double lowerBound);

	void addOutputLowerBound(String decExpr, double prevResult);

	void setCosts(boolean b);

	public abstract void setUseSoftConstraints(boolean useSoftConstraints);

	public abstract boolean isUseSoftConstraints();

}
