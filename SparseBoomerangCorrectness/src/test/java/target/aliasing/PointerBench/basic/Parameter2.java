package target.aliasing.PointerBench.basic;

import target.aliasing.PointerBench.benchmark.objects.A;

/*
 * @testcase ParameterAlias2
 *
 * @version 1.0
 *
 * @author Johannes Späth, Nguyen Quang Do Lisa (Secure Software Engineering Group, Fraunhofer
 * Institute SIT)
 *
 * @description Aliasing through non static method parameter
 */
public class Parameter2 {

  public Parameter2() {}

  public void test(A x) {
    A b_q1 = x;
    //    Benchmark.test("b",
    //        "{allocId:1, mayAlias:[b,x], notMayAlias:[], mustAlias:[b,x], notMustAlias:[]}");
  }

  public static void main(String[] args) {

    A a = new A();
    Parameter2 p2 = new Parameter2();
    p2.test(a);
  }
}
