package de.uniaugsburg.isse.abstraction.types;

import java.io.Serializable;

/**
 * Unified Interval type for model abstraction primary use: Intervals of power ranges (Double)
 * 
 * @author Alexander Schiendorfer
 * 
 * @param <T>
 */
public class Interval<T extends Comparable<T>> implements Comparable<Interval<T>>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4969023021230907321L;

	public T min;
	public T max;

	public Interval(T min, T max) {
		super();
		this.min = min;
		this.max = max;
	}

	public Interval(T powerInit) {
		this(powerInit, powerInit);
	}

	@Override
	public String toString() {
		return "[" + this.min + " " + this.max + "]";
	}

	public Interval<T> copy() {
		return new Interval<T>(this.min, this.max);
	}

	@Override
	public int compareTo(Interval<T> o) {
		// no overlaps, so just compare min element
		// but take care of equal elements
		if (this.min.equals(o.min))
			return ((Integer) this.min.hashCode()).compareTo(o.min.hashCode());
		else
			return this.min.compareTo(o.min);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.max == null) ? 0 : this.max.hashCode());
		result = prime * result + ((this.min == null) ? 0 : this.min.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		Interval<?> other = (Interval<?>) obj;
		if (this.max == null) {
			if (other.max != null)
				return false;
		} else if (!this.max.equals(other.max))
			return false;
		if (this.min == null) {
			if (other.min != null)
				return false;
		} else if (!this.min.equals(other.min))
			return false;
		return true;
	}
}
