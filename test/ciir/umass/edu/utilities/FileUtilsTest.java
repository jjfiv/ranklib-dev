package ciir.umass.edu.utilities;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * @author jfoley.
 */
public class FileUtilsTest {
  @Test
  public void testFileUtils() throws IOException {
    String encoding = "ASCII";
    try (TmpFile tmp = new TmpFile()) {
      FileUtils.write(tmp.getPath(), encoding, "Hello\nWorld");
      assertEquals(Arrays.asList("Hello", "World"), FileUtils.readLine(tmp.getPath(), encoding));
    }
  }
}