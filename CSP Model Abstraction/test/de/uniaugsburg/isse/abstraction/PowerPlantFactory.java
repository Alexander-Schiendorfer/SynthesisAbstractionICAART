package de.uniaugsburg.isse.abstraction;

import java.util.ArrayList;
import java.util.Collection;

import de.uniaugsburg.isse.abstraction.types.Interval;
import de.uniaugsburg.isse.constraints.BoundsConstraint;
import de.uniaugsburg.isse.constraints.FixedChangeConstraint;
import de.uniaugsburg.isse.constraints.ForceOnConstraint;
import de.uniaugsburg.isse.constraints.GraduallyOffConstraint;
import de.uniaugsburg.isse.powerplants.PowerPlantData;
import de.uniaugsburg.isse.util.AbstractionParameterLiterals;

public class PowerPlantFactory {
	
	/**
	 * Returns a simple powerplant data without any associated constraints
	 * other than bounds; can be turned off
	 * @param min
	 * @param max
	 * @return
	 */
	public static PowerPlantData getSimplePlant(double min, double max, String name) {
		PowerPlantData pd = new PowerPlantData(name);
		pd.setPowerBoundaries(new Interval<Double>(min, max));
		pd.addConstraint(new BoundsConstraint(pd));
		GraduallyOffConstraint goc = new GraduallyOffConstraint(pd);
		pd.addConstraint(goc);
		return pd;
	}
	
	/**
	 * Same as getSimplePlant, but powerplant has to be on!
	 * @param min
	 * @param max
	 * @return
	 */
	public static PowerPlantData getOnPlant(double min, double max, String name) {
		PowerPlantData pd = getSimplePlant(min, max, name);
		pd.addConstraint(new ForceOnConstraint());
		return pd;
	}


	public static PowerPlantData getOnPlant(double min, double max, double current,
			double fixedChange, String name) {
		PowerPlantData pd = getOnPlant(min, max, name);
		initDefaultTemporal(pd);
		
		pd.put(AbstractionParameterLiterals.MAX_PROD_CHANGE, Double.toString(fixedChange));
		pd.put(AbstractionParameterLiterals.POWER_INIT, Double.toString(current));
		pd.addConstraint(new FixedChangeConstraint(pd));
		return pd;
	}

	public static PowerPlantData getSimplePlant(double min, double max, double current,
			double fixedChange, String name) {
		PowerPlantData pd = getSimplePlant(min, max, name);
		initDefaultTemporal(pd);
		
		pd.put(AbstractionParameterLiterals.MAX_PROD_CHANGE, Double.toString(fixedChange));
		pd.put(AbstractionParameterLiterals.POWER_INIT, Double.toString(current));
		pd.addConstraint(new FixedChangeConstraint(pd));
		return pd;
	}

	private static void initDefaultTemporal(PowerPlantData pd) {
		pd.put(AbstractionParameterLiterals.CONSRUNNING_INIT, "1");
		pd.put(AbstractionParameterLiterals.CONSSTOPPING_INIT, "0");
		
	}

	public static Collection<Interval<Double>> getSingletonCollection(double min,
			double max) {
		ArrayList<Interval<Double>> singleton = new ArrayList<Interval<Double>>(1);
		singleton.add(new Interval<Double>(min, max));
		return singleton;
	}

	public static Collection<Interval<Double>> getCollection(
			double[] ds) {
		Collection<Interval<Double>> list = new ArrayList<Interval<Double>>(ds.length/2);
		for(int i = 0; i < ds.length; i+=2) {
			list.add(new Interval<Double>(ds[i], ds[i+1]));
		}
		return list;
	}

}
