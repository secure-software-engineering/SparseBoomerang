package test;

public interface FrameworkTestFactory {

  void initializeWithEntryPoint();

  void analyze();

  default void cleanup() {}
}
