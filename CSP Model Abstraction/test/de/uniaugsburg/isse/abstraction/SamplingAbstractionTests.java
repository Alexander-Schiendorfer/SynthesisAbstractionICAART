package de.uniaugsburg.isse.abstraction;

import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;

import de.uniaugsburg.isse.abstraction.types.Interval;
import de.uniaugsburg.isse.abstraction.types.PiecewiseLinearFunction;
import de.uniaugsburg.isse.powerplants.PowerPlantData;
import de.uniaugsburg.isse.solver.AbstractModel;
import de.uniaugsburg.isse.solver.AbstractSolver;
import de.uniaugsburg.isse.util.AbstractionParameterLiterals;

public class SamplingAbstractionTests {

	private static class StubSolver extends AbstractSolver {

		private class StubModel implements AbstractModel {

			@Override
			public void addEqualityConstraint(String name, String decExpr, double value, double tolerance) {
				System.out.println("Constraint model setting " + decExpr + " = " + value + " (" + name + ")");
			}

			@Override
			public void setObjective(String decExpr, boolean maximize) {
				System.out.println("Setting objective to " + (maximize ? "maximize" : "minimize") + " " + decExpr);
			}

			public Collection<PowerPlantData> getChildren() {
				return null;
			}

			public PowerPlantData getAvpp() {
				return null;
			}

			@Override
			public void setPlantData(PowerPlantData avpp, Collection<PowerPlantData> children) {
				// TODO Auto-generated method stub

			}

			public void setTimeHorizon(int timeHorizon) {
			}

			public int getTimeHorizon() {
				return 0;
			}

			@Override
			public void setFeasibleRegions(SortedSet<Interval<Double>> generalFeasibleRegions) {
				// TODO Auto-generated method stub

			}

			public void setGeneralHoles(Collection<Interval<Double>> generalHoles) {
			}

			public Collection<Interval<Double>> getGeneralHoles() {
				return null;
			}

			public void addDecisionExpressions(Collection<String> dexprs) {
			}

			@Override
			public void requireEqualBound() {
				// TODO Auto-generated method stub

			}

			@Override
			public void setInputExpression(String inputExpr) {
				// TODO Auto-generated method stub

			}

			@Override
			public void addInputLowerBoundConstraint(double lowerBound) {
				// TODO Auto-generated method stub

			}

			@Override
			public void addOutputLowerBound(String decExpr, double prevResult) {
				// TODO Auto-generated method stub

			}

			@Override
			public void setCosts(boolean b) {
				// TODO Auto-generated method stub

			}

			public void setUseSoftConstraints(boolean useSoftConstraints) {
			}

			public boolean isUseSoftConstraints() {
				return false;
			}

		}

		public StubSolver() {
			this.setModel(new StubModel());
		}

		@Override
		public void solve() {
			System.out.println("Solving");
		}

		@Override
		public double getResult(String decExpr) {
			System.out.println("Returning result for " + decExpr);
			return 42;
		}

		@Override
		public void cleanup() {
			// TODO Auto-generated method stub

		}

		@Override
		public double getObjective() {
			// TODO Auto-generated method stub
			return 0;
		}

	}

	@Test
	public void testTooLittleSamplePoints() {
		SortedSet<Interval<Double>> feasibleRegions = new TreeSet<Interval<Double>>();
		feasibleRegions.add(new Interval<Double>(1.0, 4.0));
		feasibleRegions.add(new Interval<Double>(6.0, 12.0));
		feasibleRegions.add(new Interval<Double>(14.0, 25.0));

		SamplingAbstraction sa = new SamplingAbstraction(feasibleRegions, null);
		sa.perform(0);

		List<Double> inputPoints = sa.getInputPoints();
		Assert.assertEquals(6, inputPoints.size());
		Double[] actuals = new Double[6];
		actuals = inputPoints.toArray(actuals);

		Double[] expecteds = new Double[] { 1.0, 4.0, 6.0, 12.0, 14.0, 25.0 };
		Assert.assertArrayEquals(expecteds, actuals);
	}

