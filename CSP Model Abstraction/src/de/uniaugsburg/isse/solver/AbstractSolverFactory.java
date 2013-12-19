package de.uniaugsburg.isse.solver;

import de.uniaugsburg.isse.solver.AbstractModel;
import de.uniaugsburg.isse.solver.AbstractSolver;

public interface AbstractSolverFactory {
	AbstractSolver createSolver();

	AbstractModel createModel();
}
