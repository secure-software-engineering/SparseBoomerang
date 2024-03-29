package target.aliasing.PointerBench.basic;

import target.aliasing.PointerBench.benchmark.objects.A;
import target.aliasing.PointerBench.benchmark.objects.B;

/*
 * @testcase Method1
 *
 * @version 1.0
 *
 * @author Johannes Späth, Nguyen Quang Do Lisa (Secure Software Engineering Group, Fraunhofer
 * Institute SIT)
 *
 * @description Alias in a static method
 */
public class Interprocedural1 {

  public static void alloc(A x, A y) {
    x.f = y.f;
  }

  public static void main(String[] args) {

    A a = new A();
    A b = new A();

    b.f = new B();
    alloc(a, b);

    B x = a.f;
    B y = b.f;
    B x_q1 = x;
    //    Benchmark.test("x",
    //        "{allocId:1, mayAlias:[x,y], notMayAlias:[a,b], mustAlias:[x,y],
    // notMustAlias:[a,b]}");
  }
}
