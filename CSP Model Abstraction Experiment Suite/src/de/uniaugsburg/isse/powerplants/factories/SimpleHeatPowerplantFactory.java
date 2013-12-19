package de.uniaugsburg.isse.powerplants.factories;

import de.uniaugsburg.isse.RandomManager;
import de.uniaugsburg.isse.abstraction.types.Interval;
import de.uniaugsburg.isse.constraints.BoundsConstraint;
import de.uniaugsburg.isse.constraints.FixedChangeConstraint;
import de.uniaugsburg.isse.constraints.ForceOnConstraint;
import de.uniaugsburg.isse.constraints.GraduallyOffConstraint;
import de.uniaugsburg.isse.powerplants.AbstractPowerplantFactory;
import de.uniaugsburg.isse.powerplants.PowerPlantData;
import de.uniaugsburg.isse.util.AbstractionParameterLiterals;

public class SimpleHeatPowerplantFactory extends AbstractPowerplantFactory {
	private static final double RAND_P_MIN = 5000.0;
	private static final double RAND_P_MAX = 100000.0;
	private static final double changePerMax = 0.05;
	private static final double changePercMin = 0.20;

	@SuppressWarnings("unchecked")
	@Override
	protected PowerPlantData create() {
		PowerPlantData pd = new PowerPlantData();
		Interval<Double> power = createPowerInterval(RAND_P_MIN, RAND_P_MAX);

		pd.setPowerBoundaries(power);

		double change = RandomManager.getDouble(changePercMin, changePerMax);

		pd.put(AbstractionParameterLiterals.MAX_PROD_CHANGE,
				Double.toString(power.max * change));

		constraints = new Class[] { BoundsConstraint.class,
				FixedChangeConstraint.class, GraduallyOffConstraint.class,
				ForceOnConstraint.class };
		return pd;
	}

}
