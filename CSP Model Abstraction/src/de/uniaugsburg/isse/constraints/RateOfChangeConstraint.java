package de.uniaugsburg.isse.constraints;

import de.uniaugsburg.isse.powerplants.PowerPlantData;
import de.uniaugsburg.isse.util.PowerPlantUtil;

public class RateOfChangeConstraint extends PlantConstraint {

	private double rateOfChange; // relative rate of change per timestep

	/**
	 * Concrete physical limit rate of change constraint
	 * 
	 * @param p
	 *            - Power source
	 * @param rateOfChange
	 *            - relative rate of change \in [0, 1]
	 */
	public RateOfChangeConstraint(PowerPlantData pd) {
		this.rateOfChange = PowerPlantUtil.safeDouble("rateOfChange",
				pd.getMap());
	}

	public RateOfChangeConstraint(PowerPlantData pp1, double rate) {
		this.rateOfChange = rate;
	}

	@Override
	public double maximize() {
		return (1.0 + rateOfChange) * plant.getPower().max;
	}

	@Override
	public double minimize() {
		return (1.0 - rateOfChange) * plant.getPower().min;
	}

	public double getRateOfChange() {
		return rateOfChange;
	}

	public void setRateOfChange(double rateOfChange) {
		this.rateOfChange = rateOfChange;
	}
}
