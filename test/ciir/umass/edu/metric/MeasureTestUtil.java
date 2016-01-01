package ciir.umass.edu.metric;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.DenseDataPoint;
import ciir.umass.edu.learning.RankList;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jfoley.
 */
public class MeasureTestUtil {
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

}
