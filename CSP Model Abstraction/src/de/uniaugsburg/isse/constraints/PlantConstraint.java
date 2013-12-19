package de.uniaugsburg.isse.constraints;

import de.uniaugsburg.isse.powerplants.Plant;
public abstract class PlantConstraint implements Constraint {

	protected Plant plant;
	private boolean soft;
	private int weight;
	private int id;
	
	
	public Plant getPlant() {
		return plant;
	}

	public void setPlant(Plant plant) {
		this.plant = plant;
	}
	
	@Override
	public double maximize() {
		return Double.MAX_VALUE;
	}
	
	@Override
	public double minimize() {
		return -(Double.MAX_VALUE-1);
	}
	
	@Override
	public boolean maximizeBool() {
		return true;
	}
	
	@Override
	public boolean minimizeBool() {
		return false;
	}
		
	@Override
	public boolean isSoft() {
		return soft;
	}
	
	@Override
	public void setSoft(boolean soft) {
		this.soft = soft;	
	}
	
	@Override
	public void setWeight(int weight) {
		this.weight = weight;
	}
	
	@Override
	public int getWeight() {
		return this.weight;
	}
	
	@Override
	public String getIdent() {
		return plant.getName() + this.getClass().getSimpleName() + "_"+id;
	}

	public void setId(int newId) {
		this.id = newId;
	}
}

