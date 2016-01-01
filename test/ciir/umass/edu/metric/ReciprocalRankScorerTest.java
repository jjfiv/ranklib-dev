package ciir.umass.edu.metric;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author jfoley.
 */
public class ReciprocalRankScorerTest extends MeasureTestUtil {
  @Test
  public void testSimple() {
    ReciprocalRankScorer rr = new ReciprocalRankScorer();
    assertEquals("RR", rr.name());
    assertEquals(0.0, rr.score(list(0,0,0,0,0,0,0,0,0,0)), 0.0001);
    assertEquals(1.0, rr.score(list(1,1,0,0,0,0,0,0,0,0)), 0.0001);
    assertEquals(0.5, rr.score(list(0,1,0,0,1,1,0,1,0,1)), 0.0001);
    assertEquals(0.2, rr.score(list(0,0,0,0,1,1,1,1,1,1)), 0.0001);
  }
}