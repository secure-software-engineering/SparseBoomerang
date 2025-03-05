package boomerang.weights;

import boomerang.scope.ControlFlowGraph.Edge;
import boomerang.scope.Val;
import com.google.common.collect.Lists;

import java.util.LinkedHashSet;
import java.util.List;
import sync.pds.solver.nodes.Node;
import wpds.impl.Weight;


public class PathTrackingWeight extends Weight {

  /**
   * This set keeps track of all statements on a shortest path that use an alias from source to
   * sink.
   */
  private LinkedHashSet<Node<Edge, Val>> shortestPathWitness = new LinkedHashSet<>();



  private PathTrackingWeight(
      LinkedHashSet<Node<Edge, Val>> allStatement) {
    this.shortestPathWitness = allStatement;
  }

  public PathTrackingWeight(Node<Edge, Val> relevantStatement) {
    this.shortestPathWitness.add(relevantStatement);
    LinkedHashSet<Node<Edge, Val>> firstDataFlowPath = new LinkedHashSet<>();
    firstDataFlowPath.add(relevantStatement);
  }



  @Override
  public Weight extendWith(Weight o) {
    if (!(o instanceof PathTrackingWeight))
      throw new RuntimeException("Cannot extend to different types of weight!");
    PathTrackingWeight other = (PathTrackingWeight) o;
    LinkedHashSet<Node<Edge, Val>> newAllStatements = new LinkedHashSet<>();
    newAllStatements.addAll(shortestPathWitness);
    newAllStatements.addAll(other.shortestPathWitness);



    return new PathTrackingWeight(newAllStatements);
  }

  @Override
  public Weight combineWith(Weight o) {
    if (!(o instanceof PathTrackingWeight))
      throw new RuntimeException("Cannot extend to different types of weight!");
    PathTrackingWeight other = (PathTrackingWeight) o;


    if (shortestPathWitness.size() > other.shortestPathWitness.size()) {
      return new PathTrackingWeight(
          new LinkedHashSet<>(other.shortestPathWitness));
    }

    return new PathTrackingWeight(
        new LinkedHashSet<>(this.shortestPathWitness));
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((shortestPathWitness == null) ? 0 : shortestPathWitness.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    PathTrackingWeight other = (PathTrackingWeight) obj;
    if (shortestPathWitness == null) {
      if (other.shortestPathWitness != null) return false;
    } else if (!shortestPathWitness.equals(other.shortestPathWitness)) return false;

    return false;
  }

  @Override
  public String toString() {
    return "\nAll statements: " + shortestPathWitness;
  }

  public List<Node<Edge, Val>> getShortestPathWitness() {
    return Lists.newArrayList(shortestPathWitness);
  }

}
