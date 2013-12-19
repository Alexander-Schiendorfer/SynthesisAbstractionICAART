package de.uniaugsburg.isse.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import de.uniaugsburg.isse.abstraction.types.Interval;
import de.uniaugsburg.isse.constraints.BoundsConstraint;
import de.uniaugsburg.isse.constraints.Constraint;
import de.uniaugsburg.isse.constraints.FixedChangeConstraint;
import de.uniaugsburg.isse.constraints.ForceOnConstraint;
import de.uniaugsburg.isse.constraints.GraduallyOffConstraint;
import de.uniaugsburg.isse.constraints.RateOfChangeConstraint;
import de.uniaugsburg.isse.constraints.StartWithMinConstraint;
import de.uniaugsburg.isse.powerplants.PowerPlantData;
import de.uniaugsburg.isse.powerplants.PowerPlantState;
import de.uniaugsburg.isse.solver.AbstractModel;
import de.uniaugsburg.isse.solver.AbstractSolver;

public class PowerPlantUtil {
	private static double testEps = 0.1;

	public final static Collection<Interval<Double>> extract(Collection<PowerPlantData> data) {
		ArrayList<Interval<Double>> intervals = new ArrayList<Interval<Double>>(data.size());

		for (PowerPlantData pd : data) {
			intervals.add(pd.getPowerBoundaries().copy());
		}
		return intervals;
	}

	private static PowerPlantData getCanonicalPlant(String name, double pMin, double pMax, double relDelta) {
		PowerPlantData pd = new PowerPlantData(name);
		pd.setPowerBoundaries(new Interval<Double>(pMin, pMax));

		pd.addConstraint(new BoundsConstraint(pd));
		pd.addConstraint(new GraduallyOffConstraint(pd));
		pd.addConstraint(new StartWithMinConstraint(pd));

		pd.put(AbstractionParameterLiterals.CONSRUNNING_INIT, "1");
		pd.put(AbstractionParameterLiterals.CONSSTOPPING_INIT, "0");
		pd.put(AbstractionParameterLiterals.POWER_INIT, Double.toString(pMin));

		return pd;
	}

	public static PowerPlantData getPowerPlant(String name, double pMin, double pMax, double relDelta) {
		PowerPlantData pd = getCanonicalPlant(name, pMin, pMax, relDelta);
		pd.put(AbstractionParameterLiterals.RATE_OF_CHANGE, Double.toString(relDelta));
		pd.addConstraint(new RateOfChangeConstraint(pd));
		return pd;
	}

	public static PowerPlantData getPowerPlantFixed(String name, double pMin, double pMax, double fixedDelta) {
		PowerPlantData pd = getCanonicalPlant(name, pMin, pMax, fixedDelta);
		pd.put(AbstractionParameterLiterals.MAX_PROD_CHANGE, Double.toString(fixedDelta));
		pd.addConstraint(new FixedChangeConstraint(pd));

		return pd;
	}

	public static void addState(Map<String, PowerPlantState> allStates, PowerPlantData pd) {
		if (allStates != null) {
			PowerPlantState initState = new PowerPlantState();
			initState.setPower(new Interval<Double>(pd.getPowerBoundaries().min));
			initState.setConsRunning(new Interval<Integer>(1));
			initState.setConsStopping(new Interval<Integer>(0));
			allStates.put(pd.getName(), initState);
		}
	}

	public static double safeDouble(String key, Map<String, String> map) {
		if (map.containsKey(key))
			return Double.parseDouble(map.get(key));
		else
			return 0.0;
	}

	public static int safeInt(String key, Map<String, String> map) {
		if (map.containsKey(key))
			return Integer.parseInt(map.get(key));
		else
			return 0;
	}

