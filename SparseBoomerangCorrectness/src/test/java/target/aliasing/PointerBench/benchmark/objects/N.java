package target.aliasing.PointerBench.benchmark.objects;

public class N {
  public String value = "";
  public N next;

  public N() {

    next = new N();
  }
}
