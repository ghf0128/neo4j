/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.internal.compiler.planner.logical.plans.rewriter

import org.neo4j.cypher.internal.expressions.Ands
import org.neo4j.cypher.internal.expressions.Expression
import org.neo4j.cypher.internal.expressions.FunctionInvocation
import org.neo4j.cypher.internal.logical.plans.AggregatingPlan
import org.neo4j.cypher.internal.logical.plans.Apply
import org.neo4j.cypher.internal.logical.plans.Expand
import org.neo4j.cypher.internal.logical.plans.ExpandAll
import org.neo4j.cypher.internal.logical.plans.LogicalPlan
import org.neo4j.cypher.internal.logical.plans.Optional
import org.neo4j.cypher.internal.logical.plans.Projection
import org.neo4j.cypher.internal.logical.plans.PruningVarExpand
import org.neo4j.cypher.internal.logical.plans.Selection
import org.neo4j.cypher.internal.logical.plans.VarExpand
import org.neo4j.cypher.internal.util.Rewriter
import org.neo4j.cypher.internal.util.attribution.SameId
import org.neo4j.cypher.internal.util.topDown

import scala.collection.mutable

case object pruningVarExpander extends Rewriter {

  private def findDistinctSet(plan: LogicalPlan): Set[LogicalPlan] = {
    val distinctSet = mutable.Set[VarExpand]()

    def collectDistinctSet(plan: LogicalPlan,
                           dependencies: Option[Set[String]]): (Option[Set[String]], Option[Set[String]]) = {

      val lowerDistinctLand = plan match {

        case aggPlan: AggregatingPlan if aggPlan.aggregationExpressions.values.forall(isDistinct) =>
          val variablesInTheDistinctSet = (aggPlan.groupingExpressions.values.flatMap(_.dependencies.map(_.name)) ++
            aggPlan.aggregationExpressions.values.flatMap(_.dependencies.map(_.name))).toSet
          (Some(variablesInTheDistinctSet), None)

        case expand: VarExpand
          if dependencies.nonEmpty && !distinctNeedsRelsFromExpand(dependencies, expand) && expand.length.max.nonEmpty =>
          distinctSet += expand
          (dependencies, None)

        case Projection(_, expressions) =>
          (dependencies.map(_ ++ expressions.values.flatMap(_.dependencies.map(_.name))), None)

        case Selection(Ands(predicates), _) =>
          (dependencies.map(_ ++ predicates.flatMap(_.dependencies.map(_.name))), None)

        case _: Expand |
             _: VarExpand |
             _: Optional =>
          (dependencies, None)

        case _: Apply =>
          (None, dependencies)

        case _ =>
          (None, None)
      }

      lowerDistinctLand
    }

    val planStack = new mutable.ArrayStack[(LogicalPlan, Option[Set[String]])]()
    planStack.push((plan, None))

    while(planStack.nonEmpty) {
      val (plan: LogicalPlan, deps: Option[Set[String]]) = planStack.pop()
      val (newLhsDeps, newRLhsDeps) = collectDistinctSet(plan, deps)

      plan.lhs.foreach(p => planStack.push((p, newLhsDeps)))
      plan.rhs.foreach(p => planStack.push((p, newRLhsDeps)))
    }

    distinctSet.toSet
  }

  // When the distinct horizon needs the path that includes the var length relationship,
  // we can't use DistinctVarExpand - we need all the paths
  def distinctNeedsRelsFromExpand(inDistinctLand: Option[Set[String]], expand: VarExpand): Boolean = {
    inDistinctLand.forall(vars => vars(expand.relName))
  }

  private def isDistinct(e: Expression) = e match {
    case f: FunctionInvocation => f.distinct
    case _ => false
  }

  override def apply(input: AnyRef): AnyRef = {
    input match {
      case plan: LogicalPlan =>
        val distinctSet = findDistinctSet(plan)

        val innerRewriter = topDown(Rewriter.lift {
          case expand@VarExpand(lhs,
                                fromId,
                                dir,
                                _,
                                relTypes,
                                toId,
                                _,
                                length,
                                ExpandAll,
                                nodePredicate,
                                relationshipPredicate) if distinctSet(expand) =>
            if (length.max.get > 1)
              PruningVarExpand(lhs,
                               fromId,
                               dir,
                               relTypes,
                               toId,
                               length.min,
                               length.max.get,
                               nodePredicate,
                               relationshipPredicate)(SameId(expand.id))
            else expand
        })
        plan.endoRewrite(innerRewriter)

      case _ => input
    }
  }
}
