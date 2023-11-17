package test.aliasing;

import static org.junit.Assert.assertTrue;

import aliasing.SparseAliasManager;
import boomerang.scene.sparse.SparseCFGCache;
import boomerang.scene.up.BoomerangPreInterceptor;
import boomerang.scene.up.SootUpClient;
import boomerang.util.AccessPath;
import com.google.common.base.Predicate;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sootup.callgraph.CallGraph;
import sootup.callgraph.RapidTypeAnalysisAlgorithm;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.inputlocation.ClassLoadingOptions;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.ref.JInstanceFieldRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.SootMethod;
import sootup.core.signatures.MethodSignature;
import sootup.core.transform.BodyInterceptor;
import sootup.java.bytecode.inputlocation.PathBasedAnalysisInputLocation;
import sootup.java.bytecode.interceptors.TypeAssigner;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.JavaProject;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.language.JavaLanguage;
import sootup.java.core.types.JavaClassType;

public class AliasingTestSetUp {

  private static Logger log = LoggerFactory.getLogger(AliasingTestSetUp.class);

  Set<AccessPath> aliases = null;

  protected boolean FalsePositiveInDefaultBoomerang;

  private CallGraph callGraph;
  private List<MethodSignature> entryPoints;
  private SootUpClient client;

  public Set<AccessPath> executeStaticAnalysis(
      String targetClassName,
      String targetMethod,
      String queryLHS,
      SparseCFGCache.SparsificationStrategy sparsificationStrategy,
      boolean ignoreAfterQuery) {
    setupSoot(targetClassName);
    JavaClassType classType = client.getIdentifierFactory().getClassType(targetClassName);
    JavaSootClass javaSootClass = client.getView().getClass(classType).get();
    JavaSootMethod entryMethod = getEntryPointMethod(classType, targetMethod);
    entryPoints = Collections.singletonList(entryMethod.getSignature());
    callGraph = new RapidTypeAnalysisAlgorithm(client.getView()).initialize(entryPoints);
    aliases = getAliases(entryMethod, queryLHS, sparsificationStrategy, ignoreAfterQuery);
    // registerSootTransformers(queryLHS, sparsificationStrategy, targetMethod, ignoreAfterQuery);
    // executeSootTransformers();
    return aliases;
  }

  protected void setupSoot(String targetTestClassName) {
    String userdir = System.getProperty("user.dir");
    String testClasses = userdir + File.separator + "target" + File.separator + "test-classes";

    String rtJar = "lib" + File.separator + "rt.jar";

    Path pathToTestClasses = Paths.get(testClasses);
    Path pathToRTJar = Paths.get(rtJar);
    AnalysisInputLocation<JavaSootClass> testClassesLocation =
        PathBasedAnalysisInputLocation.create(pathToTestClasses, null);
    AnalysisInputLocation<JavaSootClass> rtJarLocation =
        PathBasedAnalysisInputLocation.create(pathToRTJar, null);

    // Create a new JavaProject based on the input location
    JavaProject project =
        JavaProject.builder(new JavaLanguage(8))
            .addInputLocation(testClassesLocation)
            .addInputLocation(rtJarLocation)
            .build();

    ClassLoadingOptions clo =
        new ClassLoadingOptions() {
          @Override
          public List<BodyInterceptor> getBodyInterceptors() {
            List<BodyInterceptor> interceptors = new ArrayList<>();
            interceptors.add(new TypeAssigner());
            interceptors.add(new BoomerangPreInterceptor());
            return interceptors;
          }
        };
    this.client =
        SootUpClient.getInstance(
            project.createView(analysisInputLocation -> clo), JavaIdentifierFactory.getInstance());

    /**
     * Options.v().set_soot_classpath(sootCp);
     *
     * <p>// We want to perform a whole program, i.e. an interprocedural analysis. // We construct a
     * basic CHA call graph for the program Options.v().set_whole_program(true);
     * Options.v().setPhaseOption("cg.spark", "on"); Options.v().setPhaseOption("cg",
     * "all-reachable:true");
     *
     * <p>Options.v().set_no_bodies_for_excluded(true); Options.v().set_allow_phantom_refs(true);
     * Options.v().setPhaseOption("jb", "use-original-names:true");
     * Options.v().set_prepend_classpath(false);
     *
     * <p>Scene.v().addBasicClass("java.lang.StringBuilder"); SootClass c =
     * Scene.v().forceResolve(targetTestClassName, SootClass.BODIES); if (c != null) {
     * c.setApplicationClass(); } Scene.v().loadNecessaryClasses();*
     */
  }

