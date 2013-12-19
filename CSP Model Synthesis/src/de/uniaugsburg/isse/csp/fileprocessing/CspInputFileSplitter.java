package de.uniaugsburg.isse.csp.fileprocessing;

import java.io.FileInputStream;
import java.util.ListIterator;
import java.util.Scanner;

import de.uniaugsburg.isse.csp.model.CspInputModel;

/**
 * Splits a CSP read from an OPL file into its constituent parts.
 * 
 * @author Jan-Philipp Stegh�fer
 * 
 */
public class CspInputFileSplitter {

	private static enum ScanMode {
		UNDECIDED, PREDEFINEDCONSTANTS, MODELSPECIFICCONSTANTS, INTERFACE_PARAMETERS, DECISIONVARIABLES, PENALTIES, CONSTRAINTS, RELATIONSHIPS;
	}

	private static final String predefinedConstantsDelimiter = "/* Predefined constants */";
	private static final String modelSpecificConstantsDelimiter = "/* Model-specific constants */";
	private static final String interfaceParametersDelimiter = "/* Interface parameters */";
	private static final String decisionVariablesDelimiter = "/* Decision variables */";
	private static final String endModeDelimiter = "/* End ";
	private static final String penaltiesDelimiter = "/* Penalties */";
	private static final String relationshipsDelimiter = "RELATIONSHIPS";

	private static final String constraintsDelimiter = "subject to {";

	private final CspInputModel model;
	private final FileInputStream fis;

	/**
	 * Creates a new CspFileSplitter with the given file name and input stream.
	 * 
	 * @param fileName
	 * @param fis
	 */
	public CspInputFileSplitter(String fileName, FileInputStream fis) {
		super();
		model = new CspInputModel(fileName);
		this.fis = fis;
	}

	/**
	 * Splits the file into sections.
	 */
	public void split() {
		ScanMode mode = ScanMode.UNDECIDED;
		Scanner sc = new Scanner(fis);
		while (sc.hasNextLine()) {
			String newLine = sc.nextLine();
			if (newLine.contains(predefinedConstantsDelimiter)) {
				mode = ScanMode.PREDEFINEDCONSTANTS;
				continue; // Swallow delimiter
			} else if (newLine.contains(modelSpecificConstantsDelimiter)) {
				mode = ScanMode.MODELSPECIFICCONSTANTS;
				continue; // Swallow delimiter
			} else if (newLine.contains(decisionVariablesDelimiter)) {
				mode = ScanMode.DECISIONVARIABLES;
				continue; // Swallow delimiter
			} else if (newLine.contains(penaltiesDelimiter)) {
				mode = ScanMode.PENALTIES;
				continue; // Swallow delimiter
			} else if (newLine.contains(constraintsDelimiter)) {
				mode = ScanMode.CONSTRAINTS;
				continue; // Swallow delimiter
			} else if (newLine.contains(interfaceParametersDelimiter)) {
				mode = ScanMode.INTERFACE_PARAMETERS;
				continue; // Swallow delimiter
			} else if (newLine.contains(relationshipsDelimiter)) {
				mode = ScanMode.RELATIONSHIPS;
				continue;
			} else if (newLine.contains(endModeDelimiter)) {
				mode = ScanMode.UNDECIDED;
			}

			if (mode.equals(ScanMode.PREDEFINEDCONSTANTS)) {
				model.getPredefinedConstants().add(newLine);
			} else if (mode.equals(ScanMode.MODELSPECIFICCONSTANTS)) {
				model.getModelSpecificConstants().add(newLine);
			} else if (mode.equals(ScanMode.DECISIONVARIABLES)) {
				model.getDecisionVariables().add(newLine);
			} else if (mode.equals(ScanMode.PENALTIES)) {
				model.getPenalties().add(newLine);
			} else if (mode.equals(ScanMode.CONSTRAINTS)) {
				model.getConstraints().add(newLine);
			} else if (mode.equals(ScanMode.RELATIONSHIPS)) {
				model.getRelationships().add(newLine);
			} else if (mode.equals(ScanMode.INTERFACE_PARAMETERS)) {
				model.getInterfaceParameters().add(newLine);
				model.getModelSpecificConstants().add(newLine); // also add as
																// constraint
			} else {
				// Do nothing
				// System.out.println("Unknown mode for " + newLine);
			}
		}
		sc.close();

		// Remove closing parentheses from constraints
		int lastLine = -1;
		ListIterator<String> iterator = model.getConstraints().listIterator(
				model.getConstraints().size());
		while (iterator.hasPrevious()) {
			final String listElement = iterator.previous();
			if (listElement.contains("}")) {
				lastLine = iterator.nextIndex();
				break;
			}
		}
		for (int i = lastLine; i < model.getConstraints().size(); i++)
			model.getConstraints().remove(i);
	}

	public CspInputModel getModel() {
		return model;
	}

}
