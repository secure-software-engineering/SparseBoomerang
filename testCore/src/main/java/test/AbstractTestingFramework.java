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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import test.core.selfrunning.ImprecisionException;

public abstract class AbstractTestingFramework {
  @Rule public TestName testMethodName = new TestName();
  protected File ideVizFile;
  protected File dotFile;

  /** @return respective FrameworkTestFactory */
  public abstract FrameworkTestFactory getTestingFramework();

  @Before
  public void beforeTestCaseExecution() {
    FrameworkTestFactory ftf = getTestingFramework();

    ftf.initializeWithEntryPoint();
    createDebugFiles();
    try {
      ftf.analyze();
    } catch (ImprecisionException e) {
      Assert.fail(e.getMessage());
    }
    // To never execute the @Test method...
    org.junit.Assume.assumeTrue(false);
    ftf.cleanup();
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

  private static int getJavaVersion() {
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
