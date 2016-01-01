package ciir.umass.edu.features;

import ciir.umass.edu.learning.Dataset;
import ciir.umass.edu.learning.PointBuilder;
import ciir.umass.edu.utilities.RankLibError;

/**
 * @author jfoley.
 */
public class LibSVMFormat {

  static String getKey(String pair) {
    return pair.substring(0, pair.indexOf(":"));
  }
  static String getValue(String pair) {
    return pair.substring(pair.lastIndexOf(":")+1);
  }

  public static PointBuilder parsePoint(String text, Dataset dataset) {
    PointBuilder pb = dataset.makePointBuilder();

    try {
      int idx = text.indexOf("#");
      if(idx != -1) {
        pb.setDescription(text.substring(idx));
        text = text.substring(0, idx).trim();//remove the comment part at the end of the line
      }
      String[] fs = text.split("\\s+");

      float label = Float.parseFloat(fs[0]);
      if(label < 0) {
        System.out.println("Relevance label cannot be negative. System will now exit.");
        System.exit(1);
      }
      pb.setLabel(label);
      pb.setQID(getValue(fs[1]));
      for(int i=2;i<fs.length;i++)
      {
        String key = getKey(fs[i]);
        String val = getValue(fs[i]);
        int f = Integer.parseInt(key);
        if(f <= 0) throw RankLibError.create("Cannot use feature numbering less than or equal to zero. Start your features at 1.");

        float fVal = Float.parseFloat(val);
        pb.set(f, fVal);
      }

      return pb;
    } catch(Exception ex) {
      throw RankLibError.create("Error in LibSVMFormat::parsePoint()", ex);
    }
  }
}
