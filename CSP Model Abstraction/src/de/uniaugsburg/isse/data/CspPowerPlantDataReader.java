package de.uniaugsburg.isse.data;

import java.util.StringTokenizer;

import de.uniaugsburg.isse.abstraction.types.Interval;
import de.uniaugsburg.isse.csp.model.CspInputModel;
import de.uniaugsburg.isse.csp.model.CspParameterModel;
import de.uniaugsburg.isse.powerplants.PowerPlantData;

/**
 * Takes the CspInputModel used in synthesis
 * as input and returns a power plant data object
 * 
 * @author Alexander Schiendorfer
 *
 */
public class CspPowerPlantDataReader {

	public PowerPlantData read(CspInputModel in) {
		CspParameterModel paramModel = new CspParameterModel(in);
		
		PowerPlantData pd = new PowerPlantData(in.getModelName());
		Interval<Double> powerInterval = getInterval(paramModel, "P");
		pd.setPowerBoundaries(powerInterval);
		// state variables below are relevant for time specific abstractions
		
		// add all model specific constants to pd parameters
		for(String modelSpecificConstants : in.getModelSpecificConstants()) {
			StringTokenizer tok = new StringTokenizer(modelSpecificConstants.trim(), " ");
			tok.nextToken(); // type
			String key = tok.nextToken().replaceAll(";", "");
			tok.nextToken(); // = sign
			String value = tok.nextToken().replaceAll(";", "");;
			pd.put(key, value);
		}
		/* pd.setConsRunningTimeInit(0);
		pd.setConsStoppingTimeInit(1);
		pd.setMinOffTime(2);
		pd.setMinOnTime(2);
		pd.setRateOfChange(0.15);
		pd.setStartupSlope(1.0);
		pd.setPowerInit(0.0);
		*/
		return pd;
	}

	private Interval<Double> getInterval(CspParameterModel paramModel, String parameter) {
		double min = Double.parseDouble(paramModel.getParameter(parameter, true));
		double max = Double.parseDouble(paramModel.getParameter(parameter, false));
		return new Interval<Double>(min, max);
	}

}
