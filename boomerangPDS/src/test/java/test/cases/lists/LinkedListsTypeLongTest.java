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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;
import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class LinkedListsTypeLongTest extends AbstractBoomerangTest {

  private final String target = LinkedListsTypeLongTarget.class.getName();

  @Test
  public void addAndRetrieveWithIteratorWithTyping() {
    analyze(target, testName.getMethodName());
  }
}
