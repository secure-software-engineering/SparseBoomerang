package target.aliasing.PointerBench.cornerCases;

import target.aliasing.PointerBench.benchmark.objects.A;
import target.aliasing.PointerBench.benchmark.objects.B;

/*
 * @testcase StrongUpdate1
 *
 * @version 1.0
 *
 * @author Johannes Späth, Nguyen Quang Do Lisa (Secure Software Engineering Group, Fraunhofer
 * Institute SIT)
 *
 * @description Indirect alias of a.f and b.f through alias of a and b
 */
public class StrongUpdate2 {

  public static void main(String[] args) {

    A a = new A();
    A b = a;
    B x = b.f;

    a.f = new B();
    B y = b.f;
    B y_q1 = y;
    //    Benchmark.test("y",
    //        "{allocId:1, mayAlias:[y], notMayAlias:[x], mustAlias:[y], notMustAlias:[x]}");
  }
}
