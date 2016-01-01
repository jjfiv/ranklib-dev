package ciir.umass.edu.stats;

import ciir.umass.edu.utilities.RankLibError;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author jfoley.
 */
public class BasicStatsTest {
  @Test
  public void testMean() {
    double[] data = {1.0, 2.0, 3.0};
    assertEquals(2.0, BasicStats.mean(data), 0.00001);
  }

  @Test
  public void testEmptyArrayMean() {
    double[] data = {};
    try {
      BasicStats.mean(data);
      fail("Expected error on empty array!");
    } catch (RankLibError err) {
      assertNotNull(err);
    }
  }

}