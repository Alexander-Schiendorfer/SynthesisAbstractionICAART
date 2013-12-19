package de.uniaugsburg.isse.solver;

public class CplexSolverFactory implements AbstractSolverFactory {

	@Override
	public AbstractSolver createSolver() {
		return new CplexSolver();
	}

	@Override
	public AbstractModel createModel() {
		return new CplexModel();
	}

}