	/**
	 * Check whether two feasible region sets converge
	 * 
	 * @param feasibleRegions
	 * @param feasibleRegions2
	 * @return
	 */
	public static boolean checkConvergence(SortedSet<Interval<Double>> feasibleRegions, SortedSet<Interval<Double>> feasibleRegions2) {
		if (feasibleRegions == null || feasibleRegions2 == null)
			return false;
		else {
			return PowerPlantUtil.compareIntervals(feasibleRegions, feasibleRegions2);
		}
	}

	public static boolean compareIntervals(Collection<Interval<Double>> expecteds, Collection<Interval<Double>> actuals) {
		if (expecteds == null || actuals == null)
			return expecteds == null && actuals == null;
		else {
			if (expecteds.size() != actuals.size())
				return false;
			else {
				Iterator<Interval<Double>> actualIterator = actuals.iterator();
				for (Interval<Double> expected : expecteds) {
					Interval<Double> actual = actualIterator.next();
					if (!PowerPlantUtil.compareInterval(expected, actual))
						return false;
				}
				return true;
			}
		}
	}

	public static boolean compareInterval(Interval<Double> expected, Interval<Double> actual) {
		return PowerPlantUtil.compareApprox(expected.min, actual.min) && PowerPlantUtil.compareApprox(expected.max, actual.max);
	}

	public static boolean compareApprox(Double first, Double second) {
		return Math.abs(first - second) <= PowerPlantUtil.testEps;
	}

	/**
	 * Implements interval double plus operator for abstraction
	 * 
	 * @param i1
	 * @param i2
	 * @return
	 */
	public static de.uniaugsburg.isse.abstraction.types.Interval<Double> plus(de.uniaugsburg.isse.abstraction.types.Interval<Double> i1,
			de.uniaugsburg.isse.abstraction.types.Interval<Double> i2) {
		double min = i1.min + i2.min, max = i1.max + i2.max;

		return new Interval<Double>(min, max);
	}

	public static Collection<Interval<Double>> plus_multi(Interval<Double> interval, Collection<Interval<Double>> otherIntervals) {
		ArrayList<Interval<Double>> combineds = new ArrayList<Interval<Double>>(otherIntervals.size());
		for (Interval<Double> otherInterval : otherIntervals) {
			combineds.add(PowerPlantUtil.plus(interval, otherInterval));
		}
		return combineds;
	}

	public static Collection<Interval<Double>> plusSets(Collection<Interval<Double>> intervals, Collection<Interval<Double>> otherIntervals) {
		ArrayList<Interval<Double>> combineds = new ArrayList<Interval<Double>>(otherIntervals.size());
		for (Interval<Double> interval : intervals) {
			for (Interval<Double> otherInterval : otherIntervals) {
				combineds.add(PowerPlantUtil.plus(interval, otherInterval));
			}
		}
		return combineds;
	}

	public static Collection<Collection<Interval<Double>>> extractMultiple(Set<PowerPlantData> data) {
		ArrayList<Collection<Interval<Double>>> intervals = new ArrayList<Collection<Interval<Double>>>(data.size());

		for (PowerPlantData pd : data) {
			if (pd.getFeasibleRegions() == null) { // no AVPP assumed
				ArrayList<Interval<Double>> singletonSet = new ArrayList<Interval<Double>>();
				singletonSet.add(pd.getPowerBoundaries().copy());
				intervals.add(singletonSet);
			} else {
				intervals.add(pd.getFeasibleRegions());
			}
		}
		return intervals;
	}

	public static Collection<Collection<Interval<Double>>> extractFromPlants(Collection<PowerPlantData> data) {
		ArrayList<Collection<Interval<Double>>> intervals = new ArrayList<Collection<Interval<Double>>>(data.size());

		for (PowerPlantData pd : data) {
			if (pd.getFeasibleRegions() == null) { // no AVPP assumed
				// check if pd can be off; for now just ask for the ForcedOnConstraint
				ArrayList<Interval<Double>> singletonSet = new ArrayList<Interval<Double>>();

				if (PowerPlantUtil.canBeOff(pd)) { // add zero element
					singletonSet.add(PowerPlantUtil.getZero());
				}
				singletonSet.add(pd.getPowerBoundaries().copy());
				intervals.add(singletonSet);
			} else {
				intervals.add(pd.getFeasibleRegions());
			}
		}
		return intervals;
	}

