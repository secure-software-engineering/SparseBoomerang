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
package test.cases.hashmap;

import boomerang.scope.DataFlowScope;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import test.cases.array.ArrayTest.NoAllocation;
import test.cases.basic.Allocation;
import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class KeySensitiveTest extends AbstractBoomerangTest {

  @Test
  public void directAccess() {
    AllocatedObject someValue = new Alloc();
    Map<String, AllocatedObject> x = new HashMap<>();
    x.put("key", someValue);
    AllocatedObject t = x.get("key");
    queryFor(t);
  }

  @Test
  public void directAccess2Keys() {
    AllocatedObject someValue = new Alloc();
    Map<String, Object> x = new HashMap<>();
    x.put("key", someValue);
    x.put("key2", new NoAllocation());
    Object t = x.get("key");
    queryFor(t);
  }

  @Test
  public void overwrite() {
    AllocatedObject someValue = new Alloc();
    Map<String, Object> x = new HashMap<>();
    // False Positive: Overapproximation. We do not kill during the forward analysis.
    x.put("key", new Allocation());
    x.put("key", someValue);
    Object t = x.get("key");
    queryFor(t);
  }

  @Test
  public void accessWithAliasedKey() {
    AllocatedObject someValue = new Alloc();
    Map<String, Object> x = new HashMap<>();
    String key = "key";
    x.put(key, someValue);
    x.put("key2", new NoAllocation());
    Object t = x.get(key);
    queryFor(t);
  }

  /*
    classes: 3413
  [main] INFO boomerang.scene.AnalysisScope - Computing seeds starting at 1 entry method(s).
  [main] INFO boomerang.scene.AnalysisScope - Found 1 seeds in 35.17 ms in 3588 LOC .
  [main] INFO boomerang.scene.AnalysisScope - Computing seeds starting at 1 entry method(s).
  [main] INFO boomerang.scene.AnalysisScope - Found 1 seeds in 24.64 ms in 3588 LOC .
  [main] INFO boomerang.scene.AnalysisScope - Computing seeds starting at 1 entry method(s).
  [main] INFO boomerang.scene.AnalysisScope - Found 1 seeds in 5.280 ms in 3588 LOC .
  [main] INFO test.core.AbstractBoomerangTest - Solving query took: 168.5 ms
  [main] INFO test.core.AbstractBoomerangTest - Expected results: PT0.168372811S
  [main] INFO test.core.AbstractBoomerangTest - Boomerang Results: [($stack4 (test.cases.hashmap.KeySensitiveTest.<test.cases.hashmap.KeySensitiveTest: void accessWithKeyFromReturn()>),$stack4 = new Alloc -> $stack4.<init>())]
  [main] INFO test.core.AbstractBoomerangTest - Expected Results: [ForwardQuery: ($stack4 (test.cases.hashmap.KeySensitiveTest.<test.cases.hashmap.KeySensitiveTest: void accessWithKeyFromReturn()>),$stack4 = new Alloc -> $stack4.<init>())]

     */
  @Test
  public void accessWithKeyFromReturn() {
    AllocatedObject someValue = new Alloc();
    Map<String, Object> x = new HashMap<>();
    x.put(getKey(), someValue);
    x.put("key2", new NoAllocation());
    Object t = x.get(getKey());
    queryFor(t);
  }

  @Test
  public void interprocedural() {
    AllocatedObject someValue = new Alloc();
    Map<String, Object> x = new HashMap<>();
    x.put(getKey(), someValue);
    x.put("key2", new NoAllocation());
    Object t = wrap(x);

    queryFor(t);
  }

  private Object wrap(Map<String, Object> mp) {
    Object i = mp.get(getKey());
    return i;
  }

  private String getKey() {
    return "KEY";
  }

  @Override
  protected DataFlowScope getDataFlowScope() {
    return frameworkScope.createDataFlowScopeWithoutComplex();
  }
}
