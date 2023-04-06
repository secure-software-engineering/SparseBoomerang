package target.aliasing.PointerBench.cornerCases;

import target.aliasing.PointerBench.benchmark.objects.A;

/*
 * @testcase AccessPath1
 *
 * @version 1.0
 *
 * @author Johannes Späth, Nguyen Quang Do Lisa (Secure Software Engineering Group, Fraunhofer
 * Institute SIT)
 *
 * @description Query for access paths
 */
public class AccessPath1 {

  public static void main(String[] args) {

    A a = new A();
    A b = new A();

    a.f = b.f;
    //    Benchmark
    //        .test("a.f",
    //            "{allocId:1, mayAlias:[a.f,b.f], notMayAlias:[a,b], mustAlias:[a.f,b.f],
    // notMustAlias:[a,b]}");
  }
}
