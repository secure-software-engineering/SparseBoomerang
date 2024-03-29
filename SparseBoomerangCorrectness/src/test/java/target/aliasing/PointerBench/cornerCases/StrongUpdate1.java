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
public class StrongUpdate1 {

  public static void main(String[] args) {

    A a = new A();
    A b = a;

    a.f = new B();
    B y = a.f;
    B x = b.f;
    B x_q1 = x;
    //    Benchmark.test("x",
    //        "{allocId:1, mayAlias:[x,y], notMayAlias:[a,b], mustAlias:[x,y],
    // notMustAlias:[a,b]}");
  }
}
