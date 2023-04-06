package target.aliasing.PointerBench.basic;

import target.aliasing.PointerBench.benchmark.objects.A;

/*
 * @testcase ReturnValue1
 *
 * @version 1.0
 *
 * @author Johannes Späth, Nguyen Quang Do Lisa (Secure Software Engineering Group, Fraunhofer
 * Institute SIT)
 *
 * @description Alias to a return value from a static method
 */
public class ReturnValue1 {

  public static A id(A x) {
    return x;
  }

  public static void main(String[] args) {

    A a = new A();
    A b = id(a);
    A b_q1 = b;
    //    Benchmark.test("b",
    //        "{allocId:1, mayAlias:[a,b], notMayAlias:[], mustAlias:[a,b], notMustAlias:[]}");
  }
}