	@Test
	public void testPwLinearFunction() {
		double[] inputs = new double[] { 1.0, 2.0, 3.0, 4.0 };
		double[] outputs = new double[] { 4.0, 7.0, 5.0, 9.0 };
		PiecewiseLinearFunction pwlFunction = new PiecewiseLinearFunction();

		pwlFunction.convert(inputs, outputs);
		String s = pwlFunction.toCplex();
		System.out.println(s);
	}

	@Test
	public void simpleSamplingInputs() {
		// tests with a single feasible region [1, 10] and 5 sampling points
		/*
		 * np.linspace(1, 10, 5) Out[2]: array([ 1. , 3.25, 5.5 , 7.75, 10. ])
		 */
		SortedSet<Interval<Double>> feasibleRegions = new TreeSet<Interval<Double>>();
		feasibleRegions.add(new Interval<Double>(1.0, 10.0));

		SamplingAbstraction sa = new SamplingAbstraction(feasibleRegions, null);
		sa.perform(5);

		List<Double> inputs = sa.getInputPoints();
		Assert.assertEquals(5, inputs.size());

		Double[] expecteds = new Double[] { 1.0, 3.25, 5.5, 7.75, 10. };
		Double[] actuals = new Double[5];
		actuals = inputs.toArray(actuals);

		Assert.assertArrayEquals(expecteds, actuals);
	}

	@Test
	public void samplingMoreIntervalInputs() {
		// tests with a single feasible region [1, 10] and 5 sampling points
		/*
		 * np.linspace(1, 10, 5) Out[2]: array([ 1. , 3.25, 5.5 , 7.75, 10. ])
		 */
		SortedSet<Interval<Double>> feasibleRegions = new TreeSet<Interval<Double>>();
		feasibleRegions.add(new Interval<Double>(1.0, 4.0));
		feasibleRegions.add(new Interval<Double>(6.0, 10.0));
		feasibleRegions.add(new Interval<Double>(15.0, 35.0));

		SamplingAbstraction sa = new SamplingAbstraction(feasibleRegions, null);
		sa.perform(10);

		List<Double> inputs = sa.getInputPoints();
		// 4.0 coincides with one of the interval boundaries, otherwise 14
		Assert.assertEquals(13, inputs.size());

		Double[] expecteds = new Double[] { 1.0, 4.0, 6.0, 9.0, 10., 15., 17., 20., 23., 26., 29., 32., 35. };
		Double[] actuals = new Double[13];
		actuals = inputs.toArray(actuals);

		Assert.assertArrayEquals(expecteds, actuals);
	}

	@Test
	public void samplingJumpOverOneIntervalInputs() {
		// tests with a single feasible region [1, 10] and 5 sampling points
		/*
		 * np.linspace(1, 10, 5) Out[2]: array([ 1. , 3.25, 5.5 , 7.75, 10. ])
		 */
		SortedSet<Interval<Double>> feasibleRegions = new TreeSet<Interval<Double>>();
		feasibleRegions.add(new Interval<Double>(1.0, 4.0));
		feasibleRegions.add(new Interval<Double>(6.0, 8.0));
		feasibleRegions.add(new Interval<Double>(10.0, 40.0));

		SamplingAbstraction sa = new SamplingAbstraction(feasibleRegions, null);
		// effectively adding one sample point
		sa.perform(3);

		List<Double> inputs = sa.getInputPoints();
		// 4.0 coincides with one of the interval boundaries, otherwise 14
		Assert.assertEquals(7, inputs.size());

		Double[] expecteds = new Double[] { 1.0, 4.0, 6.0, 8.0, 10., 22.5, 40. };
		Double[] actuals = new Double[7];
		actuals = inputs.toArray(actuals);

		Assert.assertArrayEquals(expecteds, actuals);
	}

	@Test
	public void testSamplingWithDummySolver() {
		SortedSet<Interval<Double>> feasibleRegions = new TreeSet<Interval<Double>>();
		feasibleRegions.add(new Interval<Double>(1.0, 4.0));
		feasibleRegions.add(new Interval<Double>(6.0, 8.0));
		feasibleRegions.add(new Interval<Double>(10.0, 40.0));

		SamplingAbstraction sa = new SamplingAbstraction(feasibleRegions, null);
		sa.setSolver(new StubSolver());
		sa.getMinimizationDecisionExpressions().add(AbstractionParameterLiterals.DEXP_COSTS);
		sa.perform(10);

	}
}
