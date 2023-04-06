package test.aliasing.pointerbench;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import target.aliasing.PointerBench.cornerCases.*;
import test.aliasing.AliasingTestSetUp;

public class CornerCasesTest extends AliasingTestSetUp {

  private static Logger log = LoggerFactory.getLogger(CornerCasesTest.class);

  @Test
  public void accessPath1() {
    String queryLHS = "a.f";
    String targetClass = AccessPath1.class.getName();
    runAnalyses(queryLHS, targetClass, null);
  }

  @Test
  public void contextSensitivity1() {
    String queryLHS = "b_q1";
    String targetClass = ContextSensitivity1.class.getName();
    String targetMethod = "callee";
    runAnalyses(queryLHS, targetClass, targetMethod);
  }

  @Test
  public void contextSensitivity2() {
    String queryLHS = "b_q1";
    String targetClass = ContextSensitivity2.class.getName();
    String targetMethod = "callee";
    runAnalyses(queryLHS, targetClass, targetMethod);
  }

  @Test
  public void contextSensitivity3() {
    String queryLHS = "b_q1";
    String targetClass = ContextSensitivity3.class.getName();
    String targetMethod = "callee";
    runAnalyses(queryLHS, targetClass, targetMethod);
  }

  @Test
  public void fieldSensitivity1() {
    String queryLHS = "d_q1";
    String targetClass = FieldSensitivity1.class.getName();
    runAnalyses(queryLHS, targetClass, null);
  }

  @Test
  public void fieldSensitivity2() {
    String queryLHS = "d_q1";
    String targetClass = FieldSensitivity2.class.getName();
    String targetMethod = "test";
    runAnalyses(queryLHS, targetClass, targetMethod);
  }

  @Test
  public void flowSensitivity1() {
    String queryLHS = "b_q1";
    String targetClass = FlowSensitivity1.class.getName();
    runAnalyses(queryLHS, targetClass, null, false);
  }

  @Test
  public void objectSensitivity1() {
    String queryLHS = "b4_q1";
    String targetClass = ObjectSensitivity1.class.getName();
    this.FalsePositiveInDefaultBoomerang = true;
    runAnalyses(queryLHS, targetClass, null);
    this.FalsePositiveInDefaultBoomerang = false;
  }

  @Test
  public void objectSensitivity2() {
    String queryLHS = "b4_q1";
    String targetClass = ObjectSensitivity2.class.getName();
    runAnalyses(queryLHS, targetClass, null);
  }

  @Test
  public void strongUpdate1() {
    String queryLHS = "x_q1";
    String targetClass = StrongUpdate1.class.getName();
    runAnalyses(queryLHS, targetClass, null);
  }

  @Test
  public void strongUpdate2() {
    String queryLHS = "y_q1";
    String targetClass = StrongUpdate2.class.getName();
    runAnalyses(queryLHS, targetClass, null);
  }
}
