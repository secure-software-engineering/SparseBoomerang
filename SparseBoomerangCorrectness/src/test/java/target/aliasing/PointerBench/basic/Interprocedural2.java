package target.aliasing.PointerBench.basic;

import target.aliasing.PointerBench.benchmark.objects.A;
import target.aliasing.PointerBench.benchmark.objects.B;

/*
 * @testcase Method2
 *
 * @version 1.0
 *
 * @author Johannes Späth, Nguyen Quang Do Lisa (Secure Software Engineering Group, Fraunhofer
 * Institute SIT)
 *
 * @description Alias in a method
 */
public class Interprocedural2 {

  public Interprocedural2() {}

  public void alloc(A x, A y) {
    x.f = y.f;
  }

  public static void main(String[] args) {

    A a = new A();
    A b = new A();

    b.f = new B();
    Interprocedural2 m2 = new Interprocedural2();
    m2.alloc(a, b);

    B x = a.f;
    B y_q1 = b.f;
    //    Benchmark
    //        .test("x",
    //            "{allocId:1, mayAlias:[x,y], notMayAlias:[a,b,m2], mustAlias:[x,y],
    // notMustAlias:[a,b,m2]}");
  }
}
