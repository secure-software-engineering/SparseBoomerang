package boomerang.guided.targets;

public class IntegerCastTarget {

  public static void main(String... args) {
    int x = 1;
    Object y = castToObject(x);
    y.toString();
  }

  public static Object castToObject(Object param) {
    return param;
  }
}
