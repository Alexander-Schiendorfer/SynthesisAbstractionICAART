package de.uniaugsburg.isse.abstraction;

import java.util.ArrayList;
import java.util.Collection;

import de.uniaugsburg.isse.powerplants.PowerPlantData;

/**
 * Composite pattern to get AvppGraph
 * 
 * @author alexander
 * 
 */
public class AvppGraph {
	private Collection<AvppGraph> children;
	private GeneralAbstraction generalAbstraction;
	private TemporalAbstraction temporalAbstraction;
	private SamplingAbstraction samplingAbstraction;
	protected PowerPlantData powerPlant;
	protected String cplexModel; // created by CplexExporter
	private int height;

	public AvppGraph() {
		setChildren(new ArrayList<AvppGraph>());
	}

	public Collection<AvppGraph> getChildren() {
		return children;
	}

	public void setChildren(Collection<AvppGraph> children) {
		this.children = children;
	}

	public GeneralAbstraction getGeneralAbstraction() {
		return generalAbstraction;
	}

	public void setGeneralAbstraction(GeneralAbstraction generalAbstraction) {
		this.generalAbstraction = generalAbstraction;
	}

	public TemporalAbstraction getTemporalAbstraction() {
		return temporalAbstraction;
	}

	public void setTemporalAbstraction(TemporalAbstraction temporalAbstraction) {
		this.temporalAbstraction = temporalAbstraction;
	}

	public SamplingAbstraction getSamplingAbstraction() {
		return samplingAbstraction;
	}

	public void setSamplingAbstraction(SamplingAbstraction samplingAbstraction) {
		this.samplingAbstraction = samplingAbstraction;
	}

	public PowerPlantData getPowerPlant() {
		return powerPlant;
	}

	public void setPowerPlant(PowerPlantData powerPlant) {
		this.powerPlant = powerPlant;
	}

	public String getCplexModel() {
		return cplexModel;
	}

	public void setCplexModel(String cplexModel) {
		this.cplexModel = cplexModel;
	}

	public Collection<PowerPlantData> getChildrenPlantData() {
		Collection<PowerPlantData> childData = new ArrayList<PowerPlantData>(
				children.size());
		for (AvppGraph child : getChildren()) {
			childData.add(child.getPowerPlant());
		}
		return childData;
	}

	public void setHeight(int maxHeight) {
		this.height = maxHeight;
	}

	public int getHeight() {
		return height;
	}
}
