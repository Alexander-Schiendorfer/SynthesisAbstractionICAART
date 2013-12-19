package de.uniaugsburg.isse.data;

import java.util.Collection;
import java.util.Iterator;
import java.util.SortedSet;

import de.uniaugsburg.isse.abstraction.GeneralAbstraction;
import de.uniaugsburg.isse.abstraction.TemporalAbstraction;
import de.uniaugsburg.isse.abstraction.types.Interval;
import de.uniaugsburg.isse.csp.model.CspOutputModel;

public class CspAbstractedModelExporter {
	private CspOutputModel outputModel;
	private GeneralAbstraction generalAbstraction;
	private TemporalAbstraction temporalAbstraction;

	public CspAbstractedModelExporter(CspOutputModel outputModel, GeneralAbstraction ga) {
		this.outputModel = outputModel;
		this.generalAbstraction = ga;
	}

	public CspAbstractedModelExporter(CspOutputModel outputModel, GeneralAbstraction ga, TemporalAbstraction ta) {
		this(outputModel, ga);
		this.temporalAbstraction = ta;
	}

	public String getModel() {
		StringBuilder oplBuilder = new StringBuilder("/* Creating abstracted OPL model */\n");
		// first get minimal and maximal power
		SortedSet<Interval<Double>> feasibleRegions = generalAbstraction.getFeasibleRegions();
		double P_min = feasibleRegions.first().min;
		double P_max = feasibleRegions.last().max;

		// add new interface parameters to use AVPP in hierarchy again
		oplBuilder.append("/* Interface parameters */\n");
		oplBuilder.append("float P_min = " + P_min + ";\nfloat P_max = " + P_max + ";\n");
		oplBuilder.append("/* END Interface parameters */\n");

		// add constants such as TIMERANGE TODO need better way
		oplBuilder.append("/* Constants */\n");
		oplBuilder.append("int LAST_SIMULATION_STEP = 120;\n");
		oplBuilder.append("range TIMERANGE = 1..LAST_SIMULATION_STEP;\n");
		oplBuilder.append("/* END Constants */\n");

		// add d_var for residual
		String ident = outputModel.getIdent();
		oplBuilder.append("dvar float production[TIMERANGE];\n");

		// make sure holes are not hit
		oplBuilder.append("subject to {\n");

		oplBuilder.append("// Physical general limitations\nforall (t in TIMERANGE) {\n");
		oplBuilder.append(ident + "_P_Min : production[t] >= P_min;\n");
		oplBuilder.append(ident + "_P_Max : production[t] <= P_max;\n");

		int i = 0;
		for (Interval<Double> hole : generalAbstraction.getHoles()) {
			oplBuilder.append(ident + "_hole_" + (++i) + " : !(production[t] > " + hole.min + " && production[t] < " + hole.max + ");\n");
		}
		oplBuilder.append("}\n"); // END forall t

		if (temporalAbstraction != null) {
			System.out.println("Starting TA");
			int t = 1;
			Iterator<SortedSet<Interval<Double>>> feasibleRegionsIterator = temporalAbstraction.getAllFeasibleRegions().iterator();
			for (Collection<Interval<Double>> holes : temporalAbstraction.getAllHoles()) {
				// add feasibility constraints for explicit timesteps
				i = 0;
				boolean first = true;
				// min max boundaries at time t
				SortedSet<Interval<Double>> feasRegions = feasibleRegionsIterator.next();
				P_min = feasRegions.first().min;
				P_max = feasRegions.last().max;
				oplBuilder.append(ident + "_t_" + t + "_P_Min : production[" + t + "] >= " + P_min + ";\n");
				oplBuilder.append(ident + "_t_" + t + "_P_Max : production[" + t + "] <= " + P_max + ";\n");

				if (!holes.isEmpty()) {

					oplBuilder.append(ident + "_holes_t_" + t + " : (");
					for (Interval<Double> hole : holes) {
						if (!first) {
							oplBuilder.append(" && ");
						} else {
							first = false;
						}
						oplBuilder.append("!(production[" + t + "] > " + hole.min + " && production[" + t + "] < " + hole.max + ")");

					}
					oplBuilder.append(");\n");
				}
				++t;
			}
		}

		oplBuilder.append("\n}\n"); // END subject to
		return oplBuilder.toString();
	}

	public void print() {
		System.out.println(getModel());
	}
}
