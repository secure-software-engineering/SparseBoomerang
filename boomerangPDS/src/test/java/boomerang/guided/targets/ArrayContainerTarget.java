package boomerang.guided.targets;

public class ArrayContainerTarget {

  public static void main(String... args) {
    String[] y = new String[2];
    y[1] = hello();
    y[2] = world();
    y.toString();
  }

  public static String world() {
    return "world";
  }

  public static String hello() {
    return "hello";
  }
}
