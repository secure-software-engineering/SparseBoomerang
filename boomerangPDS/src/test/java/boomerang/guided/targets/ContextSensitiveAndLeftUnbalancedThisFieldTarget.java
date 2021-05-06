package boomerang.guided.targets;

import java.io.File;

public class ContextSensitiveAndLeftUnbalancedThisFieldTarget {

  public static void main(String... args) {
    new MyObject().context();
  }

  public static class MyObject {
    private String field = "bar";

    private static String doPassArgument(String param) {
      return new String(param);
    }

    public void context() {
      String bar = doPassArgument(field);
      new File(bar);
    }
  }
}
