package boomerang.scene.up;

import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import sootup.core.graph.MutableStmtGraph;
import sootup.core.jimple.basic.StmtPositionInfo;
import sootup.core.jimple.common.stmt.*;
import sootup.core.model.Body;
import sootup.core.transform.BodyInterceptor;
import sootup.core.views.View;

public class BoomerangPreInterceptor implements BodyInterceptor {

  //    public static boolean TRANSFORM_CONSTANTS = true;
  //    public static String UNINITIALIZED_FIELD_TAG_NAME = "UnitializedField";
  //
  //    private int replaceCounter;
  //    private boolean applied;

  @Override
  public void interceptBody(@Nonnull Body.BodyBuilder builder, @Nonnull View view) {
    addNopStmtToMethods(builder);
  }

  private void addNopStmtToMethods(Body.BodyBuilder b) {
    JNopStmt nopStmt = new JNopStmt(StmtPositionInfo.getNoStmtPositionInfo());
    MutableStmtGraph stmtGraph = b.getStmtGraph();
    Stmt startingStmt = stmtGraph.getStartingStmt();
    stmtGraph.insertBefore(startingStmt, nopStmt);
    Set<JIfStmt> ifStmts = Sets.newHashSet();
    for (Stmt u : b.getStmts()) {
      if (u instanceof JIfStmt) {
        // ((IfStmt) u).getTarget();
        ifStmts.add((JIfStmt) u);
      }
    }

    // After all if-stmts we add a nop-stmt to make the analysis
    for (JIfStmt ifStmt : ifStmts) {
      nopStmt = new JNopStmt(StmtPositionInfo.getNoStmtPositionInfo());
      stmtGraph.insertBefore(ifStmt, nopStmt);
      List<Stmt> targets = stmtGraph.successors(ifStmt);
      for (Stmt target : targets) {
        nopStmt = new JNopStmt(StmtPositionInfo.getNoStmtPositionInfo());
        stmtGraph.insertBefore(target, nopStmt);
      }
      // TODO: check if target is nop
      // ifStmt.setTarget(nopStmt);
    }
  }

  //      public static boolean TRANSFORM_CONSTANTS = true;
  //      public static String UNITIALIZED_FIELD_TAG_NAME = "UnitializedField";
  //
  //      private static BoomerangPretransformer instance;
  //      private int replaceCounter;
  //      private boolean applied;

  //      @Override
  //      protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
  //        addNopStmtToMethods(b);
  //      }

  //      public void apply() {
  //        if (applied) return;
  //        ReachableMethods reachableMethods = Scene.v().getReachableMethods();
  //        QueueReader<MethodOrMethodContext> listener = reachableMethods.listener();
  //        while (listener.hasNext()) {
  //          SootMethod method = listener.next().method();
  //          if (method.hasActiveBody()) {
  //            if (method.isConstructor()) {
  //              addNulliefiedFields(method);
  //            }
  //            internalTransform(method.getActiveBody(), "", new HashMap<>());
  //          }
  //        }
  //        applied = true;
  //      }
  //
  //      private static void addNulliefiedFields(SootMethod cons) {
  //        Chain<SootField> fields = cons.getDeclaringClass().getFields();
  //        UnitPatchingChain units = cons.getActiveBody().getUnits();
  //        Set<SootField> fieldsDefinedInMethod = getFieldsDefinedInMethod(cons,
  // Sets.newHashSet());
  //        for (SootField f : fields) {
  //          if (fieldsDefinedInMethod.contains(f)) continue;
  //          if (f.isStatic()) continue;
  //          if (f.isFinal()) continue;
  //          if (f.getType() instanceof RefType) {
  //            JAssignStmt jAssignStmt =
  //                new JAssignStmt(
  //                    new JInstanceFieldRef(cons.getActiveBody().getThisLocal(), f.makeRef()),
  //                    NullConstant.v());
  //
  //            jAssignStmt.addTag(new LineNumberTag(2));
  //            jAssignStmt.addTag(UNITIALIZED_FIELD_TAG);
  //            Unit lastIdentityStmt = findLastIdentityStmt(units);
  //            if (lastIdentityStmt != null) {
  //              units.insertAfter(jAssignStmt, lastIdentityStmt);
  //            } else {
  //              units.addFirst(jAssignStmt);
  //            }
  //          }
  //        }
  //      }
  //
  //      private static Unit findLastIdentityStmt(UnitPatchingChain units) {
  //        for (Unit u : units) {
  //          if (u instanceof IdentityStmt && u instanceof AssignStmt) {
  //            continue;
  //          }
  //          return u;
  //        }
  //        return null;
  //      }
  //
  //      private static Set<SootField> getFieldsDefinedInMethod(SootMethod cons, Set<SootMethod>
  //     visited) {
  //        Set<SootField> res = Sets.newHashSet();
  //        if (!visited.add(cons)) return res;
  //        if (!cons.hasActiveBody()) return res;
  //        for (Unit u : cons.getActiveBody().getUnits()) {
  //          if (u instanceof AssignStmt) {
  //            AssignStmt as = (AssignStmt) u;
  //            Value left = as.getLeftOp();
  //            if (left instanceof InstanceFieldRef) {
  //              InstanceFieldRef ifr = (InstanceFieldRef) left;
  //              res.add(ifr.getField());
  //            }
  //          }
  //          if (u instanceof Stmt) {
  //            Stmt stmt = (Stmt) u;
  //            if (stmt.containsInvokeExpr()) {
  //              if (stmt.getInvokeExpr().getMethod().isConstructor()) {
  //                res.addAll(getFieldsDefinedInMethod(stmt.getInvokeExpr().getMethod(), visited));
  //              }
  //            }
  //          }
  //        }
  //        return res;
  //      }
  //
  //      public boolean isApplied() {
  //        return applied;
  //      }

}
