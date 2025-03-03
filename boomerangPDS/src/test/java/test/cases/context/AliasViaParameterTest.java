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
package test.cases.context;

import org.junit.Test;
import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class AliasViaParameterTest extends AbstractBoomerangTest {

  private final String target = AliasViaParameterTarget.class.getName();

  @Test
  public void aliasViaParameter() {
    analyze(target, testName.getMethodName());
  }

  @Test
  public void aliasViaParameterWrapped() {
    analyze(target, testName.getMethodName());
  }
}
