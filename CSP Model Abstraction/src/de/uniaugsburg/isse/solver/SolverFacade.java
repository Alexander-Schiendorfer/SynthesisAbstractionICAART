package de.uniaugsburg.isse.solver;

/**
 * Using an interface for solvers to avoid depending on CPLEX binaries in this
 * project
 * 
 * @author Alexander Schiendorfer
 * 
 */
public interface SolverFacade {

	void setSettingsFile(String settingsFile);
	
	void solve(String modelFile, String dataFile);

	double getProduction(String key, int i);

	boolean getRunning(String key, int i);

	double getObjective();

	double getDecExpr(String decExpr, int i);

	double getDecVar(String decVar, String p, int i);

	double getTotalProduction(int i);

	void setTimeLimit(int seconds);

	public abstract void cleanup();

	public abstract boolean isSolved();

	public abstract void setSimplified(boolean isSimplified);

	void setPresolve(boolean b);

	public abstract void setUseInitialSolution(boolean useInitialSolution);

	public abstract boolean isUseInitialSolution();
}
