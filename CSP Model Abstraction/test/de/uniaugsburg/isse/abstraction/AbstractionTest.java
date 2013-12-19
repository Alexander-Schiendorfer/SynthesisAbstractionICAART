package de.uniaugsburg.isse.abstraction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import de.uniaugsburg.isse.abstraction.types.Interval;
import de.uniaugsburg.isse.constraints.BoundsConstraint;
import de.uniaugsburg.isse.constraints.FixedChangeConstraint;
import de.uniaugsburg.isse.constraints.ForceOnConstraint;
import de.uniaugsburg.isse.constraints.GraduallyOffConstraint;
import de.uniaugsburg.isse.constraints.RateOfChangeConstraint;
import de.uniaugsburg.isse.constraints.StopTimeConstraint;
import de.uniaugsburg.isse.powerplants.PowerPlantData;
import de.uniaugsburg.isse.util.AbstractionParameterLiterals;

public class AbstractionTest {
	private Collection<PowerPlantData> plantData;

	@Before
	public void setup() {
		plantData = new ArrayList<PowerPlantData>(3);

		// -- first sample plant
		PowerPlantData p1 = new PowerPlantData("P1");
		p1.setPowerBoundaries(new Interval<Double>(24.0, 36.0));

		p1.put("consRunningInit", "0");
		p1.put("consStoppingInit", "1");
		p1.put("minOffTime", "2");
		p1.put("minOnTime", "2");
		p1.put("rateOfChange", "0.15");
		p1.put("startupSlope", "1.0");
		p1.put("powerInit", "0.0");

		plantData.add(p1);

		// -- second sample plant
		PowerPlantData p2 = new PowerPlantData("P2");
		p2.setPowerBoundaries(new Interval<Double>(15.0, 20.0));
		p2.put("consRunningInit", "1");
		p2.put("consStoppingInit", "0");
		p2.put("minOffTime", "2");
		p2.put("minOnTime", "2");
		p2.put("rateOfChange", "0.125");
		p2.put("startupSlope", "1.0");
		p2.put("powerInit", "18.0");

		plantData.add(p2);

		// -- third sample plant
		PowerPlantData p3 = new PowerPlantData("P3");
		p3.setPowerBoundaries(new Interval<Double>(20.0, 45.0));
		p3.put("consRunningInit", "1");
		p3.put("consStoppingInit", "0");
		p3.put("minOffTime", "2");
		p3.put("minOnTime", "2");
		p3.put("rateOfChange", "0.2");
		p3.put("startupSlope", "1.0");
		p3.put("powerInit", "35.0");
		plantData.add(p3);

		// add constraints
		for (PowerPlantData pd : plantData) {
			RateOfChangeConstraint roc = new RateOfChangeConstraint(pd);
			pd.addConstraint(roc);

			BoundsConstraint bc = new BoundsConstraint(pd);
			pd.addConstraint(bc);

			GraduallyOffConstraint goc = new GraduallyOffConstraint(pd);
			pd.addConstraint(goc);

			StopTimeConstraint stc = new StopTimeConstraint(pd);
			pd.addConstraint(stc);
		}
	}

	@Test
	public void testGeneralAbstraction() {
		System.out.println("------------- General abstraction ------------");
		GeneralAbstraction ga = new GeneralAbstraction();
		ga.setPowerPlants(plantData);
		ga.perform();
		ga.print();

		System.out.println("Now for something completely different");
		ga.performNew();
		ga.print();
		System.out
				.println("------------- End general abstraction ------------");
	}

	@Test
	public void testTemporalAbstraction() {
		TemporalAbstraction ta = new TemporalAbstraction();
		ta.setPowerPlants(plantData);
		int T = 5; // up to T approximate
		ta.perform(T);
		ta.printAll();
	}

