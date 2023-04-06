package target.aliasing.PointerBench.collections;

import java.util.HashMap;
import target.aliasing.PointerBench.benchmark.internal.Benchmark;
import target.aliasing.PointerBench.benchmark.objects.A;

/*
 * @testcase Map1
 *
 * @version 1.0
 *
 * @author Johannes Späth, Nguyen Quang Do Lisa (Secure Software Engineering Group, Fraunhofer
 * Institute SIT)
 *
 * @description HashMap
 */
public class Map1 {

  public static void main(String[] args) {

    HashMap<String, A> map = new HashMap<String, A>();
    A a = new A();

    A b = new A();
    map.put("first", a);
    map.put("second", b);
    A c = map.get("second");
    A c_q1 = c;
    Benchmark.test(
        "c",
        "{allocId:1, mayAlias:[c,b], notMayAlias:[a,map], mustAlias:[c,b], notMustAlias:[a,map]}");
  }
}
