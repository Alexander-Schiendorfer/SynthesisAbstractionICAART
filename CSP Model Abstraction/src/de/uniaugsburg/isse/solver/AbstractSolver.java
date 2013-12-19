package de.uniaugsburg.isse.solver;

/**
 * This class represents a CSOP solver for sampling - in order to program the sampling abstraction abstractly and avoid
 * introducing dependencies to concrete libraries in the abstraction project (e.g. CPLEX libs)
 * 
 * @author alexander
 * 
 */
public abstract class AbstractSolver {

	protected AbstractModel model;
	protected boolean solved;
	protected String inputExpr;

	/**
	 * Performs the actual solving process once the problem is configured
	 */
	public abstract void solve();

	/**
	 * Fixes the input of the domain of the observed function (e.g. P_Total = 100)
	 * 
	 * @param value
	 *            - the value for the decExpr
	 */
	public void setInput(double value, double tolerance) {
		if (model != null) {
			model.addEqualityConstraint("input", inputExpr, value, tolerance);
		}
	}

	/**
	 * The objective in the functional relationship, e.g. maximizing the change in the next step or minimizing the cost
	 * for a given input
	 * 
	 * @param decExpr
	 * @param maximize
	 */
	public void setObjective(String decExpr, boolean maximize) {
		if (model != null) {
			model.setObjective(decExpr, maximize);
		}
	}

	/**
	 * Retrieves the quantity of interest
	 * 
	 * @return
	 */
	public abstract double getResult(String decExpr);

	public AbstractModel getModel() {
		return model;
	}

	public void setModel(AbstractModel model) {
		this.model = model;
	}

	public boolean isSolved() {
		return solved;
	}

	public void setSolved(boolean solved) {
		this.solved = solved;
	}

	public abstract void cleanup();

	public void setInputExpr(String string) {
		this.inputExpr = string;
		if (model != null)
			model.setInputExpression(inputExpr);
	}

	public void setInputLowerBound(double lowerBound) {
		if (model != null)
			model.addInputLowerBoundConstraint(lowerBound);
	}

	public void setOutputLowerBound(String decExpr, double prevResult) {
		if (model != null)
			model.addOutputLowerBound(decExpr, prevResult);

	}

	public abstract double getObjective();
}
