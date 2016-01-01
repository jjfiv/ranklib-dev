package ciir.umass.edu.stats;

import ciir.umass.edu.utilities.RankLibError;

public class BasicStats {
	public static double mean(double[] values)
	{
		double mean = 0.0;
		if(values.length == 0) {
			throw new RankLibError("Error in BasicStats::mean(): Empty input array.");
		}
		for (double value : values) mean += value;
		return mean / (double) values.length;
	}
}
