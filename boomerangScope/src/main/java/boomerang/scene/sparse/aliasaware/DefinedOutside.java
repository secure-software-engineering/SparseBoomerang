package boomerang.scene.sparse.aliasaware;

import sootup.core.jimple.basic.JimpleComparator;
import sootup.core.jimple.basic.StmtPositionInfo;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.stmt.AbstractStmt;
import sootup.core.jimple.visitor.StmtVisitor;
import sootup.core.util.printer.StmtPrinter;

public class DefinedOutside extends AbstractStmt {

  Value value;

  public DefinedOutside(Value value) {
    super(StmtPositionInfo.createNoStmtPositionInfo());
    this.value = value;
  }

  public Value getValue() {
    return value;
  }

  @Override
  public Object clone() {
    return null;
  }

  @Override
  public boolean fallsThrough() {
    return false;
  }

  @Override
  public boolean branches() {
    return false;
  }

  @Override
  public void toString(StmtPrinter stmtPrinter) {}

  @Override
  public int equivHashCode() {
    return 0;
  }

  @Override
  public boolean equivTo(Object o, JimpleComparator jimpleComparator) {
    return false;
  }

  @Override
  public void accept(StmtVisitor stmtVisitor) {}
}
