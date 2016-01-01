package ciir.umass.edu.learning;

import ciir.umass.edu.learning.boosting.AdaRank;
import ciir.umass.edu.learning.boosting.RankBoost;
import ciir.umass.edu.learning.neuralnet.LambdaRank;
import ciir.umass.edu.learning.neuralnet.ListNet;
import ciir.umass.edu.learning.neuralnet.RankNet;
import ciir.umass.edu.learning.tree.LambdaMART;
import ciir.umass.edu.learning.tree.MART;
import ciir.umass.edu.learning.tree.RFRanker;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author jfoley.
 */
public class RankerFactoryTest {

  @Test
  public void testRankerFactory() {
    RankerFactory rf = new RankerFactory();

    // Rankers can be created by ordinal:
    assertTrue(rf.createRanker(RankerType.MART) instanceof MART);
    assertTrue(rf.createRanker(RankerType.RANKBOOST) instanceof RankBoost);
    assertTrue(rf.createRanker(RankerType.RANKNET) instanceof RankNet);
    assertTrue(rf.createRanker(RankerType.ADARANK) instanceof AdaRank);
    assertTrue(rf.createRanker(RankerType.COOR_ASCENT) instanceof CoorAscent);
    assertTrue(rf.createRanker(RankerType.LAMBDARANK) instanceof LambdaRank);
    assertTrue(rf.createRanker(RankerType.LAMBDAMART) instanceof LambdaMART);
    assertTrue(rf.createRanker(RankerType.LISTNET) instanceof ListNet);
    assertTrue(rf.createRanker(RankerType.RANDOM_FOREST) instanceof RFRanker);
    assertTrue(rf.createRanker(RankerType.LINEAR_REGRESSION) instanceof LinearRegRank);

    for (Ranker ranker : rf.getAllRankers()) {
      assertEquals(ranker.getClass().getName(), rf.createRanker(ranker.name()).getClass().getName());
    }

  }
}