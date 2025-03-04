package boomerang.scope.soot;

import soot.SootMethod;

public class SootTestFactory {

  protected SootMethod sootTestMethod;

  /* FIXME
        protected abstract SceneTransformer createAnalysisTransformer();

  // implementation from: IDEALTestingFacory
    protected SceneTransformer createAnalysisTransformer(SootMethod sootTestMethod) throws ImprecisionException {
      return new SceneTransformer() {
        protected void internalTransform(
                String phaseName, @SuppressWarnings("rawtypes") Map options) {
          BoomerangPretransformer.v().reset();
          BoomerangPretransformer.v().apply();
          callGraph = new SootCallGraph();
          dataFlowScope = SootDataFlowScope.make(Scene.v());
          analyze(JimpleMethod.of(sootTestMethod));
        }
      };
    }


  // TODO: (from AbstractTestFactory)
        protected String getSootClassPath() {
            String userdir = System.getProperty("user.dir");
            String javaHome = System.getProperty("java.home");
            if (javaHome == null || javaHome.equals(""))
                throw new RuntimeException("Could not get property java.home!");

            String sootCp = userdir + "/target/test-classes";
            if (getJavaVersion() < 9) {
                sootCp += File.pathSeparator + javaHome + "/lib/rt.jar";
                sootCp += File.pathSeparator + javaHome + "/lib/jce.jar";
            }
            return sootCp;
        }

        private String getTargetClass() {
            SootClass sootClass = new SootClass("dummyClass");
            Type paramType = ArrayType.v(RefType.v("java.lang.String"), 1);
            SootMethod mainMethod =
                    new SootMethod(
                            "main",
                            Collections.singletonList(paramType),
                            VoidType.v(),
                            Modifier.PUBLIC | Modifier.STATIC);
            sootClass.addMethod(mainMethod);
            JimpleBody body = Jimple.v().newBody(mainMethod);
            mainMethod.setActiveBody(body);
            RefType testCaseType = RefType.v(getTestCaseClassName());
            Local loc = Jimple.v().newLocal("l0", paramType);
            body.getLocals().add(loc);
            body.getUnits().add(Jimple.v().newIdentityStmt(loc, Jimple.v().newParameterRef(paramType, 0)));
            Local allocatedTestObj = Jimple.v().newLocal("dummyObj", testCaseType);
            body.getLocals().add(allocatedTestObj);
            body.getUnits()
                    .add(Jimple.v().newAssignStmt(allocatedTestObj, Jimple.v().newNewExpr(testCaseType)));
            body.getUnits()
                    .add(
                            Jimple.v()
                                    .newInvokeStmt(
                                            Jimple.v().newVirtualInvokeExpr(allocatedTestObj, sootTestMethod.makeRef())));
            body.getUnits().add(Jimple.v().newReturnVoidStmt());

            Scene.v().addClass(sootClass);
            body.validate();
            return sootClass.toString();
        }


    */

}
