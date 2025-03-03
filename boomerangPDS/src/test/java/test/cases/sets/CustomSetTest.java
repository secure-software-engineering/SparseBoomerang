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

import boomerang.scope.DataFlowScope;
import boomerang.scope.DeclaredMethod;
import boomerang.scope.Method;
import boomerang.scope.WrappedClass;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class CustomSetTest extends AbstractBoomerangTest {

  private final String target = CustomSetTarget.class.getName();

  @Test
  public void mySetIteratorTest() {
    analyze(target, testName.getMethodName());
  }

  @Test
  public void mySetIterableTest() {
    analyze(target, testName.getMethodName());
  }

  @Override
  public DataFlowScope getDataFlowScope() {
    return new DataFlowScope() {
      @Override
      public boolean isExcluded(DeclaredMethod method) {
        WrappedClass wrappedClass = method.getDeclaringClass();

        return wrappedClass.isPhantom()
            || wrappedClass.getFullyQualifiedName().equals("java.lang.Object");
      }

      @Override
      public boolean isExcluded(Method method) {
        WrappedClass wrappedClass = method.getDeclaringClass();

        return wrappedClass.isPhantom()
            || wrappedClass.getFullyQualifiedName().equals("java.lang.Object");
      }
    };
  }
}
