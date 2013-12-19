package de.uniaugsburg.isse.powerplants;

import java.util.Map;

import de.uniaugsburg.isse.abstraction.types.Interval;
import de.uniaugsburg.isse.constraints.Constraint;
import de.uniaugsburg.isse.constraints.PlantConstraint;

/**
 * Represents the current state of a plant for time sensitive abstraction
 * 
 * @author alexander
 * 
 */
public class PowerPlantState implements Plant {

	protected Interval<Double> power;
	protected PowerPlantData data;
	protected Interval<Boolean> running = new Interval<Boolean>(false);
	protected Interval<Integer> consRunning;
	protected Interval<Integer> consStopping;
	protected int simulationStep;

	public PowerPlantState() {

	}

	@Override
	public int getSimulationStep() {
		return simulationStep;
	}

	@Override
	public void setSimulationStep(int simulationStep) {
		this.simulationStep = simulationStep;
	}

	@Override
	public Interval<Double> getPower() {
		return power;
	}

	@Override
	public void setPower(Interval<Double> power) {
		this.power = power;
	}

	public PowerPlantData getData() {
		return data;
	}

	public void setData(PowerPlantData data) {
		this.data = data;
	}

	public void initialize() {
		for (Map.Entry<String, String> entry : data.getMap().entrySet()) {
			if (entry.getKey().contains("Init")) { // then this is a state
													// variable
				String ident = entry.getKey().substring(0,
						entry.getKey().indexOf("Init"));
				if (ident.equalsIgnoreCase("Power")) {
					double powerInit = Double.parseDouble(entry.getValue());
					this.power = new Interval<Double>(powerInit);
				} else if (ident.equalsIgnoreCase("consRunning")) {
					this.consRunning = new Interval<Integer>(
							Integer.parseInt(entry.getValue()));
				} else if (ident.equalsIgnoreCase("consStopping")) {
					this.consStopping = new Interval<Integer>(
							Integer.parseInt(entry.getValue()));
				}
			}
		}
		this.running = new Interval<Boolean>(this.consRunning == null
				|| this.consRunning.max > 0);
	}

	public void updateConstraints() {
		if (getData().getAssociatedConstraints() == null)
			return;

		for (Constraint c : getData().getAssociatedConstraints()) {
			if (c instanceof PlantConstraint) {
				PlantConstraint pc = (PlantConstraint) c;
				pc.setPlant(this);
			}
		}
	}

	@Override
	public Interval<Boolean> isRunning() {
		return running;
	}

	@Override
	public boolean onlyOn() {
		return running.max && running.min;
	}

	@Override
	public boolean onlyOff() {
		return !running.max && !running.min;
	}

	@Override
	public boolean onOrOff() {
		return !onlyOff() && !onlyOn();
	}

	@Override
	public void setRunning(Interval<Boolean> running) {
		this.running = running;
	}

	public Interval<Integer> getConsRunning() {
		return consRunning;
	}

	public void setConsRunning(Interval<Integer> consRunning) {
		this.consRunning = consRunning;
	}

	public Interval<Integer> getConsStopping() {
		return consStopping;
	}

	public void setConsStopping(Interval<Integer> consStopping) {
		this.consStopping = consStopping;
	}

	public void updateRunning(boolean on_min_t_inc, boolean on_max_t_inc) {
		if (on_max_t_inc) { // on now
			if (isRunning().max) // was already on
				++consRunning.max;
			else {
				consRunning.max = 1;
				consStopping.max = 0;
			}
		} else { // still not on
			++consStopping.max;
		}

		if (!on_min_t_inc) { // finally off
			if (!isRunning().min) // was already off
				++consStopping.min;
			else {
				consRunning.min = 0;
				consStopping.min = 1;
			}
		} else { // still running
			++consRunning.min;
		}
		isRunning().min = on_min_t_inc;
		isRunning().max = on_max_t_inc;
	}

	public PowerPlantState copy() {
		PowerPlantState copiedState = new PowerPlantState();
		copiedState.setConsRunning(consRunning.copy());
		copiedState.setConsStopping(consStopping.copy());
		copiedState.setData(data);
		copiedState.setPower(power.copy());
		copiedState.setRunning(running.copy());
		copiedState.setSimulationStep(simulationStep);
		return copiedState;
	}

	@Override
	public String printState() {
		StringBuilder sb = new StringBuilder("+-+-+-+-+-+-+-+-+-+-+-+-+-+-+\n");
		sb.append("+ " + getData().getName());
		sb.append("\n+ running: " + isRunning().toString());
		sb.append("\n+ power: " + getPower().toString());
		sb.append("\n+ cons running : " + getConsRunning().toString());
		sb.append("\n+ cons stopping : " + getConsStopping().toString());
		sb.append("\n+-+-+-+-+-+-+-+-+-+-+-+-+-+-+\n");
		return sb.toString();
	}

	@Override
	public final String getName() {
		return getData().getName();
	}

}
