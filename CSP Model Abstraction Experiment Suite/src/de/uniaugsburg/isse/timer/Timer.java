package de.uniaugsburg.isse.timer;

public class Timer {
	private static final int MAX_IND = 15;
	private long startTimes[];
	private long endTimes[];

	public Timer() {
		startTimes = new long[MAX_IND + 1];
		endTimes = new long[MAX_IND + 1];
	}

	public void tick(int ident) {
		startTimes[ident] = System.nanoTime();
	}

	public long tock(int ident) {
		endTimes[ident] = System.nanoTime();
		return endTimes[ident] - startTimes[ident];
	}

	public double getElapsedSecs(TimerCategory totalRuntimeCentral) {
		long elapsed = endTimes[totalRuntimeCentral.id]
				- startTimes[totalRuntimeCentral.id];
		return ((double) elapsed) * 1.0e-9;
	}
}
