package target.aliasing.PointerBench.generalJava;

import target.aliasing.PointerBench.benchmark.objects.A;

/*
 * @testcase StaticVariables1
 *
 * @version 1.0
 *
 * @author Johannes Späth, Nguyen Quang Do Lisa (Secure Software Engineering Group, Fraunhofer
 * Institute SIT)
 *
 * @description Alias to a static variable, allocation site at the static variable site
 */
public class StaticVariables1 {

  private static A a;

  public static void main(String[] args) {

    a = new A();
    A b = a;
    A c = a;
    A b_q1 = b;
    //    Benchmark.test("b",
    //        "{allocId:1, mayAlias:[b,c], notMayAlias:[], mustAlias:[b,c], notMustAlias:[]}");
  }
}
