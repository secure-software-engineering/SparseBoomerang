package target.aliasing.PointerBench.collections;

import java.util.ArrayList;
import target.aliasing.PointerBench.benchmark.objects.A;

/*
 * @testcase List1
 *
 * @version 1.0
 *
 * @author Johannes Späth, Nguyen Quang Do Lisa (Secure Software Engineering Group, Fraunhofer
 * Institute SIT)
 *
 * @description ArrayList
 */
public class List1 {

  public static void main(String[] args) {

    ArrayList<A> list = new ArrayList<A>();
    A a = new A();

    A b = new A();
    list.add(a);
    list.add(b);
    A c = list.get(1);
    A b_q1 = b;
    //    Benchmark
    //        .test("b",
    //            "{allocId:1, mayAlias:[c,b], notMayAlias:[a,list], mustAlias:[c,b],
    // notMustAlias:[a,list]}");
  }
}
