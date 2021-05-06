package boomerang.guided.targets;

import java.io.File;

public class ContextSensitiveAndLeftUnbalancedFieldTarget {

  public static void main(String... args) {
    MyObject myObject = new MyObject();

    context(myObject.field);
  }

  private static void context(String barParam) {
    String bar = doPassArgument(barParam);
    new File(bar);
  }

  private static String doPassArgument(String param) {
    return new String(param);
  }

  private static class MyObject {
    private String field = "bar";
  }
}
