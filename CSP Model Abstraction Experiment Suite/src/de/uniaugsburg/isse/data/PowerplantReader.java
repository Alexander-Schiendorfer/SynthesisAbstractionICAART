package de.uniaugsburg.isse.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import de.uniaugsburg.isse.abstraction.types.Interval;
import de.uniaugsburg.isse.constraints.BoundsConstraint;
import de.uniaugsburg.isse.constraints.GraduallyOffConstraint;
import de.uniaugsburg.isse.constraints.StartWithMinConstraint;
import de.uniaugsburg.isse.powerplants.PowerPlantData;

public class PowerplantReader {
	private static final double minCoeff = 0.2;

	public Collection<PowerPlantData> readPlants(String fileName) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(fileName));
			String nextLine;
			List<PowerPlantData> plants = new LinkedList<PowerPlantData>();
			int i = 0;
			while ((nextLine = br.readLine()) != null) {
				double pMax = Double.parseDouble(nextLine);
				double pMin = minCoeff * pMax;

				PowerPlantData pd = new PowerPlantData(fileName + "_" + i);
				pd.setPowerBoundaries(new Interval<Double>(pMin, pMax));
				pd.addConstraint(new BoundsConstraint(pd));
				pd.addConstraint(new GraduallyOffConstraint(pd));
				pd.addConstraint(new StartWithMinConstraint(pd));
				plants.add(pd);
				++i;
			}
			return plants;
		} catch (Exception e) {
			e.printStackTrace();
			return new ArrayList<PowerPlantData>(0);
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
