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
package typestate;

import boomerang.scope.ControlFlowGraph.Edge;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import typestate.finiteautomata.ITransition;
import typestate.finiteautomata.Transition;
import wpds.impl.Weight;

import static typestate.TransitionRepresentationFunction.one;
import static typestate.TransitionRepresentationFunction.zero;

public class TransitionFunction extends Weight {

  private final Set<ITransition> value;

  private final Set<Edge> stateChangeStatements;

  public TransitionFunction(Set<? extends ITransition> trans, Set<Edge> stateChangeStatements) {
    this.stateChangeStatements = stateChangeStatements;
    this.value = new HashSet<>(trans);
  }

  public TransitionFunction(ITransition trans, Set<Edge> stateChangeStatements) {
    this(new HashSet<>(Collections.singleton(trans)), stateChangeStatements);
  }

  public Collection<ITransition> values() {
    return Lists.newArrayList(value);
  }


  @Override
  public Weight extendWith(Weight other) {
    if (other.equals(one())) {
        return this;
    }
    if (this.equals(TransitionRepresentationFunction.one())) {
        return other;
    }
    if (other.equals(TransitionRepresentationFunction.zero()) || this.equals(TransitionRepresentationFunction.zero())) {
      return zero();
    }
    TransitionFunction func = (TransitionFunction) other;
    Set<ITransition> otherTransitions = func.value;
    Set<ITransition> ress = new HashSet<>();
    Set<Edge> newStateChangeStatements = new HashSet<>();
    for (ITransition first : value) {
      for (ITransition second : otherTransitions) {
        if (second.equals(Transition.identity())) {
          ress.add(first);
          newStateChangeStatements.addAll(stateChangeStatements);
        } else if (first.equals(Transition.identity())) {
          ress.add(second);
          newStateChangeStatements.addAll(func.stateChangeStatements);
        } else if (first.to().equals(second.from())) {
          ress.add(new Transition(first.from(), second.to()));
          newStateChangeStatements.addAll(func.stateChangeStatements);
        }
      }
    }
    return new TransitionFunction(ress, newStateChangeStatements);
  }

  @Override
  public Weight combineWith(Weight other) {
    if (!(other instanceof TransitionFunction)) {
        throw new RuntimeException();
    }
    if (this.equals(zero())) {
        return other;
    }
    if (other.equals(zero())) return this;
    if (other.equals(one()) && this.equals(one())) {
      return one();
    }
    TransitionFunction func = (TransitionFunction) other;
    if (other.equals(one()) || this.equals(one())) {
      Set<ITransition> transitions = new HashSet<>((other.equals(one()) ? value : func.value));
      Set<ITransition> idTransitions = Sets.newHashSet();
      for (ITransition t : transitions) {
        idTransitions.add(new Transition(t.from(), t.from()));
      }
      transitions.addAll(idTransitions);
      return new TransitionFunction(
          transitions,
          Sets.newHashSet(
              (other.equals(one()) ? stateChangeStatements : func.stateChangeStatements)));
    }
    Set<ITransition> transitions = new HashSet<>(func.value);
    transitions.addAll(value);
    HashSet<Edge> newStateChangeStmts = Sets.newHashSet(stateChangeStatements);
    newStateChangeStmts.addAll(func.stateChangeStatements);
    return new TransitionFunction(transitions, newStateChangeStmts);
  }

  public String toString() {
    return "Weight: " + value.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((value == null) ? 0 : value.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    TransitionFunction other = (TransitionFunction) obj;
    if (value == null) {
      return other.value == null;
    } else return value.equals(other.value);
  }
}
