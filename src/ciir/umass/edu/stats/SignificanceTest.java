package ciir.umass.edu.stats;

import java.util.Map;

public interface SignificanceTest {
	double test(Map<String, Double> target, Map<String, Double> baseline);
}
