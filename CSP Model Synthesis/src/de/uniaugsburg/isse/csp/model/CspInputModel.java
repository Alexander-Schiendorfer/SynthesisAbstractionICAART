package de.uniaugsburg.isse.csp.model;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * A CSP model used as the input to a combiner. Contains fields for the
 * different elements of the model.
 * 
 * @author Jan-Philipp Stegh�fer
 */
public class CspInputModel {

	private List<String> predefinedConstants = new ArrayList<String>();
	private List<String> modelSpecificConstants = new ArrayList<String>();
	/*
	 * interface parameters are a subset of model specific constants that can be
	 * queried
	 */
	private List<String> interfaceParameters = new ArrayList<String>();
	private List<String> decisionVariables = new ArrayList<String>();
	private List<String> penalties = new ArrayList<String>();
	private List<String> constraints = new ArrayList<String>();
	private List<String> relationships = new ArrayList<String>();
	private double pMin, pMax;
	private final String fileName;

	/**
	 * Creates a new CspInputModel with the given file name.
	 * 
	 * Note: Does NOT interpret the file or populate the fields!
	 * 
	 * @param fileName
	 *            the name of the file the input model is read from
	 */
	public CspInputModel(String fileName) {
		super();
		this.fileName = fileName;
	}

	public List<String> getPredefinedConstants() {
		return predefinedConstants;
	}

	public List<String> getModelSpecificConstants() {
		return modelSpecificConstants;
	}

	public List<String> getDecisionVariables() {
		return decisionVariables;
	}

	public List<String> getPenalties() {
		return penalties;
	}

	public List<String> getConstraints() {
		return constraints;
	}

	public void setPredefinedConstants(List<String> predefinedConstants) {
		this.predefinedConstants = predefinedConstants;
	}

	public void setModelSpecificConstants(List<String> modelSpecificConstants) {
		this.modelSpecificConstants = modelSpecificConstants;
	}

	public void setDecisionVariables(List<String> decisionVariables) {
		this.decisionVariables = decisionVariables;
	}

	public void setPenalties(List<String> penalties) {
		this.penalties = penalties;
	}

	public void setConstraints(List<String> constraints) {
		this.constraints = constraints;
	}

	public String getFileName() {
		return fileName;
	}

	public String getModelName() {
		return getFileName().substring(0, getFileName().indexOf("."));
	}

	public List<String> getInterfaceParameters() {
		return interfaceParameters;
	}

	public void setInterfaceParameters(List<String> interfaceParameters) {
		this.interfaceParameters = interfaceParameters;
	}

	public List<String> getRelationships() {
		return relationships;
	}

	public void setRelationships(List<String> relationships) {
		this.relationships = relationships;
	}

	public double getInterfaceValue(String constant) {
		for (String constantStr : getInterfaceParameters()) {
			if (constantStr.contains(constant)) {
				StringTokenizer tok = new StringTokenizer(constantStr, "=");
				tok.nextToken(); // type and name
				String val = tok.nextToken().replaceAll(";", "").trim();
				return Double.parseDouble(val);
			}
		}
		throw new RuntimeException("Interface constant " + constant
				+ " not found!!");
	}

	public void setP(double min, double max) {
		setpMin(min);
		setpMax(max);
	}

	public double getpMax() {
		return pMax;
	}

	public void setpMax(double pMax) {
		this.pMax = pMax;
	}

	public double getpMin() {
		return pMin;
	}

	public void setpMin(double pMin) {
		this.pMin = pMin;
	}

}
