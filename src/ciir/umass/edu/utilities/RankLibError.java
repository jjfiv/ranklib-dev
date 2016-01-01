package ciir.umass.edu.utilities;

/**
 * Instead of using random error types, use RankLibError exceptions throughout
 *   -- this means that clients can catch-all from us easily.
 * @author jfoley
 */
public class RankLibError extends RuntimeException {
  public RankLibError(Throwable e) { super(e); }
  public RankLibError(String message) {
    super(message);
  }
  public RankLibError(String message, Throwable cause) {
    super(message, cause);
  }

  /** Don't rewrap RankLibErrors in RankLibErrors */
  public static RankLibError create(Throwable e) {
    if(e instanceof RankLibError) {
      return (RankLibError) e;
    }
    return new RankLibError(e);
  }

  public static RankLibError create(String message) {
    return new RankLibError(message);
  }

  /** Don't rewrap RankLibErrors in RankLibErrors */
  public static RankLibError create(String message, Throwable cause) {
    if(cause instanceof RankLibError) {
      return (RankLibError) cause;
    }
    return new RankLibError(message, cause);
  }
}
