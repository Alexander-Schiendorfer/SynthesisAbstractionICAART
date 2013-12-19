package de.uniaugsburg.isse.powerplants.factories;

import de.uniaugsburg.isse.RandomManager;
import de.uniaugsburg.isse.constraints.BoundsConstraint;
import de.uniaugsburg.isse.constraints.GraduallyOffConstraint;
import de.uniaugsburg.isse.constraints.RateOfChangeConstraint;
import de.uniaugsburg.isse.constraints.StopTimeConstraint;
import de.uniaugsburg.isse.powerplants.AbstractPowerplantFactory;
import de.uniaugsburg.isse.powerplants.PowerPlantData;
import de.uniaugsburg.isse.util.AbstractionParameterLiterals;

public class FossilFuelPowerplantFactory extends AbstractPowerplantFactory {

	public static final double RAND_P_MIN = 5000.0;
	public static final double RAND_P_MAX = 100000;
	private static final double changePerMax = 0.05;
	private static final double changePercMin = 0.20;

	@SuppressWarnings("unchecked")
	@Override
	public PowerPlantData create() {
		PowerPlantData pd = new PowerPlantData();
		pd.put(AbstractionParameterLiterals.MIN_OFF_TIME, "2");
		pd.put(AbstractionParameterLiterals.MIN_ON_TIME, "2");
		pd.put(AbstractionParameterLiterals.RATE_OF_CHANGE, Double
				.toString(RandomManager.getDouble(changePercMin, changePerMax)));

		pd.setPowerBoundaries(createPowerInterval(RAND_P_MIN, RAND_P_MAX));

		constraints = new Class[] { BoundsConstraint.class,
				RateOfChangeConstraint.class, GraduallyOffConstraint.class,
				StopTimeConstraint.class };
		return pd;
	}

}
