package de.uniaugsburg.isse.csp.modelprocessing;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ua.cr2csop.graph.DirectedConstraintGraph;
import ua.cr2csop.util.OPLUtil;
import de.uniaugsburg.isse.csp.model.CspInputModel;
import de.uniaugsburg.isse.csp.util.StringLists;

/**
 * Contains methods that allow the substitution of identifiers used in a
 * {@link CspInputModel}.
 * 
 * @author Jan-Philipp Stegh�fer
 * 
 */
public class CspInputModelSubstitutor {

	/**
	 * Retrieves the names of any constants defined in the model.
	 * 
	 * @param model
	 *            the CSP model to operate on
	 * @return a list of constant names
	 */
	private List<String> getConstantNames(CspInputModel model) {
		Pattern constantPattern = Pattern
				.compile("\\s*[a-zA-Z+_]*\\s*([a-zA-Z_]+)\\s+(.*)");
		List<String> constantNames = new ArrayList<String>();

		for (String line : model.getModelSpecificConstants()) {
			Matcher constantMatcher = constantPattern.matcher(line);
			if (constantMatcher.matches()) {
				constantNames.add(constantMatcher.group(1));
			}
		}
		return constantNames;
	}

	/**
	 * Replaces all model specific constants in the model with non-ambiguous new
	 * constant names.
	 * 
	 * @param model
	 *            the CSP model to operate on
	 */
	public void replaceConstantNames(CspInputModel model) {
		List<String> constantNames = this.getConstantNames(model);
		for (String constantName : constantNames) {
			String newConstantName = constantName
					+ "_"
					+ model.getFileName().substring(0,
							model.getFileName().indexOf("."));
			String allConstraints = StringLists
					.toString(model.getConstraints());
			allConstraints = allConstraints.replaceAll("\\b" + constantName
					+ "\\b", newConstantName);
			model.setConstraints(StringLists.toList(allConstraints));

			String modelSpecificConstants = StringLists.toString(model
					.getModelSpecificConstants());
			modelSpecificConstants = modelSpecificConstants.replaceAll("\\b"
					+ constantName + "\\b", newConstantName);
			model.setModelSpecificConstants(StringLists
					.toList(modelSpecificConstants));

			String decisionVariables = StringLists.toString(model
					.getDecisionVariables());
			decisionVariables = decisionVariables.replaceAll("\\b"
					+ constantName + "\\b", newConstantName);
			model.setDecisionVariables(StringLists.toList(decisionVariables));
		}

	}

	/**
	 * Retrieves the names of any constraints defined in the model.
	 * 
	 * @param model
	 *            the CSP model to operate on
	 * @return a list of constraint names
	 */
	private List<String> getConstraintNames(CspInputModel model) {
		Pattern constraintNamePattern = Pattern.compile("\\s*(\\w*)\\s*:.*");
		List<String> constraintNames = new ArrayList<String>();

		for (String line : model.getConstraints()) {
			Matcher constraintNameMatcher = constraintNamePattern.matcher(line);
			if (constraintNameMatcher.find()) {
				constraintNames.add(constraintNameMatcher.group(1));
			}
		}
		return constraintNames;
	}

	/**
	 * Replaces the names of constraints in the CSP model with non-ambiguous
	 * ones.
	 * 
	 * @param model
	 *            the model to operate on
	 */
	public void replaceConstraintNames(CspInputModel model) {
		List<String> constraintNames = this.getConstraintNames(model);
		String constraints = StringLists.toString(model.getConstraints());
		for (String constraintName : constraintNames) {

			constraints = constraints.replaceAll(
					"\\b" + constraintName + "\\b",
					constraintName
							+ "_"
							+ model.getFileName().substring(0,
									model.getFileName().indexOf(".")));
		}
		model.setConstraints(StringLists.toList(constraints));
	}

	/**
	 * Replaces the names of model-specific decision variables with
	 * non-ambiguous ones.
	 * <p>
	 * Note: At least, that it what it should do at some point. For now, it
	 * merely adds an array index to <code>production</code> and
	 * <code>off</code> in the constraints.
	 * </p>
	 * 
	 * @param model
	 *            the model to operate on
	 */
	public void replaceDecisionVariables(CspInputModel model,
			List<String> orgDecisionVariables) {

		String constraints = StringLists.toString(model.getConstraints());
		String modelName = model.getFileName().substring(0,
				model.getFileName().indexOf("."));

		for (String decVar : orgDecisionVariables) {
			constraints = constraints.replaceAll("\\b" + decVar + "\\[\\b",
					decVar + "[\"" + modelName + "\"][");
		}

		model.setConstraints(StringLists.toList(constraints));
	}

	private String renameConstraint(String constraint, CspInputModel model) {
		return constraint
				+ "_"
				+ model.getFileName().substring(0,
						model.getFileName().indexOf("."));
	}

	public String replaceConstraintRelationships(CspInputModel model) {
		StringBuilder sb = new StringBuilder();
		for (String relationshipLine : model.getRelationships()) {

			StringTokenizer tok = new StringTokenizer(relationshipLine, ">>");
			String firstConstraint = renameConstraint(tok.nextToken().trim(),
					model);
			String secondConstraint = renameConstraint(tok.nextToken().trim(),
					model);

			sb.append(firstConstraint + " >> " + secondConstraint + "\n");
		}
		return sb.toString();
	}

	public void replaceConstraintsPenalties(CspInputModel model,
			DirectedConstraintGraph dcg) {
		List<String> newConstraints = OPLUtil.replaceConstraintsPenalties(
				model.getConstraints(), dcg);

		model.setConstraints(newConstraints);
	}
}
