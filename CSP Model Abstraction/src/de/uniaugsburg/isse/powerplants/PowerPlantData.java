package de.uniaugsburg.isse.powerplants;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import de.uniaugsburg.isse.abstraction.types.Interval;
import de.uniaugsburg.isse.abstraction.types.PiecewiseLinearFunction;
import de.uniaugsburg.isse.constraints.Constraint;
import de.uniaugsburg.isse.constraints.PlantConstraint;

/**
 * This class contains all relevant data to perform abstraction / temporal and general Intervals are stored for the min
 * and max state of power, respectively TODO rename this class in order to avoid confusion with existing ppdata or merge
 * those two essentially DTOS
 * 
 * @author Alexander Schiendorfer
 * 
 */
public class PowerPlantData {

	/**
	 * Stores parameters serving as input for the CPLEX model - stored in
	 * de.uniaugsburg.isse.util.AbstractionParameterLiterals
	 */
	private Map<String, String> parameters;

	/**
	 * Absolute minimal and maximal value of interval, done for sorting purposes, for now this has to be set even with
	 * an AVPP
	 */
	private Interval<Double> powerBoundaries;

	/**
	 * In case it is an AVPP, store general feasible regions - otherwise null
	 */
	private SortedSet<Interval<Double>> feasibleRegions;

	/**
	 * Also provide the general supply holes
	 */
	private Collection<Interval<Double>> holes;

	/**
	 * Contains a list of interval sets denoting the feasible regions after time step t first entry corresponds to t=1
	 * in planning, since this is the first actual planned step
	 */
	private List<SortedSet<Interval<Double>>> allFeasibleRegions;

	/**
	 * All holes as found by temporal abstraction
	 */
	private List<Collection<Interval<Double>>> allHoles;
	/**
	 * Set of constraints that are maximized/minimized during temporal abstraction
	 */
	private Collection<Constraint> associatedConstraints;

	/**
	 * Represents the positive delta function acquired by sampling abstraction; only applicable, if this is an AVPP Then
	 * P[t+1] <= positiveDelta(P[t]) must hold
	 */
	private PiecewiseLinearFunction positiveDelta;

	/**
	 * Represents the negative delta function acquired by sampling abstraction; only applicable, if this is an AVPP.
	 * Then P[t+1] >= negativeDelta(P[t]) must hold
	 */
	private PiecewiseLinearFunction negativeDelta;

	/**
	 * Represents the cost function that can either be a single linear segment as in the case of a physical power plant
	 * or an approximation of the true cost function
	 */
	private PiecewiseLinearFunction costFunction;

	private String name;

	private boolean avpp = false;

	private Map<Class<? extends Constraint>, Integer> runningIdsConstraints;
	
	public PowerPlantData() {
		parameters = new HashMap<String, String>();
		runningIdsConstraints = new HashMap<Class<? extends Constraint>, Integer>();
	}

	public PowerPlantData(String name) {
		this();
		this.name = name;
	}

	public Interval<Double> getPowerBoundaries() {
		return powerBoundaries;
	}

	public PiecewiseLinearFunction getCostFunction() {
		return costFunction;
	}

	public void setCostFunction(PiecewiseLinearFunction costFunction) {
		this.costFunction = costFunction;
	}

	public void setPowerBoundaries(Interval<Double> powerBoundaries) {
		this.powerBoundaries = powerBoundaries;
	}

	public Collection<Constraint> getAssociatedConstraints() {
		return associatedConstraints;
	}

	public void setAssociatedConstraints(Collection<Constraint> associatedConstraints) {
		this.associatedConstraints = associatedConstraints;
	}

	public void addConstraint(Constraint roc) {
		if (associatedConstraints == null)
			associatedConstraints = new LinkedList<Constraint>();
		
		associatedConstraints.add(roc);
		int newId = 0;
		
		if(runningIdsConstraints.containsKey(roc.getClass())) {
			newId = runningIdsConstraints.get(roc.getClass());
			++newId;
		} 
		runningIdsConstraints.put(roc.getClass(), newId);
		if(roc instanceof PlantConstraint) {
			PlantConstraint pc = (PlantConstraint) roc;
			pc.setId(newId);
		}
	}

	public final String getName() {
		return name;
	}

	public final void setName(String name) {
		this.name = name;
	}

	public void put(String key, String value) {
		parameters.put(key, value);
	}

	public String get(String key) {
		return parameters.get(key);
	}

	public Map<String, String> getMap() {
		return parameters;
	}

	public void setFeasibleRegions(SortedSet<Interval<Double>> feasibleRegions) {
		this.feasibleRegions = feasibleRegions;
	}

	public SortedSet<Interval<Double>> getFeasibleRegions() {
		return feasibleRegions;
	}

	public boolean isAVPP() {
		return allFeasibleRegions != null || avpp;
	}

	public List<SortedSet<Interval<Double>>> getAllFeasibleRegions() {
		return allFeasibleRegions;
	}

	public void setAllFeasibleRegions(List<SortedSet<Interval<Double>>> allFeasibleRegions) {
		this.allFeasibleRegions = allFeasibleRegions;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PowerPlantData other = (PowerPlantData) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	public void setAVPP(boolean avpp) {
		this.avpp = avpp;
	}

	public PiecewiseLinearFunction getPositiveDelta() {
		return positiveDelta;
	}

	public void setPositiveDelta(PiecewiseLinearFunction positiveDelta) {
		this.positiveDelta = positiveDelta;
	}

	public PiecewiseLinearFunction getNegativeDelta() {
		return negativeDelta;
	}

	public void setNegativeDelta(PiecewiseLinearFunction negativeDelta) {
		this.negativeDelta = negativeDelta;
	}

	public Collection<Interval<Double>> getHoles() {
		return this.holes;
	}

	public void setHoles(Collection<Interval<Double>> holes) {
		this.holes = holes;
	}

	public List<Collection<Interval<Double>>> getAllHoles() {
		return allHoles;
	}

	public void setAllHoles(List<Collection<Interval<Double>>> allHoles) {
		this.allHoles = allHoles;
	};
}
