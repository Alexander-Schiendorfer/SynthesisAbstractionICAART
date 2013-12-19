package de.uniaugsburg.isse;

import java.io.FileInputStream;
import java.util.Properties;

import de.uniaugsburg.isse.cplex.CPLEXSolverFacade;
import de.uniaugsburg.isse.experiments.Experiment;
import de.uniaugsburg.isse.experiments.ExperimentParameterLiterals;
import de.uniaugsburg.isse.experiments.ExperimentSeries;
import de.uniaugsburg.isse.solver.CplexSolverFactory;

/**
 * This class runs experiments using the csp model abstraction experiment suite
 * and offers a CPLEX solver - this is done to avoid having the experiment suite
 * depend on CPLEX binaries
 * 
 * @author alexander
 * 
 */
public class ExperimentRunner {

	public Experiment getExperiment(String fileName) {
		Experiment exp = new Experiment();
		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream(fileName));
			exp.setTimeHorizon(readProperty(prop, "timeHorizon"));
			exp.setExperimentHorizon(readProperty(prop, "experimentHorizon"));
			exp.setNumberOfPlants(ExperimentParameterLiterals.NumberPlants
					.lookup(readProperty(prop, "numberOfPlants")));
			exp.setPlantsPerAvpp(readProperty(prop, "plantsPerAvpp"));
			exp.setSamplingPoints(readProperty(prop, "samplingPoints"));
			exp.setIsoSplit(readProperty(prop, "isoSplit") != 0);
			exp.setAvppsRandomSeed(readProperty(prop, "avppsRandomSeed"));
			exp.setHierarchyRandomSeed(readProperty(prop, "hierarchyRandomSeed"));
			exp.setInitialStatesSeed(readProperty(prop, "initialStatesSeed"));
			exp.setAvppsPerAvpp(readProperty(prop, "avppsPerAvpp"));
			exp.setOriginatingProperties(prop);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return exp;
	}

	private int readProperty(Properties prop, String key) {
		return Integer.parseInt(prop.getProperty(key));
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
			
		int nRuns = 5;
		String propertiesFile = "experiments/experiment1.properties";
		ExperimentRunner runner = new ExperimentRunner();
		Experiment exp = runner.getExperiment(propertiesFile);
		exp.setSolverFacade(new CPLEXSolverFacade());
		exp.setSolverFactory(new CplexSolverFactory());
		exp.setUseSamplingAbstraction(true);
		
		// make a series of experiments out of it
		ExperimentSeries series = new ExperimentSeries();
		
		// convert seeds to an array
		long[] avppsRandomSeeds = new long[nRuns];
		avppsRandomSeeds[0] = exp.getAvppsRandomSeed();
		
		long[] hierarchyRandomSeeds = new long[nRuns];
		hierarchyRandomSeeds[0] = exp.getHierarchyRandomSeed();
		
		long[] initialStatesSeeds = new long[nRuns];
		initialStatesSeeds[0] = exp.getInitialStatesSeed();
		
		for(int i = 1; i < nRuns; ++i ) {
			avppsRandomSeeds[i] = (avppsRandomSeeds[i-1] * 5673) % 6553;
			hierarchyRandomSeeds[i] = (hierarchyRandomSeeds[i-1] * 5673) % 6553;
			initialStatesSeeds[i] = (initialStatesSeeds[i-1] * 5673) % 6553;
		}
		series.setAvppsRandomSeeds(avppsRandomSeeds);
		series.setExperiment(exp);
		series.setHierarchyRandomSeed(hierarchyRandomSeeds);
		series.setInitialStatesSeeds(initialStatesSeeds);
		series.run();
	}

}
