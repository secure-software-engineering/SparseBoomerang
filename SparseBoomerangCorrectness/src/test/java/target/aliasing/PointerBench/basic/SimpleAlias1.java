package target.aliasing.PointerBench.basic;

import target.aliasing.PointerBench.benchmark.objects.A;

/*
 * @testcase SimpleAlias1
 *
 * @version 1.0
 *
 * @author Johannes Späth, Nguyen Quang Do Lisa (Secure Software Engineering Group, Fraunhofer
 * Institute SIT)
 *
 * @description Direct alias
 */
public class SimpleAlias1 {

  public static void main(String[] args) {

    A a = new A();

    A b_q1 = a;
    //    Benchmark.test("b",
    //        "{allocId:1, mayAlias:[a,b], notMayAlias:[], mustAlias:[a,b], notMustAlias:[]}");
  }
}
