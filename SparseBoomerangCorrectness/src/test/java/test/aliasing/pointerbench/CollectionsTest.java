package test.aliasing.pointerbench;

import org.junit.Test;
import target.aliasing.PointerBench.collections.*;
import test.aliasing.AliasingTestSetUp;

public class CollectionsTest extends AliasingTestSetUp {

  @Test
  public void array1() {
    String queryLHS = "c_q1";
    String targetClass = Array1.class.getName();
    runAnalyses(queryLHS, targetClass, null);
  }

  @Test
  public void list1() {
    String queryLHS = "b_q1";
    String targetClass = List1.class.getName();
    runAnalyses(queryLHS, targetClass, null);
  }

  @Test
  public void list2() {
    String queryLHS = "b_q1";
    String targetClass = List2.class.getName();
    runAnalyses(queryLHS, targetClass, null);
  }

  @Test
  public void Map1() {
    String queryLHS = "c_q1";
    String targetClass = Map1.class.getName();
    runAnalyses(queryLHS, targetClass, null);
  }

  @Test
  public void Set1() {
    String queryLHS = "c_q1";
    String targetClass = Set1.class.getName();
    runAnalyses(queryLHS, targetClass, null);
  }
}
