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
package test.cases.lists;

import org.junit.Test;
import test.core.AbstractBoomerangTest;

public class LinkedListsLongTest extends AbstractBoomerangTest {

  private final String target = LinkedListsLongTarget.class.getName();

  @Test
  public void addAndRetrieveWithIterator() {
    analyze(target, testName.getMethodName());
  }

  @Test
  public void addAndRetrieveByIndex1() {
    analyze(target, testName.getMethodName());
  }

  @Test
  public void addAndRetrieveByIndex2() {
    analyze(target, testName.getMethodName());
  }

  @Test
  public void addAndRetrieveByIndex3() {
    analyze(target, testName.getMethodName());
  }
}
