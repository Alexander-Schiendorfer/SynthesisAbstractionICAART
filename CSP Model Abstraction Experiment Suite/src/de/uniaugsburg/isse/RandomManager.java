package de.uniaugsburg.isse;

import java.util.Random;

public class RandomManager {
	private static Random random;

	public static void initialize(long seed) {
		if (random == null)
			random = new Random(seed);
	}

	public static double getDouble(double randPMin, double randPMax) {
		testRandom();
		return randPMin + (randPMax - randPMin) * random.nextDouble();
	}

	private static void testRandom() {
		if (random == null)
			throw new RuntimeException(
					"Random needs to be initialzed beforehand");

	}

	public static boolean getBoolean(double pTrue) {
		testRandom();
		return (random.nextDouble() <= pTrue);
	}

	public static int getInt(int size) {
		testRandom();
		return random.nextInt(size);
	}

	public static int getIntNormal(int mean, int stddev) {
		testRandom();
		return (int) Math.round(mean + random.nextGaussian() * stddev);
	}

	/**
	 * Needed for call to Collections.shuffle
	 * 
	 * @return
	 */
	public static Random getRandom() {
		return random;
	}

}
