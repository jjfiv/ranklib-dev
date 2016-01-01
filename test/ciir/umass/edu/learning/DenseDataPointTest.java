package ciir.umass.edu.learning;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author jfoley.
 */
public class DenseDataPointTest {

  @Test
  public void testSimpleParse() {
    DenseDataPoint dp = new DenseDataPoint("1 qid:foo 1:0.5 2:0.7 3:0.9 5:0.0 # name");

    assertEquals(0.5f, dp.getFeatureValue(1), 0.00001f);
    assertEquals(0.7f, dp.getFeatureValue(2), 0.00001f);
    assertEquals(0.9f, dp.getFeatureValue(3), 0.00001f);
    assertEquals(0.0f, dp.getFeatureValue(4), 0.00001f);
    assertEquals(0.0f, dp.getFeatureValue(5), 0.00001f);

    float[] inner = new float[] {
        Float.NaN, 0.5f, 0.7f, 0.9f, Float.NaN, 0.0f
    };
    assertArrayEquals(inner, dp.getFeatureVector(), 0.001f);

    DenseDataPoint copy = new DenseDataPoint(dp);
    float[] different = new float[] {
        0f, 0f, 0f, 0f, 0f, 0f
    };
    dp.setFeatureVector(different);
    assertArrayEquals(inner, copy.getFeatureVector(), 0.001f);
    assertArrayEquals(different, dp.getFeatureVector(), 0.001f);

    assertEquals(1.0f, dp.getLabel(), 0.00001f);
    assertEquals(1, dp.getIntLabel());
    assertEquals("foo", dp.getID());
    assertEquals("# name", dp.getDescription());
  }

  // Test which should trigger Ranklib's resizing logic...
  @Test
  public void testHugeVector() {
    StringBuilder sb = new StringBuilder();
    sb.append("1 qid:foo ");
    for (int i = 0; i < 1000; i++) {
      sb.append(i+1).append(':').append(i).append(' ');
    }
    sb.append("# test");

    //System.out.println(sb.toString());
    DenseDataPoint pt = new DenseDataPoint(sb.toString());
    for (int i = 0; i < 1000; i++) {
      float fval = pt.getFeatureValue(i+1);
      assertEquals(fval, i, 0.00001f);
    }
  }

}