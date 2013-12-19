package de.uniaugsburg.isse.csp.model;

import java.util.HashMap;
import java.util.StringTokenizer;

/**
 * Allows for querying the parameters of a concrete power plant CSP model
 * 
 * @author Alexander Schiendorfer
 * 
 */
public class CspParameterModel {
	CspInputModel inputModel;
	HashMap<String, String> parameters;
	private final static String MIN_POSTF = "_min";
	private final static String MAX_POSTF = "_max";

	public CspParameterModel(CspInputModel in) {
		inputModel = in;
		parameters = new HashMap<String, String>();
		for (String interfaceParam : in.getInterfaceParameters()) {
			StringTokenizer tok = new StringTokenizer(interfaceParam, " ");
			tok.nextToken(); // variable type, maybe make use of this!
			String varName = tok.nextToken().trim();
			tok.nextToken(); // = sign
			String value = tok.nextToken().replaceAll(";", "").trim();
			parameters.put(varName, value);
		}
	}

	public String getParameter(String parameter) {
		return parameters.get(parameter);
	}

	public String getParameter(String parameter, boolean min) {
		return getParameter(parameter + (min ? MIN_POSTF : MAX_POSTF));
	}

}
