package ciir.umass.edu.metric;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.DenseDataPoint;
import ciir.umass.edu.learning.RankList;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author jfoley.
 */
public class PrecisionScorerTest {
  public DataPoint pt(int label) {
    DenseDataPoint x = new DenseDataPoint();
    x.setLabel(label);
    return x;
  }

  public RankList list(int... labels) {
    List<DataPoint> pts = new ArrayList<>();
    for (int label : labels) {
      pts.add(pt(label));
    }
    return new RankList(pts);
  }

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