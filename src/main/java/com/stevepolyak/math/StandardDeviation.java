package com.stevepolyak.math;

public class StandardDeviation {

	public static double standardDeviationMean(double[] data) {
		// sd is sqrt of sum of (values-mean) squared divided by n - 1
		// Calculate the mean
		double mean = 0;
		final int n = data.length;
		if (n < 2) {
			return Double.NaN;
		}
		for (int i = 0; i < n; i++) {
			mean += data[i];
		}
		mean /= n;
		// calculate the sum of squares
		double sum = 0;
		for (int i = 0; i < n; i++) {
			final double v = data[i] - mean;
			sum += v * v;
		}
		// Change to ( n - 1 ) to n if you have complete data instead of a
		// sample.
		return Math.sqrt(sum / (n - 1));
	}
	
	public static double standardDeviationCalculate(Integer[] data) {
		return standardDeviationCalculate(copyFromIntArray(data));
	}

	public static double standardDeviationCalculate(double[] data) {
		final int n = data.length;
		if (n < 2) {
			return Double.NaN;
		}
		double avg = data[0];
		double sum = 0;
		for (int i = 1; i < data.length; i++) {
			double newavg = avg + (data[i] - avg) / (i + 1);
			sum += (data[i] - avg) * (data[i] - newavg);
			avg = newavg;
		}
		// Change to ( n - 1 ) to n if you have complete data instead of a
		// sample.
		return Math.sqrt(sum / n);
	}
	
	public static double[] copyFromIntArray(Integer[] source) {
	    double[] dest = new double[source.length];
	    for(int i=0; i<source.length; i++) {
	        dest[i] = source[i];
	    }
	    return dest;
	}
	
	public static void main(String[] args) {
		double[] data = { 10, 100, 50 };
		System.out.println(standardDeviationMean(data));
		System.out.println(standardDeviationCalculate(data));
	}
}