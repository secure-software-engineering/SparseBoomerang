package target.aliasing.PointerBench.basic;

/*
 * @testcase Loops2
 *
 * @version 1.0
 *
 * @author Johannes Späth, Nguyen Quang Do Lisa (Secure Software Engineering Group, Fraunhofer
 * Institute SIT)
 *
 * @description The analysis must support loop constructs. Allocation site in N
 */
public class Loops2 {

  public class N {
    public String value = "";
    public N next;

    public N() {

      next = new N();
    }
  }

  private void test() {

    N node = new N();

    int i = 0;
    while (i < 10) {
      node = node.next;
      i++;
    }

    N o = node.next;
    N p = node.next.next;
    N node_q1 = node;
    //    Benchmark.test("node",
    //        "{allocId:1, mayAlias:[node], notMayAlias:[i,o,p], mustAlias:[node],
    // notMustAlias:[p]},"
    //            + "{allocId:2, mayAlias:[o], notMayAlias:[node], mustAlias:[o],
    // notMustAlias:[p]}");
  }

  public static void main(String[] args) {
    Loops2 l1 = new Loops2();
    l1.test();
  }
}
