package de.uniaugsburg.isse.abstraction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;

import de.uniaugsburg.isse.abstraction.merging.HoleDetection;
import de.uniaugsburg.isse.abstraction.types.Interval;
import de.uniaugsburg.isse.constraints.Constraint;
import de.uniaugsburg.isse.powerplants.PowerPlantData;
import de.uniaugsburg.isse.powerplants.PowerPlantState;
import de.uniaugsburg.isse.util.PowerPlantUtil;

/**
 * Performs temporally sensitive abstraction i.e. looks some timesteps ahead and
 * returns boundaries and holes for some future
 * 
 * @author Alexander Schiendorfer
 * 
 */
public class TemporalAbstraction extends GeneralAbstraction {

	protected Collection<Constraint> temporalConstraint;
	protected List<SortedSet<Interval<Double>>> allFeasibleRegions;
	protected List<Collection<Interval<Double>>> allHoles;
	private Double delta = 2.0; // delta to avoid having single points when
								// starting up

	/**
	 * performs time sensitive abstraction up to some time step t maximizes and
	 * minimizes in each step to get boundaries of the AVPP
	 * 
	 * @param t
	 */
	public final void perform(int T) {
		// first order set by P_min

		ArrayList<PowerPlantState> plantStates = new ArrayList<PowerPlantState>(
				powerPlants.size());
		ArrayList<PowerPlantData> avpps = new ArrayList<PowerPlantData>(
				powerPlants.size());

		HoleDetection hd = new HoleDetection();

		allFeasibleRegions = new ArrayList<SortedSet<Interval<Double>>>();
		allHoles = new ArrayList<Collection<Interval<Double>>>();
		SortedSet<Interval<Double>> feasibleRegions;
		Collection<Interval<Double>> holes;

		// collect initial state
		for (PowerPlantData pd : powerPlants) {
			if (pd.isAVPP()) {
				avpps.add(pd);
			} else {
				PowerPlantState newPlant = new PowerPlantState();
				newPlant.setData(pd);
				newPlant.initialize(); // take init values from data
				newPlant.updateConstraints();
				plantStates.add(newPlant);
			}
		}

		boolean reachHorizon = false; // all possible states can be reached
		for (int t = 1; t <= T && !reachHorizon; ++t) {
			// System.out.println("* --------------------- t = " + t +
			// " ------------------- ");
			Collection<Collection<Interval<Double>>> plantIntervalsList = new ArrayList<Collection<Interval<Double>>>();

			for (PowerPlantState pp : plantStates) {
				pp.setSimulationStep(t);
				// start with initial values for P_(t+1)
				double P_min_t_inc = -(Double.MAX_VALUE - 1);
				double P_max_t_inc = Double.MAX_VALUE;

				// same for On_(t+1)
				boolean On_min_t_inc = false;
				boolean On_max_t_inc = true;
				// System.out.println("Looking at: " + pp.getName());

				for (Constraint c : pp.getData().getAssociatedConstraints()) {
					if (!c.isSoft()) {
						// minimize step
						P_min_t_inc = Math.max(c.minimize(), P_min_t_inc);
						// none may say that pp can be on - default is false!
						On_min_t_inc = c.minimizeBool() || On_min_t_inc;
	
						// maximize step
						P_max_t_inc = Math.min(c.maximize(), P_max_t_inc);
						// all have to allow pp to be on - default is true
						On_max_t_inc = c.maximizeBool() && On_max_t_inc;
					}
				}

				pp.updateRunning(On_min_t_inc, On_max_t_inc);

				// System.out.println("------------ "+pp.getPower().min + " / "
				// + pp.getPower().max);
				if (!On_min_t_inc)
					P_min_t_inc = 0.0;
				if (On_max_t_inc) {
					P_max_t_inc = Math.max(P_max_t_inc, pp.getData()
							.getPowerBoundaries().min);
				}
				pp.getPower().min = P_min_t_inc;
				pp.getPower().max = P_max_t_inc;

				// intervals to add
				double add_int_min = Math.max(P_min_t_inc, pp.getData()
						.getPowerBoundaries().min);
				double add_int_max = Math.max(P_max_t_inc, pp.getData()
						.getPowerBoundaries().min + delta);
				// System.out.println("------------ "+add_int_min + " / " +
				// add_int_max);

				Interval<Double> addInt = new Interval<Double>(add_int_min,
						add_int_max);
				Collection<Interval<Double>> setIntervals = new ArrayList<Interval<Double>>();
				if (pp.onOrOff() || pp.onlyOff()) {
					setIntervals.add(PowerPlantUtil.getZero());
				}
				if (!pp.onlyOff()) {
					setIntervals.add(addInt);
				}
				plantIntervalsList.add(setIntervals);

				// System.out.println(pp.printState());
			}

			for (PowerPlantData avpp : avpps) {
				List<SortedSet<Interval<Double>>> avppRegions = avpp
						.getAllFeasibleRegions();
				int index = t - 1;

				if (avppRegions.size() > index) {
					Collection<Interval<Double>> timeRegions = avppRegions
							.get(index);
					plantIntervalsList.add(timeRegions);
				} else { // converged, use general abstraction
					plantIntervalsList.add(avpp.getFeasibleRegions());
				}
			}

			holes = hd.detectSupplyHoles(plantIntervalsList);
			feasibleRegions = hd.getIntervalList();

			reachHorizon = PowerPlantUtil.checkConvergence(feasibleRegions,
					getFeasibleRegions());
			// System.out.println("#### Feasible regions after t = "+t);
			/*
			 * for(Interval region : feasibleRegions) { System.out.print(region
			 * + " , "); } System.out.println();
			 */
			allFeasibleRegions.add(feasibleRegions);
			allHoles.add(holes);
		}

	}

	public List<SortedSet<Interval<Double>>> getAllFeasibleRegions() {
		return allFeasibleRegions;
	}

	public void setAllFeasibleRegions(
			List<SortedSet<Interval<Double>>> allFeasibleRegions) {
		this.allFeasibleRegions = allFeasibleRegions;
	}

	public List<Collection<Interval<Double>>> getAllHoles() {
		return allHoles;
	}

	public void setAllHoles(List<Collection<Interval<Double>>> allHoles) {
		this.allHoles = allHoles;
	}

	public void printAll() {
		int t = 0;
		for (SortedSet<Interval<Double>> regions : allFeasibleRegions) {
			System.out.println("#### Feasible regions after t = " + (++t));
			for (Interval<Double> region : regions) {
				System.out.print(region + " , ");
			}
			System.out.println();
		}

	}
}
