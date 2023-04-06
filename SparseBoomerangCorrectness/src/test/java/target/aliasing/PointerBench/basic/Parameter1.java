package target.aliasing.PointerBench.basic;

import target.aliasing.PointerBench.benchmark.objects.A;

/*
 * @testcase ParameterAlias1
 *
 * @version 1.0
 *
 * @author Johannes Späth, Nguyen Quang Do Lisa (Secure Software Engineering Group, Fraunhofer
 * Institute SIT)
 *
 * @description Aliasing through static method parameter
 */
public class Parameter1 {

  public static void test(A x) {
    A b = x;
    A b_q1 = b;
    //    Benchmark.test("b",
    //        "{allocId:1, mayAlias:[b,x], notMayAlias:[], mustAlias:[b,x], notMustAlias:[]}");
  }

  public static void main(String[] args) {

    A a = new A();
    test(a);
  }
}
