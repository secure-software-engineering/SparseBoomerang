package boomerang.guided.targets;

public class ContextSensitiveAndLeftUnbalancedTarget2 {

  private String field;

  private ContextSensitiveAndLeftUnbalancedTarget2() {
    field = "bar";
  }

  public void context() {
    byte[] bytes = field.getBytes();
    bytes.toString();
  }

  public static void main(String... args) {
    new ContextSensitiveAndLeftUnbalancedTarget2().context();
  }
}
