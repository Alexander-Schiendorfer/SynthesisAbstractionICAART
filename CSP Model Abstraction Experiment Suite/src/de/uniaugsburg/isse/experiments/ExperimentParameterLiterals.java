package de.uniaugsburg.isse.experiments;

import java.util.HashMap;
import java.util.Map;

/**
 * This class contains enums and string literals for running an experiment
 * 
 * @author alexander
 * 
 */
public class ExperimentParameterLiterals {
	public static enum PowerplantType {
		BIO, GAS
	}

	public static enum NumberPlants {
		KW_6(0), KW_50(1), KW_100(2), KW_500(3), KW_1000(4), KW_2000(5);
		private int id;

		private final static Map<Integer, NumberPlants> reversed;
		static {
			reversed = new HashMap<Integer, NumberPlants>();
			for (NumberPlants np : values()) {
				reversed.put(np.id, np);
			}
		}

		public static NumberPlants lookup(int id) {
			return reversed.get(id);
		}

		NumberPlants(int id) {
			this.id = id;
		}
	}

	public static final String filePrefix = "schwaben_2012_05_";
	public static final String fileSuffixBio = "_biomasse";
	public static final String fileSuffixGas = "_gas";
	public static final String fileType = ".csv";

	public static String getPlants(NumberPlants np) {
		switch (np) {
		case KW_6:
			return "06kw";
		case KW_50:
			return "50kw";
		case KW_100:
			return "100kw";
		case KW_500:
			return "500kw";
		case KW_1000:
			return "1000kw";
		case KW_2000:
			return "2000kw";

		default:
			return "";
		}
	}

	public static String getFileName(NumberPlants np, PowerplantType pt) {
		return filePrefix + getPlants(np)
				+ (pt == PowerplantType.BIO ? fileSuffixBio : fileSuffixGas)
				+ fileType;
	}

	public static final String residualLoadFile = "data/SchwabenNetzlast_ScenariosMay2012_2011_10.csv";
}