	@Test
	@Ignore
	public void testMultiIntervalPlants() {
		SortedSet<Interval<Double>> firstPlantSet = new TreeSet<Interval<Double>>();
		SortedSet<Interval<Double>> secondPlantSet = new TreeSet<Interval<Double>>();
		firstPlantSet.add(new Interval<Double>(16.0, 20.0));
		firstPlantSet.add(new Interval<Double>(24.0, 30.0));

		secondPlantSet.add(new Interval<Double>(4.0, 7.0));
		secondPlantSet.add(new Interval<Double>(13.0, 15.0));

		PowerPlantData firstPlant = new PowerPlantData();
		firstPlant.setFeasibleRegions(firstPlantSet);
		firstPlant.setPowerBoundaries(new Interval<Double>(16.0, 30.0));

		PowerPlantData secondPlant = new PowerPlantData();
		secondPlant.setFeasibleRegions(secondPlantSet);
		secondPlant.setPowerBoundaries(new Interval<Double>(4.0, 15.0));

		Collection<PowerPlantData> plants = new ArrayList<PowerPlantData>(2);
		plants.add(firstPlant);
		plants.add(secondPlant);

		GeneralAbstraction ga = new GeneralAbstraction();
		ga.setPowerPlants(plants);
		ga.perform();

		for (Interval<Double> region : ga.getFeasibleRegions()) {
			System.out.println(region);
		}
	}

	@Test
	@Ignore
	public void testConcreteExample() {

		PowerPlantData pd1 = new PowerPlantData("CPP1");
		pd1.setPowerBoundaries(new Interval<Double>(0.0, 100.0));
		pd1.put(AbstractionParameterLiterals.POWER_INIT, "10.0");
		pd1.put(AbstractionParameterLiterals.CONSRUNNING_INIT, "1");
		pd1.put(AbstractionParameterLiterals.CONSSTOPPING_INIT, "0");
		pd1.put(AbstractionParameterLiterals.MAX_PROD_CHANGE, "10.0");

		PowerPlantData pd2 = new PowerPlantData("CPP2");
		pd2.setPowerBoundaries(new Interval<Double>(0.0, 100.0));
		pd2.put(AbstractionParameterLiterals.POWER_INIT, "90.0");
		pd2.put(AbstractionParameterLiterals.CONSRUNNING_INIT, "1");
		pd2.put(AbstractionParameterLiterals.CONSSTOPPING_INIT, "0");
		pd2.put(AbstractionParameterLiterals.MAX_PROD_CHANGE, "20.0");

		Collection<PowerPlantData> plants = new ArrayList<PowerPlantData>(2);
		plants.add(pd1);
		plants.add(pd2);

		for (PowerPlantData pd : plants) {
			BoundsConstraint bc = new BoundsConstraint(pd);
			pd.addConstraint(bc);

			FixedChangeConstraint fcc = new FixedChangeConstraint(pd);
			pd.addConstraint(fcc);

			ForceOnConstraint foc = new ForceOnConstraint();
			pd.addConstraint(foc);
		}
		TemporalAbstraction ta = new TemporalAbstraction();
		ta.setPowerPlants(plants);
		ta.perform(5);
		ta.printAll();
	}

	@Test
	public void testThesisExample() {
		System.out.println("THESIS EXAMPLE");
		PowerPlantData pd1 = new PowerPlantData("a");
		pd1.setPowerBoundaries(new Interval<Double>(2., 5.));

		PowerPlantData pd2 = new PowerPlantData("b");
		pd2.setPowerBoundaries(new Interval<Double>(7., 10.0));

		PowerPlantData pd3 = new PowerPlantData("c");
		pd3.setPowerBoundaries(new Interval<Double>(25., 30.0));

		Collection<PowerPlantData> plants = new ArrayList<PowerPlantData>(3);
		plants.add(pd1);
		plants.add(pd2);
		plants.add(pd3);

		GeneralAbstraction ga = new GeneralAbstraction();
		ga.setPowerPlants(plants);
		ga.perform();
		ga.print();
	}

}
