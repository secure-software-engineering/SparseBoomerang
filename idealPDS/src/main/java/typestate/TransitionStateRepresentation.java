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
import typestate.finiteautomata.ITransition;
import typestate.finiteautomata.Transition;
import wpds.impl.Weight;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static typestate.TransitionRepresentationFunction.one;
import static typestate.TransitionRepresentationFunction.zero;

public class TransitionStateRepresentation extends Weight {

  private final Set<ITransition> value;



  public TransitionStateRepresentation(Set<? extends ITransition> trans) {
    this.value = new HashSet<>(trans);
  }

  public TransitionStateRepresentation(ITransition trans) {
    this(new HashSet<>(Collections.singleton(trans)));
  }

  public Collection<ITransition> values() {
    return Lists.newArrayList(value);
  }


  @Override
  public Weight extendWith(Weight other) {
    if (other.equals(one())) return this;
    if (this.equals(one())) {
        return other;
    }
    if (other.equals(zero()) || this.equals(zero())) {
      return zero();
    }
    TransitionStateRepresentation func = (TransitionStateRepresentation) other;
    Set<ITransition> otherTransitions = func.value;
    Set<ITransition> ress = new HashSet<>();
    Set<Edge> newStateChangeStatements = new HashSet<>();
    for (ITransition first : value) {
      for (ITransition second : otherTransitions) {
        if (second.equals(Transition.identity())) {
          ress.add(first);

        } else if (first.equals(Transition.identity())) {
          ress.add(second);

        } else if (first.to().equals(second.from())) {
          ress.add(new Transition(first.from(), second.to()));

        }
      }
    }
    return new TransitionStateRepresentation(ress);
  }

  @Override
  public Weight combineWith(Weight other) {
    if (!(other instanceof TransitionStateRepresentation)) {
        throw new RuntimeException();
    }
    if (this.equals(zero())) return other;
    if (other.equals(zero())) return this;
    if (other.equals(one()) && this.equals(one())) {
      return one();
    }
    TransitionStateRepresentation func = (TransitionStateRepresentation) other;
    if (other.equals(one()) || this.equals(one())) {
      Set<ITransition> transitions = new HashSet<>((other.equals(one()) ? value : func.value));
      Set<ITransition> idTransitions = Sets.newHashSet();
      for (ITransition t : transitions) {
        idTransitions.add(new Transition(t.from(), t.from()));
      }
      transitions.addAll(idTransitions);
      return new TransitionStateRepresentation(transitions );
    }
    Set<ITransition> transitions = new HashSet<>(func.value);
    transitions.addAll(value);
    return new TransitionStateRepresentation(transitions);
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
    TransitionStateRepresentation other = (TransitionStateRepresentation) obj;
    if (value == null) {
      return other.value == null;
    } else return value.equals(other.value);
  }
}
