package boomerang.guided.targets;

public class CustomFlowFunctionTarget {

  public static void main(String... args) {
    int x = 1;
    int y = x + 1;
    Object z = new Object();
    System.exit(y);
    queryFor(z);
  }

  private static void queryFor(Object x) {}
}
