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
package test.cases.sets;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import org.junit.Ignore;
import org.junit.Test;
import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;

public class TreeSetsLongTest extends AbstractBoomerangTest {

  private final String target = TreeSetsLongTest.class.getName();

  @Ignore
  @Test
  public void addAndRetrieve() {
    analyze(target, testName.getMethodName());
  }
}
