package de.uniaugsburg.isse.solver;

import java.io.File;

import de.uniaugsburg.isse.cplex.CPLEXSolverFacade;
import de.uniaugsburg.isse.util.Utils;

public class CplexSolver extends AbstractSolver {

	private String modelFile;
	private String dataFile;
	private final CPLEXSolverFacade facade;
	private final double jitter = 0.001; // matches CPLEX file

	private static boolean DELETE_MOD_AND_DAT_FILES = true;

	public CplexSolver() {
		this.facade = new CPLEXSolverFacade();
		this.facade.setSimplified(true);
	}

	@Override
	public void solve() {
		CplexModel model = (CplexModel) this.getModel();
		String modelString = model.getCplexString();
		// System.out.println(modelString);
		String dataString = model.getCplexDataString();

		File tmpModelFile = Utils.createTempFile("samplingAbstraction", ".mod");
		File tmpDataFile = null;
		if (dataString != null) {
			tmpDataFile = Utils.createTempFile("samplingAbstractionData", ".dat");
			this.dataFile = tmpDataFile.getAbsolutePath();
			Utils.writeFile(this.dataFile, dataString);
		}

		this.modelFile = tmpModelFile.getAbsolutePath();
		Utils.writeFile(this.modelFile, modelString);
		this.facade.solve(this.modelFile, this.dataFile);
		this.solved = this.facade.isSolved();

		// delete temporary files
		if (CplexSolver.DELETE_MOD_AND_DAT_FILES) {
			this.deleteTempFile(tmpModelFile.getAbsolutePath());
			this.deleteTempFile(tmpDataFile.getAbsolutePath());
		}
	}

	@Override
	public double getResult(String decExpr) {
		if (this.isSolved()) {
			double res = this.facade.getDecExpr(decExpr);
			if (Math.abs(res) < this.jitter)
				return 0.0;
			else
				return res;
		}
		return 0.0;
	}

	@Override
	public double getObjective() {
		return this.facade.getObjective();
	}

	@Override
	public void cleanup() {
		this.facade.cleanup();
	}

	/**
	 * Deletes the temporary file of the given name.
	 * 
	 * @param dataFileName
	 */
	private void deleteTempFile(String dataFileName) {
		File dataFile = new File(dataFileName);
		if (!dataFile.delete()) {
			System.err.println("Could not delete temporary file " + dataFileName);
		}
	}
}
