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
package org.neo4j.cypher.internal.logical.plans

import org.neo4j.cypher.internal.expressions.Expression
import org.neo4j.cypher.internal.macros.AssertMacros
import org.neo4j.cypher.internal.util.attribution.IdGen

/**
 * OrderedDistinct is like Distinct, except that it relies on the input coming
 * * in a particular order, which it can leverage by keeping less state to aggregate at any given time.
 */
case class OrderedDistinct(override val source: LogicalPlan,
                           override val groupingExpressions: Map[String, Expression],
                           orderToLeverage: Seq[Expression])
                          (implicit idGen: IdGen) extends LogicalUnaryPlan(idGen)  with ProjectingPlan with AggregatingPlan {

  override val projectExpressions: Map[String, Expression] = groupingExpressions
  override val availableSymbols: Set[String] = groupingExpressions.keySet

  override def aggregationExpressions: Map[String, Expression] = Map.empty

  override def withLhs(newLHS: LogicalPlan)(idGen: IdGen): LogicalUnaryPlan = copy(source = newLHS)(idGen)

  override def addGroupingExpressions(newGroupingExpressions: Map[String, Expression]): AggregatingPlan =
    copy(groupingExpressions = groupingExpressions ++ newGroupingExpressions)

  AssertMacros.checkOnlyWhenAssertionsAreEnabled(orderToLeverage.forall(exp => groupingExpressions.values.exists(_ == exp)),
   s"""orderToLeverage expressions can only be grouping expression values, i.e. the expressions _before_ the distinct.
       |Grouping expressions: $groupingExpressions
       |   Order to leverage: $orderToLeverage
       |   """.stripMargin)
}
