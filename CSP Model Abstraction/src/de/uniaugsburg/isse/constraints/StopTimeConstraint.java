package de.uniaugsburg.isse.constraints;

import de.uniaugsburg.isse.powerplants.PowerPlantData;
import de.uniaugsburg.isse.util.PowerPlantUtil;

public class StopTimeConstraint extends PlantConstraint {
	private int minOffTime;
	private int minOnTime;
	
	public StopTimeConstraint(PowerPlantData pd) {
		this.minOffTime = PowerPlantUtil.safeInt("minOffTime", pd.getMap());
		this.minOnTime = PowerPlantUtil.safeInt("minOnTime", pd.getMap());
	}

	/**
	 * MinStopTS is only relevant if we want to maximize (since we're in a hurry to get back running)
	 */
	@Override
	public boolean maximizeBool() {
		return getPlant().isRunning().max || getPlant().getConsStopping().max - minOffTime >= 0;
	}
	
	/**
	 * MinRunTS is only relevant when minimizing, since we want to get down
	 */
	@Override
	public boolean minimizeBool() {
		// more verbatim since more inconvenient to think about
		if(!getPlant().isRunning().min)
			return false;
		else { // if the condition is met, we might allow for false, hence the negation
			return !(getPlant().getConsRunning().min - minOnTime >= 0);
		}
	}
}
