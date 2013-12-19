package de.uniaugsburg.isse.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

public class ResidualLoadReader {

	public Double[] readLoad(String fileName) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(fileName));
			String nextLine;
			List<Double> loads = new LinkedList<Double>();
			while ((nextLine = br.readLine()) != null) {
				StringTokenizer tok = new StringTokenizer(nextLine, ";");
				tok.nextToken(); // date
				tok.nextToken(); // time
				String doubleVal = tok.nextToken();
				loads.add(Double.parseDouble(doubleVal));
			}
			Double[] loadsArray = new Double[loads.size()];
			loadsArray = loads.toArray(loadsArray);
			return loadsArray;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
