package ciir.umass.edu.learning;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author jfoley
 */
public class PointBuilderTest {

  @Test
  public void testPointBuilder() {
    Dataset ds = new Dataset();
    PointBuilder pb = ds.makePointBuilder();
    pb.setQID("qid").setDescription("#doc").set(1, 1f).set(20, 2f).setLabel(3f);
    assertEquals(3f, pb.getLabel(), 0.00001f);
    assertTrue(pb.hasFeature(20));
    assertEquals(2f, pb.getRawFeatures()[20], 0.00001f);
    assertTrue(pb.hasFeature(1));
    assertEquals(1f, pb.getRawFeatures()[1], 0.00001f);
    assertEquals(Float.NaN, pb.getRawFeatures()[0], 0.00001f);
    assertEquals("#doc", pb.getDescription());
    assertEquals("qid", pb.getQID());

    assertEquals(20, ds.getMaxFeaturePosition());
    assertEquals(20, pb.getMaxObservedFeature());
    assertEquals(2, pb.getObservedFeatures().cardinality());
  }

  @Test
  public void testPointBuilderErr() {
    Dataset ds = new Dataset();
    PointBuilder pb = ds.makePointBuilder();
     try {
       pb.setDescription("doc");
       fail("PointBuilder should check well-formedness of Descriptions...");
     } catch (IllegalArgumentException iae) {
       assertNotNull(iae);
     }

  }

}