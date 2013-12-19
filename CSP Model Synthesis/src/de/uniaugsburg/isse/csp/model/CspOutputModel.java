package de.uniaugsburg.isse.csp.model;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * 
 * @author Jan-Philipp Stegh�fer
 */
public class CspOutputModel {

	private final String filename;

	private final List<String> constants = new ArrayList<String>();
	// required by agent models
	private List<String> mandatoryConstants = new ArrayList<String>();
	private final List<String> decisionVariables = new ArrayList<String>();
	private final List<String> decisionExpressions = new ArrayList<String>();
	private final List<String> types = new ArrayList<String>();
	private List<String> constraints = new ArrayList<String>();
	private String feasibleRegionString;
	private String ident;
	private List<String> modelList = new ArrayList<String>();

	private List<String> softConstraintsList = new ArrayList<String>();

	public CspOutputModel(String filename) {
		super();
		this.filename = filename;
	}

	public String getFilename() {
		return filename;
	}

	public List<String> getConstants() {
		return constants;
	}

	public List<String> getDecisionVariables() {
		return decisionVariables;
	}

	public List<String> getConstraints() {
		return constraints;
	}

	public List<String> getModelList() {
		return modelList;
	}

	public void setModelList(List<String> newList) {
		this.modelList = newList;
	}

	public String getIdent() {
		return ident;
	}

	public void setIdent(String outputIdent) {
		this.ident = outputIdent.replaceAll("/", "").trim();
	}

	public List<String> getSoftConstraintsList() {
		return softConstraintsList;
	}

	public void setSoftConstraintsList(List<String> softConstraintsList) {
		this.softConstraintsList = softConstraintsList;
	}

	public void setConstraints(List<String> constraints) {
		this.constraints = constraints;
	}

	public List<String> getDecisionExpressions() {
		return decisionExpressions;
	}

	public List<String> getDecisionVariableNames() {
		List<String> decVarNames = new ArrayList<String>(
				decisionVariables.size());
		List<String> decExps = new ArrayList<String>(decisionExpressions.size()
				+ decisionVariables.size());
		decExps.addAll(decisionVariables);
		for (String decExpr : decisionExpressions) {
			if (decExpr.contains("REPLACE"))
				decExps.add(decExpr);
		}
		for (String decVarLine : decExps) {

			decVarLine = decVarLine.replaceAll("dvar", "")
					.replaceAll("dexpr", "").trim();
			// now take snd string as decvar
			StringTokenizer tok = new StringTokenizer(decVarLine, " ");

			if (tok.hasMoreTokens()) {
				tok.nextToken(); // type
				String varName = tok.nextToken();
				if (varName.contains("[")) {
					varName = varName.substring(0, varName.indexOf("["));
					decVarNames.add(varName);
				}
			}
		}

		return decVarNames;
	}

	public List<String> getMandatoryConstants() {
		return mandatoryConstants;
	}

	public List<String> getMandatoryConstantNames() {
		List<String> mandConstantNames = new ArrayList<String>(
				mandatoryConstants.size());
		for (String mandConstantLine : getMandatoryConstants()) {
			StringTokenizer tok = new StringTokenizer(mandConstantLine, " ");
			tok.nextToken(); // type
			String nameStr = tok.nextToken();
			nameStr = nameStr.substring(0, nameStr.indexOf("["));
			mandConstantNames.add(nameStr);
		}
		return mandConstantNames;
	}

	public void setMandatoryConstants(List<String> mandatoryConstants) {
		this.mandatoryConstants = mandatoryConstants;
	}

	public void setFeasibleRegionString(String string) {
		this.feasibleRegionString = string;

	}

	public String getFeasibleRegionString() {
		return feasibleRegionString;
	}

	public List<String> getTypes() {
		return types;
	}
}
