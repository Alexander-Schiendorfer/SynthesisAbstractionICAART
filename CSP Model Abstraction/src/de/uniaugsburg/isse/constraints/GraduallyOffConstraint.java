package de.uniaugsburg.isse.constraints;

import de.uniaugsburg.isse.powerplants.PowerPlantData;
import de.uniaugsburg.isse.util.PowerPlantUtil;

public class GraduallyOffConstraint extends PlantConstraint {

	private double P_min;
	private double rateOfChange;

	// no preferences when maximizing
	public GraduallyOffConstraint(PowerPlantData pd) {
		this.P_min = pd.getPowerBoundaries().min;
		this.rateOfChange = PowerPlantUtil.safeDouble("rateOfChange",
				pd.getMap());

	}

	/**
	 * Has to return the on status therefore inverting the condition
	 */
	@Override
	public boolean minimizeBool() {
		double p_next = (1.0 - rateOfChange) * getPlant().getPower().min;
		return (p_next > P_min);
	}

}
