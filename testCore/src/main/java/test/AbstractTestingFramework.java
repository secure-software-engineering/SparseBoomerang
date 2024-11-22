/**
 * ***************************************************************************** Copyright (c) 2018
 * Fraunhofer IEM, Paderborn, Germany. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 *
 * <p>Contributors: Johannes Spaeth - initial API and implementation
 * *****************************************************************************
 */
package test;

import boomerang.scene.FrameworkScope;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import test.core.selfrunning.ImprecisionException;

public abstract class AbstractTestingFramework {
  @Rule public TestName testMethodName = new TestName();
  protected File ideVizFile;
  protected File dotFile;
  protected FrameworkScope scopeFactory;

  protected abstract void initializeWithEntryPoint();

  protected abstract void analyze();

  protected String buildClassPath() {
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

  protected List<String> getIncludedList() {
    List<String> includeList = new ArrayList<>();
    includeList.add("java.lang.*");
    includeList.add("java.util.*");
    includeList.add("java.io.*");
    includeList.add("sun.misc.*");
    includeList.add("java.net.*");
    includeList.add("sun.nio.*");
    includeList.add("javax.servlet.*");
    return includeList;
  }

  protected List<String> getExludedPackageList() {
    List<String> excludedPackages = new ArrayList<>();
    excludedPackages.add("sun.*");
    excludedPackages.add("javax.*");
    excludedPackages.add("com.sun.*");
    excludedPackages.add("com.ibm.*");
    excludedPackages.add("org.xml.*");
    excludedPackages.add("org.w3c.*");
    excludedPackages.add("apple.awt.*");
    excludedPackages.add("com.apple.*");
    return excludedPackages;
  }

  protected void cleanup() {
    // implement me in subclass if necessary
  }

  @Before
  public void beforeTestCaseExecution() {

    initializeWithEntryPoint();
    System.out.println("ms: initialized");
    createDebugFiles();
    try {
      analyze();
      System.out.println("ms: analyzed");
    } catch (ImprecisionException e) {
      Assert.fail(e.getMessage());
    } finally {
      cleanup();
      System.out.println("ms: cleanupped.");
    }
    // To never execute the @Test method...
    org.junit.Assume.assumeTrue(false);
  }

  private void createDebugFiles() {
    ideVizFile =
        new File(
            "target/IDEViz/"
                + getTestCaseClassName()
                + "/IDEViz-"
                + testMethodName.getMethodName()
                + ".json");
    if (!ideVizFile.getParentFile().exists()) {
      try {
        Files.createDirectories(ideVizFile.getParentFile().toPath());
      } catch (IOException e) {
        throw new RuntimeException("Was not able to create directories for IDEViz output!");
      }
    }
    dotFile =
        new File(
            "target/dot/"
                + getTestCaseClassName()
                + "/Dot-"
                + testMethodName.getMethodName()
                + ".dot");
    if (!dotFile.getParentFile().exists()) {
      try {
        Files.createDirectories(dotFile.getParentFile().toPath());
      } catch (IOException e) {
        throw new RuntimeException("Was not able to create directories for dot output!");
      }
    }
  }

  public String getTestCaseClassName() {
    return this.getClass().getName().replace("class ", "");
  }

  /**
   * This method can be used in test cases to create branching. It is not optimized away.
   *
   * @return
   */
  protected boolean staticallyUnknown() {
    return true;
  }

  static int getJavaVersion() {
    String version = System.getProperty("java.version");
    if (version.startsWith("1.")) {
      version = version.substring(2, 3);
    } else {
      int dot = version.indexOf(".");
      if (dot != -1) {
        version = version.substring(0, dot);
      }
    }
    return Integer.parseInt(version);
  }
}
