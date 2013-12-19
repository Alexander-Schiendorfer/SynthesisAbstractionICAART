package de.uniaugsburg.isse.abstraction;

import java.util.ArrayList;
import java.util.Collection;

import de.uniaugsburg.isse.powerplants.PowerPlantData;

public class CplexAvppGraphExporter {
	private CplexExporter exporter;

	public CplexAvppGraphExporter() {
		exporter = new CplexExporter();
	}

	public CplexAvppGraphExporter(CplexExporter exporter) {
		this.exporter = exporter;
	}

	/**
	 * This method creates a cplex model for a single avpp taking into account
	 * intervals etc.
	 * 
	 * @param graph
	 */
	public void createRegionalModels(AvppGraph graph) {
		// postfix traversal - first get the kids right
		Collection<PowerPlantData> children = new ArrayList<PowerPlantData>(
				graph.getChildren().size());
		for (AvppGraph child : graph.getChildren()) {
			createRegionalModels(child);
			children.add(child.getPowerPlant());
		}
		graph.setCplexModel(exporter.createModel(children));
	}

	public String createSingleModel(AvppGraph graph) {
		Collection<PowerPlantData> allPlants = getPlants(graph);
		return exporter.createModel(allPlants);
	}

	private Collection<PowerPlantData> getPlants(AvppGraph graph) {
		Collection<PowerPlantData> allPlantStrings = new ArrayList<PowerPlantData>();
		return getPlantsRec(graph, allPlantStrings);
	}

	private Collection<PowerPlantData> getPlantsRec(AvppGraph graph,
			Collection<PowerPlantData> allPlantStrings) {
		if (graph != null) {
			for (AvppGraph child : graph.getChildren()) {
				if (child instanceof AvppLeafNode)
					allPlantStrings.add(((AvppGraph) child).getPowerPlant());
				else
					getPlantsRec(child, allPlantStrings);
			}
		}
		return allPlantStrings;
	}

	public String getGeneralAbstractionData(AvppGraph node) {
		return exporter.getGeneralAbstractionData(toPPData(node.getChildren()));
	}

	private Collection<PowerPlantData> toPPData(Collection<AvppGraph> children) {
		Collection<PowerPlantData> data = new ArrayList<PowerPlantData>(
				children.size());
		for (AvppGraph child : children) {
			data.add(child.getPowerPlant());
		}
		return data;
	}

	public String getTemporalAbstractionData(AvppGraph node) {
		return exporter
				.getTemporalAbstractionData(toPPData(node.getChildren()));

	}

	public CplexExporter getExporter() {
		return exporter;
	}

}
