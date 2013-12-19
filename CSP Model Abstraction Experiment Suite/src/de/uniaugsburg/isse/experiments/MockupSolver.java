package de.uniaugsburg.isse.experiments;

import de.uniaugsburg.isse.solver.SolverFacade;

public class MockupSolver implements SolverFacade {

	@Override
	public void solve(String modelFile, String dataFile) {
		System.out.println("Asked to solve " + modelFile + " with " + dataFile);
	}

	@Override
	public double getProduction(String key, int step) {
		System.out.println("Asking for key " + key);
		return 3.5;
	}

	@Override
	public boolean getRunning(String key, int step) {
		System.out.println("Asking for key " + key);
		return true;
	}

	@Override
	public double getObjective() {
		return 530.0;
	}

	@Override
	public double getDecExpr(String decExpr, int i) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getDecVar(String decVar, String p, int i) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getTotalProduction(int i) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setSettingsFile(String settingsFile) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTimeLimit(int seconds) {
		// TODO Auto-generated method stub
		
	}

	public void cleanup() {
	}

	public boolean isSolved() {
		return false;
	}

	public void setSimplified(boolean isSimplified) {
	}

	@Override
	public void setPresolve(boolean b) {
		// TODO Auto-generated method stub
		
	}

	public void setUseInitialSolution(boolean useInitialSolution) {
	}

	public boolean isUseInitialSolution() {
		return false;
	}

}
