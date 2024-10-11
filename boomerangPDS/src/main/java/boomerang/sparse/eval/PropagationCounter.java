package boomerang.sparse.eval;

import boomerang.sparse.SparsificationStrategy;

public class PropagationCounter {
  private SparsificationStrategy strategy;
  private long forwardPropagation = 0;
  private long backwardPropagation = 0;

  private static PropagationCounter NONE_INSTANCE;
  private static PropagationCounter TYPE_BASED_INSTANCE;
  private static PropagationCounter ALIAS_AWARE_INSTANCE;

  public static PropagationCounter getInstance(SparsificationStrategy strategy) {
    /*
    TODO:[ms] refactor mapping

    switch (strategy) {
      case NONE:
        if (NONE_INSTANCE == null) {
          NONE_INSTANCE = new PropagationCounter(strategy);
        }
        return NONE_INSTANCE;
      case TYPE_BASED:
        if (TYPE_BASED_INSTANCE == null) {
          TYPE_BASED_INSTANCE = new PropagationCounter(strategy);
        }
        return TYPE_BASED_INSTANCE;
      case ALIAS_AWARE:
        if (ALIAS_AWARE_INSTANCE == null) {
          ALIAS_AWARE_INSTANCE = new PropagationCounter(strategy);
        }
        return ALIAS_AWARE_INSTANCE;
      default:
        throw new RuntimeException("No such strategy");
    }

     */
    throw new RuntimeException("No such strategy");
  }

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
