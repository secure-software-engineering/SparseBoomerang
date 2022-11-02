package target.aliasing.PointerBench.collections;

import target.aliasing.PointerBench.benchmark.objects.A;

import java.util.LinkedList;

/*
 * @testcase List2
 * 
 * @version 1.0
 * 
 * @author Johannes Späth, Nguyen Quang Do Lisa (Secure Software Engineering Group, Fraunhofer
 * Institute SIT)
 * 
 * @description LinkedList
 */
public class List2 {

  public static void main(String[] args) {

    LinkedList<A> list = new LinkedList<A>();
    A a = new A();

    A b = new A();
    list.add(a);
    list.add(b);
    A c = list.get(1);
    A b_q1 = b;
//    Benchmark
//        .test("b",
//            "{allocId:1, mayAlias:[c,b], notMayAlias:[a,list], mustAlias:[c,b], notMustAlias:[a,list]}");
  }
}
