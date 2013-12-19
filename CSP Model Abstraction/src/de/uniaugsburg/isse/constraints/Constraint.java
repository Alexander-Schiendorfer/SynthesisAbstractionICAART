package de.uniaugsburg.isse.constraints;

/**
 * Interface for the constraints included in the Java models
 * used for temporal abstraction and CPLEX file generation
 * @author Alexander Schiendorfer
 *
 */
public interface Constraint {
	/**
	 * Return maximal production in step t+1 given step t
	 * @return
	 */
	double maximize();
	
	/**
	 * Return minimal production in step t+1 given step t
	 * @return
	 */
	double minimize();
	
	/**
	 * Return maximal on/off state in step t+1 given step t
	 * @return
	 */
	boolean maximizeBool();
	
	/**
	 * Return minimal on/off state in step t+1 given step t
	 * @return
	 */
	boolean minimizeBool();
	
	/**
	 * Denote whether this constraint is a soft constraint
	 * @return
	 */
	void setSoft(boolean soft);
	
	/**
	 * Return whether this constraint is a soft constraint
	 * @return
	 */
	boolean isSoft();
	
	/**
	 * Returns the assigned weight for this soft constraint 
	 * @return
	 */
	int getWeight();

	/**
	 * Make sure weight is also set for a soft constraint using constraint relationships
	 * @param weight
	 */
	void setWeight(int weight);
	

	/**
	 * Returns the unique identifier
	 * @return
	 */
	String getIdent();
}
