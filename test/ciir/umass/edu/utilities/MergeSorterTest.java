package ciir.umass.edu.utilities;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author jfoley.
 */
public class MergeSorterTest {

  @Test
  public void testSimpleSortFloat() {
    float[] rwd = new float[] { 5, 4, 3, 2, 1 };
    float[] fwd = new float[] { 1, 2, 3, 4, 5 };
    int[] idx = MergeSorter.sort(rwd, true);
    int[] idx2 = MergeSorter.sort(fwd, false);
    for (int i = 0; i < idx.length; i++) {
      assertEquals(rwd[idx[i]] , fwd[i], 0.00001f);
      assertEquals(rwd[i] , fwd[idx2[i]], 0.00001f);
    }
  }

  @Test
  public void testSimpleSortDouble() {
    double[] rwd = new double[] { 5, 4, 3, 2, 1 };
    double[] fwd = new double[] { 1, 2, 3, 4, 5 };
    int[] idx = MergeSorter.sort(rwd, true);
    int[] idx2 = MergeSorter.sort(fwd, false);
    for (int i = 0; i < idx.length; i++) {
      assertEquals(rwd[idx[i]] , fwd[i], 0.00001);
      assertEquals(rwd[i] , fwd[idx2[i]], 0.00001);
    }
  }
}