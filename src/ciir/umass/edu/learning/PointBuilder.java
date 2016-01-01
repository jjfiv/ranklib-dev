package ciir.umass.edu.learning;

import java.util.Arrays;
import java.util.BitSet;

/**
 * @author jfoley.
 */
public class PointBuilder {
  final Dataset dataset;
  String description = null;
  String qid = null;
  float label = -1;
  private float[] fVals;
  /**
   * This is the max_id observed for the data point we're building.
   */
  private int lastFeature = 0;
  private BitSet knownFeatures = new BitSet();

  public PointBuilder(Dataset dataset) {
    this.dataset = dataset;

    // create float buffer and fill it with unknown
    this.fVals = new float[dataset.maxFeature];
    Arrays.fill(fVals, DataPoint.UNKNOWN);
  }

  public PointBuilder setDescription(String description) {
    this.description = description;
    return this;
  }

  public PointBuilder setLabel(float label) {
    this.label = label;
    return this;
  }

  public PointBuilder setQID(String qid) {
    this.qid = qid;
    return this;
  }

  public void set(int fid, float value) {
    knownFeatures.set(fid);
    if (dataset.resizeToFit(fid)) {
      // make a new buffer
      float[] tmp = new float[dataset.maxFeature];
      // fill it with NaNs
      Arrays.fill(tmp, DataPoint.UNKNOWN);
      // copy the old fVals into the new buffer
      for (int i = 0; i < fVals.length; i++) {
        tmp[i] = fVals[i];
      }
      fVals = tmp;
    }

    this.lastFeature = Math.max(this.lastFeature, fid);
    fVals[fid] = value;
  }

  public int getMaxObservedFeature() {
    return this.lastFeature;
  }

  // TODO, shrink?
  public float[] getRawFeatures() {
    return Arrays.copyOf(this.fVals, this.lastFeature + 1);
  }

  public DenseDataPoint toDensePoint() {
    DenseDataPoint pt = new DenseDataPoint();
    build(pt);
    return pt;
  }

  public Dataset getDataset() {
    return dataset;
  }

  public SparseDataPoint toSparsePoint() {
    SparseDataPoint pt = new SparseDataPoint();
    build(pt);
    return pt;
  }

  private void checkDoneBuilding() {
    assert (qid != null) : "QID must be set for a data point...";
    assert (label >= 0) : "Label must be non-negative";
    assert (knownFeatures.size() > 0) : "Point must have features.";
  }

  public void build(DataPoint pt) {
    checkDoneBuilding();

    pt.setID(qid);
    pt.setDescription(description);
    pt.setLabel(label);
    pt.setFeatureVector(getRawFeatures());
    pt.setKnownFeatures(knownFeatures.cardinality());
  }
}
