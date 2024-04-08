package test.aliasing.pointerbench;

import org.junit.Ignore;
import org.junit.Test;
import target.aliasing.PointerBench.basic.*;
import test.aliasing.AliasingTestSetUp;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class BasicTest extends AliasingTestSetUp {

  @Test
  public void branching1() {
    String queryLHS = "a_q1";
    String targetClass = Branching1.class.getName();
    Set<String> expectedAliases = new HashSet<>(Arrays.asList("$stack5","a","$stack6","a_q1"));
    runAnalyses(queryLHS, targetClass, null,expectedAliases);
  }

  @Test
  public void interprocedural1() {
    String queryLHS = "x_q1";
    String targetClass = Interprocedural1.class.getName();
    Set<String> expectedAliases = new HashSet<>(Arrays.asList("x_q1","a","b","y","$stack6","$stack7","$stack8")); // -$stack6, -$stack7 ?
    runAnalyses(queryLHS, targetClass, null,expectedAliases);
  }

  @Test
  public void interprocedural2() {
    String queryLHS = "y_q1";
    String targetClass = Interprocedural2.class.getName();
    Set<String> expectedAliases = new HashSet<>(Arrays.asList("a","b","x","y_q1","$stack6","$stack7","$stack8")); // -$stack6, -$stack7 ??
    runAnalyses(queryLHS, targetClass, null,expectedAliases);
  }

  @Test
  public void loops1() {
    String queryLHS = "node_q1";
    String targetClass = Loops1.class.getName();
    String targetMetod = "test";
    Set<String> expectedAliases = new HashSet<>(Arrays.asList("node", "node_q1", "$stack7")); // o, p , q ?
    runAnalyses(queryLHS, targetClass, targetMetod, expectedAliases);
  }

  @Test
  public void loops2() {
    String queryLHS = "node_q1";
    String targetClass = Loops2.class.getName();
    String targetMetod = "test";
    Set<String> expectedAliases = new HashSet<>(Arrays.asList("node", "node_q1", "$stack7","p","$stack6","o"));
    runAnalyses(queryLHS, targetClass, targetMetod,expectedAliases);
  }

  @Test
  public void parameter1() {
    String queryLHS = "b_q1";
    String targetClass = Parameter1.class.getName();
    String targetMetod = "test";
    Set<String> expectedAliases = new HashSet<>(Arrays.asList("x"));
    runAnalyses(queryLHS, targetClass, targetMetod,expectedAliases);
  }

  @Test
  public void parameter2() {
    String queryLHS = "b_q1";
    String targetClass = Parameter2.class.getName();
    String targetMetod = "test";
    Set<String> expectedAliases = new HashSet<>(Arrays.asList("x"));
    runAnalyses(queryLHS, targetClass, targetMetod,expectedAliases);
  }

  @Test
  public void recursion1() {
    String queryLHS = "n_q1";
    String targetClass = Recursion1.class.getName();
    String targetMetod = "test";
    Set<String> expectedAliases = new HashSet<>(Arrays.asList("node", "n_q1", "$stack8", "n")); // o,p,q ?
    runAnalyses(queryLHS, targetClass, targetMetod,expectedAliases);
  }

  @Test
  public void returnValue1() {
    String queryLHS = "b_q1";
    String targetClass = ReturnValue1.class.getName();
    Set<String> expectedAliases = new HashSet<>(Arrays.asList("b_q1","b", "$stack4"));
    runAnalyses(queryLHS, targetClass, null,expectedAliases);
  }

  @Test
  public void returnValue2() {
    String queryLHS = "b_q1";
    String targetClass = ReturnValue2.class.getName();
    Set<String> expectedAliases = new HashSet<>(Arrays.asList("b_q1","a", "$stack5"));
    runAnalyses(queryLHS, targetClass, null,expectedAliases);
  }

  @Test
  public void returnValue3() {
    String queryLHS = "x_q1";
    String targetClass = ReturnValue3.class.getName();
    Set<String> expectedAliases = new HashSet<>(Arrays.asList("x_q1","a","b","y", "$stack6"));
    runAnalyses(queryLHS, targetClass, null,expectedAliases);
  }

  @Test
  public void simpleAlias1() {
    String queryLHS = "b_q1";
    String targetClass = SimpleAlias1.class.getName();
    Set<String> expectedAliases = new HashSet<>(Arrays.asList("b_q1", "$stack3"));
    runAnalyses(queryLHS, targetClass, null, expectedAliases);
  }


}
