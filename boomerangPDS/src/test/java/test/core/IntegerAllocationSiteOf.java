package test.core;

import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.scene.AllocVal;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.Statement;
import java.util.Optional;

class IntegerAllocationSiteOf implements ValueOfInterestInUnit {
  public Optional<? extends Query> test(Edge cfgEdge) {
    Statement stmt = cfgEdge.getStart();
    if (stmt.isAssign()) {
      if (stmt.getLeftOp().toString().contains("allocation")) {
        if (stmt.getLeftOp().isLocal() && stmt.getRightOp().isIntConstant()) {
          AllocVal allocVal = new AllocVal(stmt.getLeftOp(), stmt, stmt.getRightOp());
          ForwardQuery forwardQuery = new ForwardQuery(cfgEdge, allocVal);
          return Optional.of(forwardQuery);
        }
      }
    }

    return Optional.empty();
  }
}
