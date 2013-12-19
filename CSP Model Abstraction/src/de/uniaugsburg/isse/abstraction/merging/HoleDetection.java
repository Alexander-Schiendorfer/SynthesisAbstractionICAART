package de.uniaugsburg.isse.abstraction.merging;

import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

import de.uniaugsburg.isse.abstraction.merging.MergeLists.List;
import de.uniaugsburg.isse.abstraction.types.Interval;
import de.uniaugsburg.isse.util.PowerPlantUtil;

/**
 * Wraps merge list internal details and performs the core of the abstraction
 * algorithm i.e. merge intervals to find feasible regions and supply holes
 * 
 * @author Alexander Schiendorfer
 * 
 */
public class HoleDetection {
	private SortedSet<Interval<Double>> intervalList;

	public SortedSet<Interval<Double>> getIntervalList() {
		return intervalList;
	}

	public Collection<Interval<Double>> detectSupplyHoles(
			Collection<Collection<Interval<Double>>> plantIntervals) {
		boolean first = true;
		Collection<Interval<Double>> minIntervalSet = null;

		// do interval merging and combining
		for (Collection<Interval<Double>> plantIntervalSet : plantIntervals) {
			if (first) {
				minIntervalSet = plantIntervalSet;
				first = false;
			} else {
				minIntervalSet = PowerPlantUtil.plusSets(minIntervalSet,
						plantIntervalSet);
			}
			// TODO maybe simplify here
		}

		// print min interval set
		/*
		 * System.out.println("Min interval set"); for (Interval<Double> intV :
		 * minIntervalSet) { System.out.println(intV); }
		 * System.out.println("End min interval set");
		 */
		first = true;
		List listHead = null;

		// can happen to be empty during bootstrapping
		if (minIntervalSet != null && !minIntervalSet.isEmpty()) {
			for (Interval<Double> resultingIntervals : minIntervalSet) {
				// System.out.println("Merging in ... " + resultingIntervals);
				if (first) {
					listHead = new List(resultingIntervals, null);
					first = false;
				} else {
					listHead = List.mergeIn(listHead, resultingIntervals);
				}
				// MergeLists.printList(listHead);
			}
			intervalList = MergeLists.toJavaSet(listHead);

			// collect holes
			Collection<Interval<Double>> holes = new TreeSet<Interval<Double>>();
			if (listHead.size() > 1) {
				List curr = listHead;
				while (curr.getNext() != null) {
					holes.add(new Interval<Double>(curr.getInterval().max, curr
							.getNext().getInterval().min));
					curr = curr.getNext();
				}
			}
			return holes;
		} else {
			intervalList = new TreeSet<Interval<Double>>(); // empty set
															// suffices
			return new TreeSet<Interval<Double>>(); //
		}
	}
}
