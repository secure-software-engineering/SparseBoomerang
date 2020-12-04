package boomerang.guided;

import boomerang.BackwardQuery;
import boomerang.Boomerang;
import boomerang.BoomerangOptions;
import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.QueryGraph;
import boomerang.guided.Specification.QueryDirection;
import boomerang.results.AbstractBoomerangResults.Context;
import boomerang.results.BackwardBoomerangResults;
import boomerang.results.ForwardBoomerangResults;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.DataFlowScope;
import boomerang.scene.SootDataFlowScope;
import boomerang.scene.Val;
import boomerang.scene.jimple.SootCallGraph;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import soot.Scene;
import sync.pds.solver.nodes.Node;
import wpds.impl.Weight.NoWeight;

public class DemandDrivenGuidedAnalysis {

  private final BoomerangOptions customBoomerangOptions;
  private final IDemandDrivenGuidedManager spec;
  private final DataFlowScope scope;
  private final SootCallGraph callGraph;
  private final LinkedList<QueryWithContext> queryQueue = Lists.newLinkedList();
  private final Set<Query> visited = Sets.newHashSet();

  public DemandDrivenGuidedAnalysis(
      IDemandDrivenGuidedManager specification, BoomerangOptions options) {
    spec = specification;
    callGraph = new SootCallGraph();
    scope = SootDataFlowScope.make(Scene.v());
    if (!options.allowMultipleQueries()) {
      throw new RuntimeException(
          "Boomerang options allowMultipleQueries is set to false. Please enable it.");
    }
    customBoomerangOptions = options;
  }

  /**
   * The query graph takes as input an initial query from which all follow up computations are
   * computed. Based on the specification provided. It returns the QueryGraph which is a graph whose
   * nodes are Boomerang Queries, there is an edge between the queries if there node A triggered a
   * subsequent query B.
   *
   * @param query The initial query to start the analysis from.
   * @return a query graph containing all queries triggered.
   */
  public QueryGraph<NoWeight> run(Query query) {
    queryQueue.add(new QueryWithContext(query));
    Boomerang bSolver = new Boomerang(callGraph, scope, customBoomerangOptions);
    while (!queryQueue.isEmpty()) {
      QueryWithContext pop = queryQueue.pop();
      if (pop.query instanceof ForwardQuery) {
        ForwardBoomerangResults<NoWeight> results;
        ForwardQuery currentQuery = (ForwardQuery) pop.query;
        if (pop.parentQuery == null) {
          results = bSolver.solve(currentQuery);
        } else {
          results = bSolver.solveUnderScope(currentQuery, pop.triggeringNode, pop.parentQuery);
        }

        Table<Edge, Val, NoWeight> forwardResults =
            results.asStatementValWeightTable((ForwardQuery) pop.query);
        // Any ForwardQuery may trigger additional ForwardQuery under its own scope.
        triggerNewBackwardQueries(forwardResults, currentQuery, QueryDirection.FORWARD);
      } else {
        BackwardBoomerangResults<NoWeight> results;
        if (pop.parentQuery == null) {
          results = bSolver.solve((BackwardQuery) pop.query);
        } else {
          results =
              bSolver.solveUnderScope(
                  (BackwardQuery) pop.query, pop.triggeringNode, pop.parentQuery);
        }
        Table<Edge, Val, NoWeight> backwardResults =
            bSolver.getBackwardSolvers().get(query).asStatementValWeightTable();

        triggerNewBackwardQueries(backwardResults, pop.query, QueryDirection.BACKWARD);
        Map<ForwardQuery, Context> allocationSites = results.getAllocationSites();

        for (Entry<ForwardQuery, Context> entry : allocationSites.entrySet()) {
          triggerNewBackwardQueries(
              results.asStatementValWeightTable(entry.getKey()),
              entry.getKey(),
              QueryDirection.FORWARD);
        }
      }
    }

    QueryGraph<NoWeight> queryGraph = bSolver.getQueryGraph();
    bSolver.unregisterAllListeners();
    return queryGraph;
  }

  private void triggerNewBackwardQueries(
      Table<Edge, Val, NoWeight> backwardResults, Query lastQuery, QueryDirection direction) {
    for (Cell<Edge, Val, NoWeight> cell : backwardResults.cellSet()) {
      Edge triggeringEdge = cell.getRowKey();
      Val fact = cell.getColumnKey();
      Collection<Query> queries;
      if (direction == QueryDirection.FORWARD) {
        queries =
            spec.onForwardFlow((ForwardQuery) lastQuery, cell.getRowKey(), cell.getColumnKey());
      } else {
        queries =
            spec.onBackwardFlow((BackwardQuery) lastQuery, cell.getRowKey(), cell.getColumnKey());
      }
      for (Query q : queries) {
        addToQueue(new QueryWithContext(q, new Node<>(triggeringEdge, fact), lastQuery));
      }
    }
  }

  private void addToQueue(QueryWithContext nextQuery) {
    if (visited.add(nextQuery.query)) {
      queryQueue.add(nextQuery);
    }
  }

  private static class QueryWithContext {
    private QueryWithContext(Query query) {
      this.query = query;
    }

    private QueryWithContext(Query query, Node<Edge, Val> triggeringNode, Query parentQuery) {
      this.query = query;
      this.parentQuery = parentQuery;
      this.triggeringNode = triggeringNode;
    }

    Query query;
    Query parentQuery;
    Node<Edge, Val> triggeringNode;
  }
}
