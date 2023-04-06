package target.aliasing.PointerBench.generalJava;

import target.aliasing.PointerBench.benchmark.internal.Benchmark;
import target.aliasing.PointerBench.benchmark.objects.A;
import target.aliasing.PointerBench.benchmark.objects.B;

/*
 * @testcase Null1
 *
 * @version 1.0
 *
 * @author Johannes Späth, Nguyen Quang Do Lisa (Secure Software Engineering Group, Fraunhofer
 * Institute SIT)
 *
 * @description Direct alias to null
 */
public class Null1 {

  public static void main(String[] args) {

    // No allocation site
    A h = new A();
    B a = h.getH();
    B b = a;
    B b_q1 = b;
    //    Benchmark.test("b",
    //        "{NULLALLOC, mayAlias:[], notMayAlias:[b,a], mustAlias:[b,a], notMustAlias:[i]}");
    Benchmark.use(b);
  }
}
