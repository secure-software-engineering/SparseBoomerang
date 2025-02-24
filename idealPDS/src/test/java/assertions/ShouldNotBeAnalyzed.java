package assertions;

import boomerang.scope.Statement;
import test.Assertion;

public class ShouldNotBeAnalyzed implements Assertion {

  private final Statement statement;
  private boolean isSatisfied = true;

  public ShouldNotBeAnalyzed(Statement statement) {
    this.statement = statement;
  }

  public Statement getStatement() {
    return statement;
  }

  @Override
  public String toString() {
    return "Method should not be included in analysis: " + statement.toString();
  }

  @Override
  public boolean isUnsound() {
    return isSatisfied;
  }

  @Override
  public boolean isImprecise() {
    return false;
  }

  @Override
  public String getAssertedMessage() {
    return toString();
  }

  public void hasBeenAnalyzed() {
    isSatisfied = false;
  }
}
