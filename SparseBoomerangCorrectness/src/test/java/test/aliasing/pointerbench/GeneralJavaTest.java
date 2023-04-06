package test.aliasing.pointerbench;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import target.aliasing.PointerBench.generalJava.*;
import test.aliasing.AliasingTestSetUp;

public class GeneralJavaTest extends AliasingTestSetUp {

  private static Logger log = LoggerFactory.getLogger(GeneralJavaTest.class);

  @Test
  public void exception1() {
    String queryLHS = "b_q1";
    String targetClass = Exception1.class.getName();
    runAnalyses(queryLHS, targetClass, null);
  }

  @Test
  public void interface1() {
    String queryLHS = "c_q1";
    String targetClass = Interface1.class.getName();
    runAnalyses(queryLHS, targetClass, null);
  }

  @Test
  public void null1() {
    String queryLHS = "b_q1";
    String targetClass = Null1.class.getName();
    runAnalyses(queryLHS, targetClass, null);
  }

  @Test
  public void null2() {
    String queryLHS = "x_q1";
    String targetClass = Null2.class.getName();
    runAnalyses(queryLHS, targetClass, null);
  }

  @Test
  public void outerClass1() {
    String queryLHS = "h_q1";
    String targetClass = OuterClass1.class.getName();
    String targetMethod = "test";
    runAnalyses(queryLHS, targetClass, targetMethod);
  }

  @Test
  public void staticVariables1() {
    String queryLHS = "b_q1";
    String targetClass = StaticVariables1.class.getName();
    runAnalyses(queryLHS, targetClass, null);
  }

  @Test
  public void superClasses1() {
    String queryLHS = "h_q1";
    String targetClass = SuperClasses1.class.getName();
    runAnalyses(queryLHS, targetClass, null);
  }
}
