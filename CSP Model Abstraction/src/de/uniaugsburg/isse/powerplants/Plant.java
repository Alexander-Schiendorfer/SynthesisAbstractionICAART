package de.uniaugsburg.isse.powerplants;

import de.uniaugsburg.isse.abstraction.types.Interval;

public interface Plant {
	Interval<Double> getPower();
	void setPower(Interval<Double> power);
	Interval<Boolean> isRunning();
	
	Interval<Integer> getConsRunning();
	void setConsRunning(Interval<Integer> consRunning);

	Interval<Integer> getConsStopping();
	void setConsStopping(Interval<Integer> consStopping); 
	/* ==== convenience methods for checking the possible states sigma */
	boolean onlyOn();
	boolean onlyOff();
	boolean onOrOff();
	void setRunning(Interval<Boolean> running);
	
	String printState();
	String getName();
	public abstract void setSimulationStep(int simulationStep);
	public abstract int getSimulationStep();
}
