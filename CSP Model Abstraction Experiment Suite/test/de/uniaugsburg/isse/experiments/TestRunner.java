package de.uniaugsburg.isse.experiments;

import de.uniaugsburg.isse.experiments.ExperimentParameterLiterals.NumberPlants;

public class TestRunner {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Experiment exp = new Experiment();
		exp.setTimeHorizon(4);
		exp.setSolverFacade(new MockupSolver());
		exp.setExperimentHorizon(10);
		exp.setNumberOfPlants(NumberPlants.KW_6);
		exp.setPlantsPerAvpp(3);
		exp.run();
	}

}
