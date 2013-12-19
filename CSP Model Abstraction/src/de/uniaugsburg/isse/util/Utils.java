package de.uniaugsburg.isse.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import de.uniaugsburg.isse.abstraction.types.Interval;

public class Utils {
	/**
	 * Creates a string of a padded set (i.e. it contains all interval sets in
	 * intervalSet, then adds {} such that length timeHorizon is reached
	 * 
	 * e.g. intervalSetSet = {{[1.0, 2.0]}, {[3.0, 4.0]}}, timeHorizon = 3 = "[
	 * {<1.0, 2.0>}, {<3.0,4.0>}, {}]
	 * 
	 * @param intervalSetSet
	 * @param timeHorizon
	 * @return
	 */
	public static String getPaddedSet(
			Collection<Collection<Interval<Double>>> intervalSetSet,
			int timeHorizon) {
		int counter = 0;
		Collection<String> allSetStrings = new ArrayList<String>(timeHorizon);

		for (Collection<Interval<Double>> intervalSet : intervalSetSet) {
			String s = Utils.CplexIntervalSet(intervalSet);
			allSetStrings.add(s);
			++counter;
		}

		// pad with empty sets
		while (counter < timeHorizon) {
			allSetStrings.add("{}");
			++counter;
		}
		return Utils.getSeparatedListOfStrings(allSetStrings, ", ");
	}

	/**
	 * Prints a collection of interval to CPLEX conform syntax (<1.0, 2.0>)
	 * 
	 * @param feasibleRegion
	 * @return
	 */
	public static String CplexIntervalSet(
			Collection<Interval<Double>> feasibleRegion) {
		StringBuilder sb = new StringBuilder("{");
		if (feasibleRegion != null) {
			for (Interval<Double> iv : feasibleRegion) {
				sb.append("<" + iv.min + ", " + iv.max + ">");
			}
		}
		sb.append("}");
		return sb.toString();
	}

	public static String getEmptySets(int timeHorizon) {
		Collection<String> eSets = new ArrayList<String>(timeHorizon);
		for (int i = 0; i < timeHorizon; i++) {
			eSets.add("{}");
		}
		return getSeparatedListOfStrings(eSets, ",");
	}

	public static void writeFile(String fileName, String content) {
		FileWriter writer = null;
		System.out.println("Writing " + fileName);
		try {
			writer = new FileWriter(fileName);
			writer.write(content);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (writer != null)
				try {
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}

	public static String getSeparatedListOfStrings(Collection<String> strings,
			String sep) {
		if (strings.isEmpty())
			return "";
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String s : strings) {
			if (first)
				first = false;
			else
				sb.append(sep);
			sb.append(s);
		}
		return sb.toString();
	}

	public static File createTempFile(String string, String string2) {
		try {
			File f = File.createTempFile(string, string2);
		
			return f;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static void deleteFile(String file) {
		File f = new File(file);
		if(f.exists())
			f.delete();
	}

}
