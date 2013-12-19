package de.uniaugsburg.isse.constraints;

import de.uniaugsburg.isse.abstraction.types.Interval;
import de.uniaugsburg.isse.powerplants.PowerPlantData;

public class BoundsConstraint extends PlantConstraint {

	private Interval<Double> boundaries;

	public BoundsConstraint(PowerPlantData pd) {
		this.boundaries = pd.getPowerBoundaries();
	}

	public BoundsConstraint(PowerPlantData pp2, double min, double max) {
		this.boundaries = new Interval<Double>(min, max);
	}

	@Override
	public double maximize() {
		return boundaries.max;
	}

	@Override
	public double minimize() {
		return boundaries.min;
	}

	public Interval<Double> getBoundaries() {
		return boundaries;
	}

}
