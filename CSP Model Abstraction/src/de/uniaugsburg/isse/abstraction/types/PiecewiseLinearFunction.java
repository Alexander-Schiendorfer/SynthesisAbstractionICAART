package de.uniaugsburg.isse.abstraction.types;

import java.util.Arrays;

/**
 * This class represents a piecewise linear function given by breakpoints and slopes as specified by CPLEX can also
 * convert a set of input/output pairs
 * 
 * @author alexander
 * 
 */
public class PiecewiseLinearFunction {
	private double[] s; // slopes, n+1 of them
	private double[] t; // change values, n of them
	private double t0; // ground in
	private double v0; // ground out: v0 = f(t0)
	private String nDef = "n";
	private String firstInDef = "firstIn";
	private String fAtFirstDef = "fAtFirst";
	private String breakpointDef = "breakpoint";
	private String slopeDef = "slope";
	private double[] ins, outs;
	private int numberInputOutputPairs;

	public PiecewiseLinearFunction() {
		// default constructor
	}

	/**
	 * Convenience constructor to create a piecewise linear function from min, max and c
	 * 
	 * @param min
	 * @param max
	 * @param c
	 */
	public PiecewiseLinearFunction(double min, double max, double c) {
		this.convertFromLinearFunction(min, max, c);
	}

	public double evaluate(double in) {
		return in;
	}

	/**
	 * Convert from a linear function that is defined for the interval [minInput, maxOutput] with slope k
	 * 
	 * @param minInput
	 *            lower bound for domain
	 * @param maxOutput
	 *            upper bound for domain
	 * @param k
	 *            slope for linear function
	 */
	public void convertFromLinearFunction(double minInput, double maxOutput, double k) {
		this.s = new double[] { k, k, k };
		this.t = new double[] { minInput, maxOutput };
		this.t0 = minInput;
		this.v0 = k * minInput;
	}

	public void convert(double[] in, double[] out) {
		this.convert(in, out, in.length);
	}

	/**
	 * use s[1] as s[0], slope for -inf to t[0] and s[s.length-2] as s[s.length-1] for t[t.length] to +inf
	 */
	public void prolongAdInfinitum() {
		if (this.s == null || this.s.length < 3)
			return;
		this.s[0] = this.s[1];
		this.s[this.s.length - 1] = this.s[this.s.length - 2];
	}

	/**
	 * converts sets of input output pairs such that \forall i out[i] = f(in[i])
	 * 
	 * @param in
	 * @param out
	 */
	public void convert(double[] in, double[] out, int n) {
		if (in.length != out.length || in.length < 2)
			return; // maybe say something mean
		this.t0 = in[0];
		this.v0 = out[0];
		this.numberInputOutputPairs = n;
		this.s = new double[n + 1];
		this.t = new double[n];
		// from infinity to t0, let slope be 0 (constant)
		this.s[0] = 0;
		this.t[0] = in[0];
		// same for values beyond in[n-1]
		this.s[n] = 0;
		for (int i = 1; i < n; ++i) {
			if (in[i] - in[i - 1] < 0.00001) // avoid division by zero
				this.s[i] = 0;
			else
				this.s[i] = (out[i] - out[i - 1]) / (in[i] - in[i - 1]);

			if (Math.abs(this.s[i]) < 0.00001)
				this.s[i] = 0.0;
			this.t[i] = in[i];
		}
		this.ins = in;
		this.outs = out;
	}

	public String toCplex(String n, String firstIn, String fAtFirst, String breakpoint, String slope) {
		if (this.t == null)
			return "";
		StringBuilder sb = new StringBuilder();
		sb.append(n + "=" + this.t.length + ";\n");
		sb.append(firstIn + "=" + this.t0 + ";\n");
		sb.append(fAtFirst + "=" + this.v0 + ";\n");
		sb.append(breakpoint + "=" + this.toDoubleArrayString(this.t) + ";\n");
		sb.append(slope + "=" + this.toDoubleArrayString(this.s) + ";\n");
		return sb.toString();
	}

	public String toDoubleArrayString(double[] t2) {
		return this.toDoubleArrayString(t2, 0);
	}

	private double[] toDoubleArray(double[] t2, int zerosToAdd) {
		return Arrays.copyOf(t2, t2.length + zerosToAdd);
	}

	private String toDoubleArrayString(double[] t2, int zerosToAdd) {
		double[] doubleArray = this.toDoubleArray(t2, zerosToAdd);
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		sb.append("[");
		for (int i = 0; i < doubleArray.length; ++i) {
			if (!first)
				sb.append(", ");
			else
				first = false;
			sb.append(doubleArray[i]);
		}
		sb.append("]");
		return sb.toString();
	}

	public String toCplex() {
		return this.toCplex(this.nDef, this.firstInDef, this.fAtFirstDef, this.breakpointDef, this.slopeDef);
	}

	public int getBPs() {
		return this.t.length;
	}

	public double getFirstInput() {
		return this.t0;
	}

	public double getFirstOutput() {
		return this.v0;
	}

	/**
	 * Convenience method that formats the slopes array as "[d[i], ..., d[n]]" as necessary for e.g. CPLEX
	 * 
	 * @return
	 */
	public String getSlopesString() {
		return this.toDoubleArrayString(this.s);
	}

	/**
	 * Convenience method that formats the breakpoint array as "[d[i], ..., d[n]]" as necessary for e.g. CPLEX
	 * 
	 * @return
	 */
	public String getBreakpointString() {
		return this.toDoubleArrayString(this.t);
	}

	public String writeInputOutput() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < this.ins.length; ++i) {
			sb.append(this.ins[i] + ";" + this.outs[i] + "\n");
		}
		return sb.toString();
	}

	public double[] getIns() {
		return this.ins;
	}

	public void setIns(double[] ins) {
		this.ins = ins;
	}

	public double[] getOuts() {
		return this.outs;
	}

	public void setOuts(double[] outs) {
		this.outs = outs;
	}

	public int getNumberInputOutputPairs() {
		return this.numberInputOutputPairs;
	}

	public void setNumberInputOutputPairs(int numberInputOutputPairs) {
		this.numberInputOutputPairs = numberInputOutputPairs;
	}

	/**
	 * Returns the slopes string but pads the set with 0s (in case of a maxBPs value)
	 * 
	 * @param maxBps
	 * @return
	 */
	public String getSlopesString(int maxBps) {
		int diff = maxBps + 1 - this.s.length;
		return this.toDoubleArrayString(this.s, diff);
	}

	public String getBreakpointString(int maxBps) {
		return this.toDoubleArrayString(this.t, maxBps - this.t.length);
	}

	public double[] getSlopes(int maxBps) {
		int diff = maxBps + 1 - this.s.length;
		return this.toDoubleArray(this.s, diff);
	}

	public double[] getBreakpoints(int maxBps) {
		return this.toDoubleArray(this.t, maxBps - this.t.length);
	}

	public boolean isEmpty() {
		return this.t == null || this.t.length == 0;
	}

	public double getMaxSlope() {
		double max = Double.NEGATIVE_INFINITY;
		for(int i = 0; i < s.length; ++i) {
			max = Math.max(s[i] , max);
		}
		return max;
	}

	public void divideSlopesBy(double d) {
		this.v0 /= d;
		for(int i = 0; i < s.length; ++i) 
			s[i] /= d;
	}

	public void multiplySlopesWith(double d) {
		this.v0 *= d;
		for(int i = 0; i < s.length; ++i) 
			s[i] *= d;
	}
}
