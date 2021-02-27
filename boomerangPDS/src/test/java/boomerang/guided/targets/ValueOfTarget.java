package boomerang.guided.targets;

public class ValueOfTarget {

  public static void main(String... args) {
    int z = 3;
    int x = 1;
    foo(x, z);
  }

  private static void foo(int x, int z) {
    Integer.valueOf(z);
    Query.queryFor(x);
  }
}