  public Set<AccessPath> getAliases(
      SootMethod method,
      String queryLHS,
      SparseCFGCache.SparsificationStrategy sparsificationStrategy,
      boolean ignoreAfterQuery) {
    queryLHS = "$" + queryLHS;
    String[] split = queryLHS.split("\\.");
    Optional<Stmt> unitOp;
    if (split.length > 1) {
      unitOp =
          method.getBody().getStmts().stream()
              .filter(e -> e.toString().startsWith(split[0]) && e.toString().contains(split[1]))
              .findFirst();
    } else {
      unitOp =
          method.getBody().getStmts().stream()
              .filter(e -> e.toString().startsWith(split[0]))
              .findFirst();
    }

    if (unitOp.isPresent()) {
      Stmt stmt = unitOp.get();
      if (stmt instanceof JAssignStmt) {
        JAssignStmt assignStmt = (JAssignStmt) stmt;
        Value leftOp = assignStmt.getLeftOp();
        if (leftOp instanceof JInstanceFieldRef) {
          // get base
          leftOp = ((JInstanceFieldRef) leftOp).getBase();
        }
        SparseAliasManager sparseAliasManager =
            SparseAliasManager.getInstance(
                client.getView(), callGraph, entryPoints, sparsificationStrategy, ignoreAfterQuery);
        return sparseAliasManager.getAliases(assignStmt, method, leftOp);
      }
    }
    throw new RuntimeException(
        "Query Variable not found. Does variable:"
            + queryLHS
            + " exist in the method:"
            + method.getName());
  }

  //  protected Transformer createAnalysisTransformer(
  //      String queryLHS,
  //      SparseCFGCache.SparsificationStrategy sparsificationStrategy,
  //      String targetMethod,
  //      boolean ignoreAfterQuery) {
  //    return new SceneTransformer() {
  //      @Override
  //      protected void internalTransform(String phaseName, Map<String, String> options) {
  //        aliases =
  //            getAliases(
  //                getEntryPointMethod(targetMethod),
  //                queryLHS,
  //                sparsificationStrategy,
  //                ignoreAfterQuery);
  //      }
  //    };
  //  }

  protected JavaSootMethod getEntryPointMethod(JavaClassType classType, String targetMethod) {
    for (JavaSootMethod m : client.getView().getClass(classType).get().getMethods()) {
      if (!m.hasBody()) {
        continue;
      }
      if (targetMethod != null && m.getName().equals(targetMethod)) {
        return m;
      }
      if (m.getName().equals("entryPoint")
          || m.toString().contains("void main(java.lang.String[])")) {
        return m;
      }
    }

    throw new IllegalArgumentException("Method does not exist in view!");
  }

  //  protected void registerSootTransformers(
  //      String queryLHS,
  //      SparseCFGCache.SparsificationStrategy sparsificationStrategy,
  //      String targetMethod,
  //      boolean ignoreAfterQuery) {
  //    Transform transform =
  //        new Transform(
  //            "wjtp.ifds",
  //            createAnalysisTransformer(
  //                queryLHS, sparsificationStrategy, targetMethod, ignoreAfterQuery));
  //    PackManager.v().getPack("wjtp").add(transform);
  //  }

  //  protected void executeSootTransformers() {
  //    // Apply all necessary packs of soot. This will execute the respective Transformer
  //    PackManager.v().getPack("cg").apply();
  //    // Must have for Boomerang
  //    BoomerangPretransformer.v().reset();
  //    BoomerangPretransformer.v().apply();
  //    PackManager.v().getPack("wjtp").apply();
  //  }

