package sparse.eval;

import sparse.SparsificationStrategy;

public class PropagationCounter {
  private SparsificationStrategy strategy;
  private long forwardPropagation = 0;
  private long backwardPropagation = 0;

  public PropagationCounter(SparsificationStrategy strategy) {
    this.strategy = strategy;
  }

  public void countForward() {
    forwardPropagation++;
  }

  public void countBackward() {
    backwardPropagation++;
  }

  public long getForwardPropagation() {
    return forwardPropagation;
  }

  public long getBackwardPropagation() {
    return backwardPropagation;
  }

  public SparsificationStrategy getStrategy() {
    return strategy;
  }
}
