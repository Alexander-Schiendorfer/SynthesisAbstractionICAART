package de.uniaugsburg.isse.constraints;

/**
 * Constraint that obliges every power plant to be on at all times
 * 
 * @author Alexander Schiendorfer
 * 
 */
public class ForceOnConstraint extends PlantConstraint {
	@Override
	public boolean maximizeBool() {
		return true;
	}

	@Override
	public boolean minimizeBool() {
		return true;
	}
}
