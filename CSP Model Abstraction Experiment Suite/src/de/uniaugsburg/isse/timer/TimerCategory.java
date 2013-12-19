package de.uniaugsburg.isse.timer;

public enum TimerCategory {
	TOTAL_RUNTIME_CENTRAL(0), TOTAL_RUNTIME_REGIOCENTRAL(1), RUNTIME_CENTRAL_TS(
			2), RUNTIME_REGIOCENTRAL_TS(3), ABSTRACTION_RUNTIME(4), AVPP_TIME(5);

	public final int id;

	TimerCategory(int id) {
		this.id = id;
	}
}
