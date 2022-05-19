package boomerang.scene.jimple;

import boomerang.scene.ControlFlowGraph;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import soot.SootMethod;

public class JimpleSparseMethod extends JimpleMethod {

  private JimpleStatement stmt;
  private JimpleVal value;

  protected static Interner<JimpleSparseMethod> INTERNAL_POOL = Interners.newWeakInterner();
  protected ControlFlowGraph cfg;

  private JimpleSparseMethod(SootMethod m) {
    super(m);
    if (!m.hasActiveBody()) {
      throw new RuntimeException("Building a Jimple method without active body present");
    }
  }

  public void setStmt(JimpleStatement stmt) {
    this.stmt = stmt;
  }

  public void setValue(JimpleVal value) {
    this.value = value;
  }

  public static JimpleSparseMethod of(SootMethod m) {
    return INTERNAL_POOL.intern(new JimpleSparseMethod(m));
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((getDelegate() == null) ? 0 : getDelegate().hashCode());
    result = prime * result + ((stmt == null) ? 0 : stmt.getDelegate().hashCode());
    result = prime * result + ((value == null) ? 0 : value.getDelegate().hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    JimpleSparseMethod other = (JimpleSparseMethod) obj;
    if (getDelegate() == null) {
      if (other.getDelegate() != null) return false;
    } else if (!getDelegate().equals(other.getDelegate())) return false;

    if (stmt == null) {
      if (other.stmt != null) return false;
    } else if (!stmt.equals(other.stmt)) return false;

    if (value == null) {
      if (other.value != null) return false;
    } else if (!value.equals(other.value)) return false;

    return true;
  }

  @Override
  public ControlFlowGraph getControlFlowGraph() {
    if (this.stmt == null) {
      throw new RuntimeException("Stmt not set");
    }
    if (this.value == null) {
      throw new RuntimeException("Value not set");
    }
    if (this.cfg == null) {
      cfg = new JimpleSparseControlFlowGraph(this, stmt, value);
    }
    return cfg;
  }
}
