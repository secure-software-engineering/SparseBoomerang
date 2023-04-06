package target.aliasing.PointerBench.generalJava;

import target.aliasing.PointerBench.benchmark.objects.A;
import target.aliasing.PointerBench.benchmark.objects.B;

/*
 * @testcase Null2
 * @version 1.0
 * @author Johannes Sp�th, Nguyen Quang Do Lisa (Secure Software Engineering Group, Fraunhofer Institute SIT)
 *
 * @description Implicit alias to null
 *
 */
public class Null2 {

  public static void main(String[] args) {

    // No allocation site
    A a = new A();
    A b = a;
    B x = b.h; // a.h is null
    B x_q1 = x;
    //		Benchmark
    //				.test("x",
    //						"{NULLALLOC, mayAlias:[], notMayAlias:[b,a], mustAlias:[b,a], notMustAlias:[i]}");
  }
}