  protected void runAnalyses(String queryLHS, String targetClass, String targetMethod) {
    Set<AccessPath> nonSparseAliases =
        getAliasesFirst(
            targetClass, queryLHS, targetMethod, SparseCFGCache.SparsificationStrategy.NONE, true);
    //    Set<AccessPath> typeBasedSparseAliases =
    //        getAliasesFirst(
    //            targetClass,
    //            queryLHS,
    //            targetMethod,
    //            SparseCFGCache.SparsificationStrategy.TYPE_BASED,
    //            true);
    //    Set<AccessPath> aliasAwareSparseAliases =
    //        getAliasesFirst(
    //            targetClass,
    //            queryLHS,
    //            targetMethod,
    //            SparseCFGCache.SparsificationStrategy.ALIAS_AWARE,
    //            true);
    //    checkResults(
    //        SparseCFGCache.SparsificationStrategy.TYPE_BASED, typeBasedSparseAliases,
    // nonSparseAliases);
    //    checkResults(
    //        SparseCFGCache.SparsificationStrategy.ALIAS_AWARE,
    //        aliasAwareSparseAliases,
    //        nonSparseAliases);
    System.out.println(nonSparseAliases);
  }

  protected void runAnalyses(
      String queryLHS, String targetClass, String targetMethod, boolean ignoreAfterQuery) {
    Set<AccessPath> nonSparseAliases =
        getAliasesFirst(
            targetClass,
            queryLHS,
            targetMethod,
            SparseCFGCache.SparsificationStrategy.NONE,
            ignoreAfterQuery);
    Set<AccessPath> typeBasedSparseAliases =
        getAliasesFirst(
            targetClass,
            queryLHS,
            targetMethod,
            SparseCFGCache.SparsificationStrategy.TYPE_BASED,
            ignoreAfterQuery);
    Set<AccessPath> aliasAwareSparseAliases =
        getAliasesFirst(
            targetClass,
            queryLHS,
            targetMethod,
            SparseCFGCache.SparsificationStrategy.ALIAS_AWARE,
            ignoreAfterQuery);
    checkResults(
        SparseCFGCache.SparsificationStrategy.TYPE_BASED, typeBasedSparseAliases, nonSparseAliases);
    checkResults(
        SparseCFGCache.SparsificationStrategy.ALIAS_AWARE,
        aliasAwareSparseAliases,
        nonSparseAliases);
  }

  protected Set<AccessPath> getAliasesFirst(
      String targetClass,
      String queryLHS,
      String targetMethod,
      SparseCFGCache.SparsificationStrategy sparsificationStrategy,
      boolean ignoreAfterQuery) {
    Set<AccessPath> aliases =
        executeStaticAnalysis(
            targetClass, targetMethod, queryLHS, sparsificationStrategy, ignoreAfterQuery);
    return aliases;
  }

  protected void checkResults(
      SparseCFGCache.SparsificationStrategy strategy,
      Set<AccessPath> sparseAliases,
      Set<AccessPath> nonSparseAliases) {
    List<String> nonSparse =
        nonSparseAliases.stream().map(e -> e.toString()).collect(Collectors.toList());
    List<String> sparse =
        sparseAliases.stream().map(e -> e.toString()).collect(Collectors.toList());
    removeIntermediateLocals(nonSparse, sparse);
    if (!FalsePositiveInDefaultBoomerang)
      assertTrue(
          strategy + " " + generateDiffMessage(nonSparse, sparse), sparse.containsAll(nonSparse));
    assertTrue(
        "nonSparse " + generateDiffMessage(sparse, nonSparse), nonSparse.containsAll(sparse));
  }

  private void removeIntermediateLocals(List<String> nonSparse, List<String> sparse) {
    Predicate<String> isIntermediate = e -> e.startsWith("$stack");
    nonSparse.removeIf(isIntermediate);
    sparse.removeIf(isIntermediate);
  }

  private String generateDiffMessage(List<String> larger, List<String> smaller) {
    larger.removeAll(smaller); // remove only for message generation
    String collect = larger.stream().collect(Collectors.joining(System.lineSeparator()));
    larger.addAll(smaller);
    return "missing " + collect;
  }
}
