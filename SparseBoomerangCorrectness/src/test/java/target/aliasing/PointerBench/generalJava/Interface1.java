package target.aliasing.PointerBench.generalJava;

import target.aliasing.PointerBench.benchmark.internal.Benchmark;
import target.aliasing.PointerBench.benchmark.objects.A;
import target.aliasing.PointerBench.benchmark.objects.G;
import target.aliasing.PointerBench.benchmark.objects.H;

/*
 * @testcase Interface1
 *
 * @version 1.0
 *
 * @author Johannes Späth, Nguyen Quang Do Lisa (Secure Software Engineering Group, Fraunhofer
 * Institute SIT)
 *
 * @description Alias from method in interface
 */
public class Interface1 {

  public static void main(String[] args) {

    A a = new A();

    A b = new A();

    G g = new G();
    H h = new H();
    g.foo(a);
    A c = h.foo(b);
    A c_q1 = c;
    //    Benchmark.test("c",
    //        "{allocId:1, mayAlias:[c,b], notMayAlias:[a,g,h], mustAlias:[c,b],
    // notMustAlias:[a,g,h]}");

    Benchmark.use(c);
  }
}
