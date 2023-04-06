package target.aliasing.PointerBench.generalJava;

import target.aliasing.PointerBench.benchmark.objects.A;

/*
 * @testcase OuterClass1
 *
 * @version 1.0
 *
 * @author Johannes Späth, Nguyen Quang Do Lisa (Secure Software Engineering Group, Fraunhofer
 * Institute SIT)
 *
 * @description Alias from method in inner class
 */
public class OuterClass1 {

  public OuterClass1() {}

  public class InnerClass {
    private A a;

    public InnerClass(A a) {
      this.a = a;
    }

    public void alias(A x) {
      this.a = x;
    }
  }

  private void test() {

    A a = new A();
    A b = new A();

    InnerClass i = new InnerClass(a);
    i.alias(b);
    A h = i.a;
    A h_q1 = h;
    //    Benchmark.test("h",
    //        "{allocId:1, mayAlias:[b,h], notMayAlias:[i,a], mustAlias:[b,a], notMustAlias:[i]}");
  }

  private static void main(String[] args) {
    OuterClass1 oc1 = new OuterClass1();
    oc1.test();
  }
}
