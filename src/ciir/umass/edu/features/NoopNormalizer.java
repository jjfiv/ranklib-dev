package ciir.umass.edu.features;

import ciir.umass.edu.learning.Dataset;
import ciir.umass.edu.learning.RankList;

/**
 * @author jfoley.
 */
public class NoopNormalizer extends Normalizer {
  @Override
  public void normalize(Dataset ds, RankList rl, int[] fids) { }

  @Override
  public String name() {
    return "noop";
  }

  @Override
  public boolean isNoop() {
    return true;
  }
}
