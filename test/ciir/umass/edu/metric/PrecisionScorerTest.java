package ciir.umass.edu.metric;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author jfoley.
 */
public class PrecisionScorerTest extends MeasureTestUtil {
  @Test
  public void testSimple() {
    PrecisionScorer p10 = new PrecisionScorer();
    assertEquals("P@10", p10.name());
    assertEquals(0.0, p10.score(list(0,0,0,0,0,0,0,0,0,0)), 0.0001);
    assertEquals(0.2, p10.score(list(1,1,0,0,0,0,0,0,0,0)), 0.0001);
    assertEquals(0.6, p10.score(list(1,1,0,0,1,1,0,1,0,1)), 0.0001);
    assertEquals(1.0, p10.score(list(1,1,1,1,1,1,1,1,1,1)), 0.0001);

    assertEquals(1.0, (new PrecisionScorer(2)).score(list(1,1,0,0,0,0,0,0,0,0)), 0.0001);
  }

}