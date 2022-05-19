package boomerang.scene.sparse;

import com.google.common.collect.Iterables;
import java.util.*;
import soot.Unit;
import soot.toolkits.graph.DirectedGraph;

public class SparseAliasingUnitGraph implements DirectedGraph<Unit> {

  private SparseAliasingCFG sparseCFG;

  public SparseAliasingUnitGraph(SparseAliasingCFG sparseCFG) {
    this.sparseCFG = sparseCFG;
  }

  @Override
  public List<Unit> getHeads() {
    Unit first = Iterables.getFirst(sparseCFG.getGraph().nodes(), null);
    if (first == null) {
      throw new RuntimeException("No head");
    }
    return Collections.singletonList(first);
  }

  @Override
  public List<Unit> getTails() {
    Unit last = Iterables.getLast(sparseCFG.getGraph().nodes(), null);
    if (last == null) {
      throw new RuntimeException("No tail");
    }
    return Collections.singletonList(last);
  }

  @Override
  public List<Unit> getPredsOf(Unit unit) {
    Set<Unit> predecessors = sparseCFG.getGraph().predecessors(unit);
    return new ArrayList<>(predecessors);
  }

  @Override
  public List<Unit> getSuccsOf(Unit unit) {
    return new ArrayList<>(sparseCFG.getSuccessors(unit));
  }

  @Override
  public int size() {
    return sparseCFG.getGraph().nodes().size();
  }

  @Override
  public Iterator<Unit> iterator() {
    return sparseCFG.getGraph().nodes().iterator();
  }
}
