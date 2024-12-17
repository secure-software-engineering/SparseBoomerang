package boomerang.framework.sootup;

import java.util.*;
import javax.annotation.Nonnull;
import sootup.core.graph.MutableStmtGraph;
import sootup.core.jimple.Jimple;
import sootup.core.jimple.basic.*;
import sootup.core.jimple.common.constant.ClassConstant;
import sootup.core.jimple.common.constant.Constant;
import sootup.core.jimple.common.expr.AbstractInstanceInvokeExpr;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.jimple.common.expr.JStaticInvokeExpr;
import sootup.core.jimple.common.ref.JArrayRef;
import sootup.core.jimple.common.ref.JInstanceFieldRef;
import sootup.core.jimple.common.ref.JStaticFieldRef;
import sootup.core.jimple.common.stmt.*;
import sootup.core.model.Body;
import sootup.core.transform.BodyInterceptor;
import sootup.core.views.View;

public class BoomerangPreInterceptor implements BodyInterceptor {

  public static boolean TRANSFORM_CONSTANTS = true;
  private static final String LABEL = "varReplacer";
  private int replaceCounter = 0;

  @Override
  public void interceptBody(@Nonnull Body.BodyBuilder bodyBuilder, @Nonnull View view) {
    addNopStatementsToMethod(bodyBuilder);

    if (TRANSFORM_CONSTANTS) {
      transformConstantsAtFieldWrites(bodyBuilder);
    }
  }

  private void addNopStatementsToMethod(Body.BodyBuilder body) {
    // Initial nop statement
    JNopStmt initialNop = new JNopStmt(StmtPositionInfo.getNoStmtPositionInfo());
    MutableStmtGraph stmtGraph = body.getStmtGraph();
    stmtGraph.insertBefore(stmtGraph.getStartingStmt(), initialNop);

    // Collect if-statements
    Collection<JIfStmt> ifStatements = new HashSet<>();
    for (Stmt stmt : body.getStmts()) {
      if (stmt instanceof JIfStmt) {
        JIfStmt ifStmt = (JIfStmt) stmt;

        ifStatements.add(ifStmt);
      }
    }

    // Add nop statement before each if-statement branch
    for (JIfStmt ifStmt : ifStatements) {
      // Branch under if-Statement: Insert after if-statement
      List<Stmt> successors = stmtGraph.successors(ifStmt);
      if (!successors.isEmpty()) {
        Stmt directSuccessor = successors.get(0);

        JNopStmt nopAfterIfStmt = Jimple.newNopStmt(ifStmt.getPositionInfo());
        stmtGraph.insertBefore(directSuccessor, nopAfterIfStmt);
      }

      // Branch for target: For if-statements, there is always exactly one target
      List<Stmt> targets = stmtGraph.getBranchTargetsOf(ifStmt);
      if (targets.size() == 1) {
        Stmt target = targets.get(0);

        JNopStmt nopBeforeTarget = Jimple.newNopStmt(target.getPositionInfo());
        stmtGraph.insertBefore(target, nopBeforeTarget);
        // TODO Check if the target is actually nop
      }
    }
  }

