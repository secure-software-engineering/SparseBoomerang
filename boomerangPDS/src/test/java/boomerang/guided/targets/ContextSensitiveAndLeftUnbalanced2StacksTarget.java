package boomerang.guided.targets;

import java.io.File;

public class ContextSensitiveAndLeftUnbalanced2StacksTarget {

  public static void main(String... args) {
    context();
  }

  private static void context() {
    String barParam = "bar";
    String bar = doPassArgument(barParam);
    String foo = doPassArgument("foo");
    String quz = doPassArgument("quz");
    new File(bar);
    new File(foo);
    new File(quz);
  }

  private static String doPassArgument(String paramDoPassArgument) {
    return wrapped(paramDoPassArgument);
  }

  private static String wrapped(String paramWrapped) {
    return new String(paramWrapped);
  }
}
