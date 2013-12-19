package de.uniaugsburg.isse.powerplants;

import java.lang.reflect.Constructor;

import de.uniaugsburg.isse.RandomManager;
import de.uniaugsburg.isse.abstraction.types.Interval;
import de.uniaugsburg.isse.constraints.Constraint;

public abstract class AbstractPowerplantFactory {
	protected Class<? extends Constraint>[] constraints;

	protected static int runningNo;

	protected abstract PowerPlantData create();

	public PowerPlantData createPowerplant() {
		PowerPlantData pd = create();
		for (Class<? extends Constraint> constraint : constraints) {
			Constructor<?>[] allConstructors = constraint
					.getDeclaredConstructors();
			for (Constructor<?> ctor : allConstructors) {
				Class<?>[] pType = ctor.getParameterTypes();
				if (pType.length == 1 && pType[0].equals(PowerPlantData.class)) {
					try {
						pd.addConstraint((Constraint) ctor.newInstance(pd));
						break;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		pd.setName("CPP_" + (runningNo++));
		return pd;
	}

	protected Interval<Double> createPowerInterval(double min, double max) {
		double first = RandomManager.getDouble(min, max);
		double second = RandomManager.getDouble(min, max);
		if (first > second)
			return new Interval<Double>(second, first);
		else
			return new Interval<Double>(first, second);
	}

}
