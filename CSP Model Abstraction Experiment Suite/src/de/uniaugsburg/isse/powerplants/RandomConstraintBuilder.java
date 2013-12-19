package de.uniaugsburg.isse.powerplants;

import java.util.Collection;

import de.uniaugsburg.isse.RandomManager;
import de.uniaugsburg.isse.constraints.Constraint;
import de.uniaugsburg.isse.constraints.FixedChangeConstraint;
import de.uniaugsburg.isse.constraints.RateOfChangeConstraint;
import de.uniaugsburg.isse.constraints.StopTimeConstraint;
import de.uniaugsburg.isse.util.AbstractionParameterLiterals;

public class RandomConstraintBuilder {

	private static final double rateOfChangeMin = 0.05;
	private static final double rateOfChangeMax = 0.15;
	private static final int minOffTimeMax = 10;

	public static void addChangeConstraints(Collection<PowerPlantData> plants) {
		for (PowerPlantData pd : plants) {
			Constraint changeConstraint = null;
			if (RandomManager.getBoolean(.5)) {
				double rateOfChange = RandomManager.getDouble(rateOfChangeMin,
						rateOfChangeMax);
				pd.put(AbstractionParameterLiterals.RATE_OF_CHANGE,
						Double.toString(rateOfChange));
				changeConstraint = new RateOfChangeConstraint(pd);
			} else {
				double rateOfChange = RandomManager.getDouble(rateOfChangeMin,
						rateOfChangeMax);
				double fixedChange = rateOfChange * pd.getPowerBoundaries().max;
				pd.put(AbstractionParameterLiterals.MAX_PROD_CHANGE,
						Double.toString(fixedChange));
				changeConstraint = new FixedChangeConstraint(pd);
			}
			pd.addConstraint(changeConstraint);
		}

	}

	public static void addStopTimeConstraints(
			Collection<PowerPlantData> bioPlants) {
		for (PowerPlantData pd : bioPlants) {

			int minOffTime = 0;// 1 + RandomManager.getInt(minOffTimeMax);

			pd.put(AbstractionParameterLiterals.MIN_OFF_TIME,
					Integer.toString(minOffTime));
			pd.addConstraint(new StopTimeConstraint(pd));
		}

	}
}
