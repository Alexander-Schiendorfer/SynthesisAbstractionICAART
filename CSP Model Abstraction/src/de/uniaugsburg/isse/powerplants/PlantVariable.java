package de.uniaugsburg.isse.powerplants;

/**
 * Identifies the type of constraint of the decision variables (=
 * power plant state variables)
 * 
 * e.g. rateOfChange affects only the Power
 * 		minOffTime affects only the On
 * @author Alexander Schiendorfer
 *
 */

public enum PlantVariable {
	POWER,
	ON
}
