package target.aliasing.PointerBench.generalJava;

import target.aliasing.PointerBench.benchmark.objects.A;

/*
 * @testcase Exception1
 *
 * @version 1.0
 *
 * @author Johannes Späth, Nguyen Quang Do Lisa (Secure Software Engineering Group, Fraunhofer
 * Institute SIT)
 *
 * @description Alias in exception
 */
public class Exception1 {

  public static void main(String[] args) {

    A a = new A();
    A b = new A();

    try {
      b = a;
      throw new RuntimeException();
    } catch (RuntimeException e) {
      A b_q1 = b;
      dummy(b_q1);
      // Benchmark.test("b",
      //    "{allocId:1, mayAlias:[a,b], notMayAlias:[], mustAlias:[a,b], notMustAlias:[]}");
    }
  }

  private static void dummy(Object o) {}
}
