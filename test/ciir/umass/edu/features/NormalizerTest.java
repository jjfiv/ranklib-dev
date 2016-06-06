package ciir.umass.edu.features;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.Dataset;
import ciir.umass.edu.learning.PointBuilder;
import ciir.umass.edu.learning.RankList;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;

/**
 * @author jfoley.
 */
public class NormalizerTest {

  @Test
  public void testLinearNormalization() {
    float[] xs = new float[] { 1, 2, 3, 4, 5 };
    float[] ys = new float[] { 0, 0, 0, 0, 10 };

    Dataset ds = new Dataset();
    List<DataPoint> pts = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      PointBuilder pb = new PointBuilder(ds);
      pb.set(1, xs[i]);
      pb.set(2, ys[i]);
      pb.setLabel(1);
      pb.setQID("foo");
      pts.add(pb.toDensePoint());
    }

    RankList query = new RankList(pts);
    ds.samples.add(query);

    Normalizer nml = new LinearNormalizer();
    nml.normalize(ds);

    float[] xns = new float[5];
    float[] yns = new float[5];
    for (int i = 0; i < query.size(); i++) {
      DataPoint pt = query.get(i);
      xns[i] = pt.getFeatureValue(1);
      yns[i] = pt.getFeatureValue(2);
    }

    //System.out.println(Arrays.toString(xns));
    //System.out.println(Arrays.toString(yns));
    assertArrayEquals(new float[]{0.2f, 0.4f, 0.6f, 0.8f, 1f}, xns, 0.0001f);
    assertArrayEquals(new float[]{0.0f, 0.0f, 0.0f, 0.0f, 1f}, yns, 0.0001f);
  }

  @Test
  public void testSumNormalization() {
    float[] xs = new float[] { 1, 1, 1, 1, 1 };
    float[] ys = new float[] { 0, 0, 0, 0, 1 };

    Dataset ds = new Dataset();
    List<DataPoint> pts = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      PointBuilder pb = new PointBuilder(ds);
      pb.set(1, xs[i]);
      pb.set(2, ys[i]);
      pb.setLabel(1);
      pb.setQID("foo");
      pts.add(pb.toDensePoint());
    }

    RankList query = new RankList(pts);
    ds.samples.add(query);

    Normalizer nml = new SumNormalizor();
    nml.normalize(ds);

    float[] xns = new float[5];
    float[] yns = new float[5];
    for (int i = 0; i < query.size(); i++) {
      DataPoint pt = query.get(i);
      xns[i] = pt.getFeatureValue(1);
      yns[i] = pt.getFeatureValue(2);
    }

    System.out.println(Arrays.toString(xns));
    System.out.println(Arrays.toString(yns));
    assertArrayEquals(new float[]{0.2f, 0.2f, 0.2f, 0.2f, 0.2f}, xns, 0.0001f);
    assertArrayEquals(new float[]{0f, 0f, 0f, 0f, 1f}, yns, 0.0001f);
  }



}