package ciir.umass.edu.stats;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;

/**
 * @author jfoley.
 */
public class RandomPermutationTestTest {

  @Test
  public void testRandomPermNoDiff() {
    HashMap<String, Double> seriesX = new HashMap<>();
    HashMap<String, Double> seriesY = new HashMap<>();

    seriesX.put("A", 2.0);
    seriesY.put("A", 2.0);

    seriesX.put("B", 2.0);
    seriesY.put("B", 2.0);

    seriesX.put("C", 2.0);
    seriesY.put("C", 2.0);

    RandomPermutationTest tester = new RandomPermutationTest();
    double effect = tester.test(seriesX, seriesY);

    // They're the same, p-value should be 1:
    assertEquals(1.0, effect, 0.0001);
  }

  @Test
  public void testRandomPermObviousDiff() {
    HashMap<String, Double> seriesX = new HashMap<>();
    HashMap<String, Double> seriesY = new HashMap<>();

    for (int i = 0; i < 100; i++) {
      String key = Integer.toString(i);
      seriesX.put(key, 100.0);
      seriesY.put(key, 0.0);
    }

    RandomPermutationTest tester = new RandomPermutationTest();
    double effect = tester.test(seriesX, seriesY);

    // They're totally different, p-value should be 0:
    assertEquals(0.0, effect, 0.0001);
  }

}