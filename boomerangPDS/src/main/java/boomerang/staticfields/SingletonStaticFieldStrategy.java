package boomerang.staticfields;

import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.Field;
import boomerang.scene.Statement;
import boomerang.scene.StaticFieldVal;
import boomerang.scene.Val;
import boomerang.solver.AbstractBoomerangSolver;
import com.google.common.collect.Multimap;
import java.util.Set;
import sync.pds.solver.nodes.Node;
import wpds.impl.Weight;
import wpds.interfaces.State;

public class SingletonStaticFieldStrategy<W extends Weight> implements StaticFieldStrategy<W> {

  private Multimap<Field, Statement> fieldStoreStatements;
  private Multimap<Field, Statement> fieldLoadStatements;
  private AbstractBoomerangSolver solver;

  public SingletonStaticFieldStrategy(
      AbstractBoomerangSolver solver,
      Multimap<Field, Statement> fieldLoadStatements,
      Multimap<Field, Statement> fieldStoreStatements) {
    this.solver = solver;
    this.fieldStoreStatements = fieldStoreStatements;
    this.fieldLoadStatements = fieldLoadStatements;
  }

  @Override
  public void handleForward(
      Edge storeStmt, Val storedVal, StaticFieldVal staticVal, Set<State> out) {
    for (Statement matchingStore : fieldLoadStatements.get(staticVal.field())) {
      if (matchingStore.isAssign()) {
        for (Statement succ :
            matchingStore.getMethod().getControlFlowGraph().getSuccsOf(matchingStore)) {
          solver.processNormal(
              new Node<>(storeStmt, storedVal),
              new Node<>(new Edge(matchingStore, succ), matchingStore.getLeftOp()));
        }
      }
    }
  }

  @Override
  public void handleBackward(
      Edge loadStatement, Val loadedVal, StaticFieldVal staticVal, Set<State> out) {
    for (Statement matchingStore : fieldStoreStatements.get(staticVal.field())) {
      for (Statement pred :
          matchingStore.getMethod().getControlFlowGraph().getPredsOf(matchingStore)) {
        solver.processNormal(
            new Node<>(loadStatement, loadedVal),
            new Node<>(new Edge(pred, matchingStore), matchingStore.getRightOp()));
      }
    }
  }
}
