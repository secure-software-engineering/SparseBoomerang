package target.aliasing.PointerBench.cornerCases;

import target.aliasing.PointerBench.benchmark.objects.A;
import target.aliasing.PointerBench.benchmark.objects.B;

/*
 * @testcase FieldSensitivity1
 *
 * @version 1.0
 *
 * @author Johannes Späth, Nguyen Quang Do Lisa (Secure Software Engineering Group, Fraunhofer
 * Institute SIT)
 *
 * @description Field Sensitivity with static method
 */
public class FieldSensitivity1 {

  private static void assign(A x, A y) {
    y.f = x.f;
  }

  public static void main(String[] args) {

    B b = new B();
    A a = new A(b);
    A c = new A();
    assign(a, c);
    B d = c.f;
    B d_q1 = d;

    //    Benchmark.test("d",
    //        "{allocId:1, mayAlias:[d,b], notMayAlias:[a,c], mustAlias:[d,b],
    // notMustAlias:[a,c]}");

  }
}
