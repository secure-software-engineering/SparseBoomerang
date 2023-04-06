package target.aliasing.PointerBench.cornerCases;

import target.aliasing.PointerBench.benchmark.objects.A;

/*
 * @testcase FlowSensitivity1
 *
 * @version 1.0
 *
 * @author Johannes Späth, Nguyen Quang Do Lisa (Secure Software Engineering Group, Fraunhofer
 * Institute SIT)
 *
 * @description Is the analysis flow-sensitive?
 */
public class FlowSensitivity1 {

  public static void main(String[] args) {

    A a = new A();

    A b = new A();
    A b_q1 = b;
    //    Benchmark.test("b",
    //        "{allocId:1, mayAlias:[b], notMayAlias:[a], mustAlias:[b], notMustAlias:[a]}");

    b = a;
  }
}