  private void transformConstantsAtFieldWrites(Body.BodyBuilder body) {
    Collection<Stmt> statements = getStatementsWithConstants(body);

    for (Stmt stmt : statements) {
      if (stmt instanceof JAssignStmt) {
        /* Transform simple assignments to two assignment steps:
         * - value = 10;
         * becomes
         * - varReplacer = 10;
         * - value = varReplacer;
         */
        JAssignStmt assignStmt = (JAssignStmt) stmt;

        LValue leftOp = assignStmt.getLeftOp();
        Value rightOp = assignStmt.getRightOp();

        if (isFieldRef(leftOp)
            && rightOp instanceof Constant
            && !(rightOp instanceof ClassConstant)) {
          String label = LABEL + replaceCounter++;
          Local local = Jimple.newLocal(label, rightOp.getType());
          JAssignStmt newAssignStmt = Jimple.newAssignStmt(local, rightOp, stmt.getPositionInfo());

          body.addLocal(local);
          body.getStmtGraph().insertBefore(stmt, newAssignStmt);

          JAssignStmt updatedAssignStmt =
              Jimple.newAssignStmt(leftOp, local, stmt.getPositionInfo());
          body.getStmtGraph().replaceNode(stmt, updatedAssignStmt);
        }
      }

      if (stmt.isInvokableStmt()) {
        /* Extract constant arguments to new assignments
         * - method(10)
         * becomes
         * - varReplacer = 10
         * - method(varReplacer)
         */
        List<Immediate> newArgs = new ArrayList<>();

        InvokableStmt invStmt = stmt.asInvokableStmt();

        AbstractInvokeExpr invokeExpr = invStmt.getInvokeExpr().get();
        for (int i = 0; i < invokeExpr.getArgCount(); i++) {
          Immediate arg = invokeExpr.getArg(i);

          if (arg instanceof Constant && !(arg instanceof ClassConstant)) {
            String label = LABEL + replaceCounter++;
            Local local = Jimple.newLocal(label, arg.getType());
            JAssignStmt newAssignStmt = Jimple.newAssignStmt(local, arg, stmt.getPositionInfo());

            body.addLocal(local);
            body.getStmtGraph().insertBefore(stmt, newAssignStmt);

            newArgs.add(local);
          } else {
            newArgs.add(arg);
          }
        }

        // Update the invoke expression with new arguments
        // TODO: [ms] make use of new ReplaceUseExprVisitor()
        AbstractInvokeExpr newInvokeExpr;
        if (invokeExpr instanceof JStaticInvokeExpr) {
          JStaticInvokeExpr staticInvokeExpr = (JStaticInvokeExpr) invokeExpr;

          newInvokeExpr = staticInvokeExpr.withArgs(newArgs);
        } else if (invokeExpr instanceof AbstractInstanceInvokeExpr) {
          AbstractInstanceInvokeExpr instanceInvokeExpr = (AbstractInstanceInvokeExpr) invokeExpr;

          newInvokeExpr = instanceInvokeExpr.withArgs(newArgs);
        } else {
          // TODO Are there other relevant cases?
          newInvokeExpr = invokeExpr;
        }

        if (stmt instanceof JInvokeStmt) {
          JInvokeStmt invokeStmt = (JInvokeStmt) stmt;
          JInvokeStmt newStmt = invokeStmt.withInvokeExpr(newInvokeExpr);

          body.getStmtGraph().replaceNode(stmt, newStmt);
        } else if (stmt instanceof JAssignStmt) {
          JAssignStmt assignStmt = (JAssignStmt) stmt;
          JAssignStmt newStmt = assignStmt.withRValue(newInvokeExpr);

          body.getStmtGraph().replaceNode(stmt, newStmt);
        }
      }

      if (stmt instanceof JReturnStmt) {
        /* Transform return statements into two statements
         * - return 10
         * becomes
         * - varReplacer = 10
         * - return varReplacer
         */
        JReturnStmt returnStmt = (JReturnStmt) stmt;

        String label = LABEL + replaceCounter++;
        Local local = Jimple.newLocal(label, returnStmt.getOp().getType());
        JAssignStmt assignStmt =
            Jimple.newAssignStmt(local, returnStmt.getOp(), returnStmt.getPositionInfo());

        body.addLocal(local);
        body.getStmtGraph().insertBefore(stmt, assignStmt);

        JReturnStmt newReturnStmt = Jimple.newReturnStmt(local, returnStmt.getPositionInfo());
        body.getStmtGraph().replaceNode(stmt, newReturnStmt);
      }
    }
  }

  private Collection<Stmt> getStatementsWithConstants(Body.BodyBuilder body) {
    Collection<Stmt> result = new HashSet<>();

    for (Stmt stmt : body.getStmts()) {
      // Assign statements: If the right side is a constant
      if (stmt instanceof JAssignStmt) {
        JAssignStmt assignStmt = (JAssignStmt) stmt;

        if (isFieldRef(assignStmt.getLeftOp()) && assignStmt.getRightOp() instanceof Constant) {
          result.add(stmt);
        }
      }

      // Consider arguments of invoke expressions
      if (stmt.isInvokableStmt()) {
        Optional<AbstractInvokeExpr> invokeExpr = stmt.asInvokableStmt().getInvokeExpr();
        if (invokeExpr.isPresent()) {
          for (Value arg : invokeExpr.get().getArgs()) {
            if (arg instanceof Constant) {
              result.add(stmt);
            }
          }
        }
      }

      // Check for constant return values
      if (stmt instanceof JReturnStmt) {
        JReturnStmt returnStmt = (JReturnStmt) stmt;

        if (returnStmt.getOp() instanceof Constant) {
          result.add(stmt);
        }
      }
    }

    return result;
  }

  private boolean isFieldRef(Value value) {
    return value instanceof JInstanceFieldRef
        || value instanceof JStaticFieldRef
        || value instanceof JArrayRef;
  }
}