	public static Interval<Double> getZero() {
		return new Interval<Double>(0.0);
	}

	private static boolean canBeOff(PowerPlantData pd) {
		if (pd.getAssociatedConstraints() != null) {
			for (Constraint c : pd.getAssociatedConstraints()) {
				if (c instanceof ForceOnConstraint)
					return false;
			}
		}
		return true;
	}

	/**
	 * Populates a model for sampling abstraction with the default decision expressions needed in the power management
	 * example
	 * 
	 * @param solver
	 * @param model
	 * @param avpp
	 * @param children
	 */
	public static void populateDefaultSamplingModel(AbstractSolver solver, AbstractModel model, PowerPlantData avpp, Collection<PowerPlantData> children) {
		model.setPlantData(avpp, children);
		Collection<String> dexprs = new ArrayList<String>(2);

		dexprs.add(AbstractionParameterLiterals.DEXP_POWER + "Init = " + AbstractionParameterLiterals.DEXP_POWER + "[0]");
		dexprs.add(AbstractionParameterLiterals.DEXP_POWER + "Succ = " + AbstractionParameterLiterals.DEXP_POWER + "[1]");
		dexprs.add(AbstractionParameterLiterals.DEXP_COSTS + "Init = " + AbstractionParameterLiterals.DEXP_COSTS + "[0]");
		model.addDecisionExpressions(dexprs);
		model.setCosts(true);
		solver.setModel(model);
	}

	public static boolean compareIntervalSets(Collection<Collection<Interval<Double>>> expecteds, Collection<Collection<Interval<Double>>> actuals) {
		if (expecteds == null || actuals == null)
			return expecteds == null && actuals == null;
		else {
			if (expecteds.size() != actuals.size())
				return false;
			else {
				Iterator<Collection<Interval<Double>>> actualIterator = actuals.iterator();
				for (Collection<Interval<Double>> expected : expecteds) {
					Collection<Interval<Double>> actual = actualIterator.next();
					if (!PowerPlantUtil.compareIntervals(expected, actual))
						return false;
				}
				return true;
			}
		}
	}

	public static Collection<Collection<Interval<Double>>> convert(List<SortedSet<Interval<Double>>> setOfSetOfIntervals) {
		List<Collection<Interval<Double>>> newList = new ArrayList<Collection<Interval<Double>>>(setOfSetOfIntervals.size());
		for (SortedSet<Interval<Double>> element : setOfSetOfIntervals) {
			newList.add(element);
		}
		return newList;
	}

	/**
	 * For this operation to make sense, minIntervalSet must not be null
	 * 
	 * @param minIntervalSet
	 * @param plantIntervalSet
	 * @return
	 */
	public static de.uniaugsburg.isse.abstraction.merging.MergeLists.List plusSets(de.uniaugsburg.isse.abstraction.merging.MergeLists.List minIntervalSet,
			Collection<Interval<Double>> plantIntervalSet) {
		// first collect every combined interval, then merge them all in
		if (minIntervalSet == null)
			return null;

		de.uniaugsburg.isse.abstraction.merging.MergeLists.List head = minIntervalSet;
		Collection<Interval<Double>> newCombinations = new ArrayList<Interval<Double>>(head.size() * plantIntervalSet.size());
		while (head != null) {
			Interval<Double> leftSide = head.getInterval();
			for (Interval<Double> rightSide : plantIntervalSet) {
				newCombinations.add(plus(leftSide, rightSide));
			}
			head = head.getNext();
		}
		for (Interval<Double> newCombination : newCombinations) {
			minIntervalSet = de.uniaugsburg.isse.abstraction.merging.MergeLists.List.mergeIn(minIntervalSet, newCombination);
		}
		return minIntervalSet;
	}

}
