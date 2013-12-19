/**
 * 
 */
package de.uniaugsburg.isse.csp.fileprocessing;

import java.io.FileInputStream;
import java.util.ListIterator;
import java.util.Scanner;

import de.uniaugsburg.isse.csp.model.CspOutputModel;

/**
 * @author steghoja
 * 
 */
public class CspOutputFileSplitter {

	private static enum ScanMode {
		UNDECIDED, CONSTANTS, MANDATORYCONSTANTS, MODELLIST, DECISIONVARIABLES, CONSTRAINTS, IDENT, DECISIONEXPRESSIONS, TYPES;
	}

	private static final String constantsDelimiter = "/* Constants */";
	private static final String mandatoryConstantsDelimiter = "/* MandatoryConstants */";
	private static final String modelListDelimiter = "/* Model list */";
	private static final String decisionVariablesDelimiter = "/* Decision variables */";
	private static final String decisionExpressionsDelimiter = "Decision Expressions";
	private static final String typesDelimiter = "Types";
	private static final String constraintsDelimiter = "subject to {";
	private static final String identDelimiter = "/* AVPP ident */";
	private static final String endDelimiter = "END";

	private final CspOutputModel model;
	private final FileInputStream fis;

	/**
	 * Creates a new CspFileSplitter with the given file name and input stream.
	 * 
	 * @param fileName
	 * @param fis
	 */
	public CspOutputFileSplitter(String fileName, FileInputStream fis) {
		super();
		model = new CspOutputModel(fileName);
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
			if (newLine.contains(constantsDelimiter)) {
				mode = ScanMode.CONSTANTS;
				continue; // Swallow delimiter
			} else if (newLine.contains(mandatoryConstantsDelimiter)) {
				mode = ScanMode.MANDATORYCONSTANTS;
				continue; // Swallow delimiter
			} else if (newLine.contains(modelListDelimiter)) {
				mode = ScanMode.MODELLIST;
				continue;
			} else if (newLine.contains(typesDelimiter)) {
				mode = ScanMode.TYPES;
				continue;
			} else if (newLine.contains(decisionVariablesDelimiter)) {
				mode = ScanMode.DECISIONVARIABLES;
				continue; // Swallow delimiter
			} else if (newLine.contains(decisionExpressionsDelimiter)) {
				mode = ScanMode.DECISIONEXPRESSIONS;
			} else if (newLine.contains(constraintsDelimiter)) {
				mode = ScanMode.CONSTRAINTS;
				continue; // Swallow delimiter
			} else if (newLine.contains(identDelimiter)) {
				mode = ScanMode.IDENT;
				continue; // Swallow delimiter
			} else if (newLine.contains(endDelimiter)) {
				mode = ScanMode.UNDECIDED;
				continue; // Swallow delimiter
			}

			if (mode.equals(ScanMode.CONSTANTS)) {
				model.getConstants().add(newLine);
			} else if (mode.equals(ScanMode.MANDATORYCONSTANTS)) {
				model.getMandatoryConstants().add(newLine);
			} else if (mode.equals(ScanMode.MODELLIST)) {
				model.getModelList().add(newLine);
			} else if (mode.equals(ScanMode.TYPES)) {
				model.getTypes().add(newLine);
			} else if (mode.equals(ScanMode.DECISIONVARIABLES)) {
				model.getDecisionVariables().add(newLine);
			} else if (mode.equals(ScanMode.DECISIONEXPRESSIONS)) {
				model.getDecisionExpressions().add(newLine);
			} else if (mode.equals(ScanMode.CONSTRAINTS)) {
				model.getConstraints().add(newLine);
			} else if (mode.equals(ScanMode.IDENT)) {
				model.setIdent(newLine);
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

	public CspOutputModel getModel() {
		return this.model;
	}
}
