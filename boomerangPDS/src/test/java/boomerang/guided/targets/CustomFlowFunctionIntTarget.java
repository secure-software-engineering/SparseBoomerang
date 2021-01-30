package boomerang.guided.targets;

public class CustomFlowFunctionIntTarget {

  public static void main(String... args) {
    int z = 0;
    if (z == 0) z++;
    System.out.println(z);
    System.exit(0);
    queryFor(z);
  }

  private static void queryFor(int x) {}
}
