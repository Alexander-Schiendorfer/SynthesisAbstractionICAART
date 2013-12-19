package de.uniaugsburg.isse.constraints;

import de.uniaugsburg.isse.powerplants.PowerPlantData;
import de.uniaugsburg.isse.util.PowerPlantUtil;

/**
 * Expects a fixed rate of change that must not be exceeded per timestep in
 * contrast to relative rate of change constraints
 * 
 * @author Alexander Schiendorfer
 * 
 */
public class FixedChangeConstraint extends PlantConstraint {
	private double maxProductionChange; // relative rate of change per timestep

	/**
	 * Concrete physical limit rate of change constraint
	 * 
	 * @param p
	 *            - Power source
	 * @param maxProductionChange
	 *            - absolute amount of change
	 */
	public FixedChangeConstraint(PowerPlantData pd) {
		this.maxProductionChange = PowerPlantUtil.safeDouble(
				"maxProductionChange", pd.getMap());
	}

	@Override
	public double maximize() {
		return maxProductionChange + plant.getPower().max;
	}

	@Override
	public double minimize() {
		return plant.getPower().min - maxProductionChange;
	}

	public double getMaxProductionChange() {
		return maxProductionChange;
	}

	public void setMaxProductionChange(double maxProductionChange) {
		this.maxProductionChange = maxProductionChange;
	}
}
