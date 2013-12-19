package de.uniaugsburg.isse.util;

import java.util.Comparator;

import de.uniaugsburg.isse.powerplants.PowerPlantData;

public class PowerPlantComparator implements Comparator<PowerPlantData> {
	@Override
	public int compare(PowerPlantData o1, PowerPlantData o2) {
		if (o1.getPowerBoundaries().min == o2.getPowerBoundaries().min)
			return ((Integer) o1.getPowerBoundaries().hashCode()).compareTo(o2.getPowerBoundaries().hashCode());
		return Double.compare(o1.getPowerBoundaries().min, o2.getPowerBoundaries().min);
	}
}
