/*
 * Copyright (c) 2002-2015 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
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
package org.neo4j.cypher.internal.compiler.v3_0.planner.logical

import org.neo4j.cypher.internal.compiler.v3_0.pipes.LazyLabel
import org.neo4j.cypher.internal.compiler.v3_0.planner.LogicalPlanningTestSupport2
import org.neo4j.cypher.internal.compiler.v3_0.planner.logical.plans._
import org.neo4j.cypher.internal.frontend.v3_0.SemanticDirection
import org.neo4j.cypher.internal.frontend.v3_0.ast._
import org.neo4j.cypher.internal.frontend.v3_0.test_helpers.CypherFunSuite

class NamedPathProjectionPlanningIntegrationTest extends CypherFunSuite with LogicalPlanningTestSupport2 {

  test("should build plans containing outgoing path projections") {
    planFor("MATCH p = (a:X)-[r]->(b) RETURN p").plan should equal(
      Projection(
        Expand( NodeByLabelScan("a",  LazyLabel("X"), Set.empty)(solved), "a", SemanticDirection.OUTGOING, Seq.empty, "b", "r")(solved),
        expressions = Map(
          "p" -> PathExpression(NodePathStep(Variable("a")_,SingleRelationshipPathStep(Variable("r")_, SemanticDirection.OUTGOING, NilPathStep)))_
        )
      )(solved)
    )
  }

  test("should build plans containing path projections and path selections") {
    val pathExpr = PathExpression(NodePathStep(Variable("a")_,SingleRelationshipPathStep(Variable("r")_, SemanticDirection.OUTGOING, NilPathStep)))_

    val result = planFor("MATCH p = (a:X)-[r]->(b) WHERE head(nodes(p)) = a RETURN b").plan

    result should equal(
      Selection(
        Seq(Equals(
          FunctionInvocation(FunctionName("head") _, FunctionInvocation(FunctionName("nodes") _, varFor("p")) _) _,
          varFor("a")
        ) _),
        Projection(
          Expand(NodeByLabelScan("a", LazyLabel("X"), Set.empty)(solved), "a", SemanticDirection.OUTGOING, Seq.empty, "b", "r")(solved),
          expressions = Map("a" -> varFor("a"), "b" -> varFor("b"), "p" -> pathExpr, "r" -> varFor("r")))(solved)
      )(solved)
    )
  }

  test("should build plans containing multiple path projections and path selections") {
    val pathExpr = PathExpression(NodePathStep(Variable("a")_,SingleRelationshipPathStep(Variable("r")_, SemanticDirection.OUTGOING, NilPathStep)))_

    val result = planFor("MATCH p = (a:X)-[r]->(b) WHERE head(nodes(p)) = a AND length(p) > 10 RETURN b").plan

    result should equal(
      Selection(
        Seq(
          Equals(
            FunctionInvocation(FunctionName("head") _, FunctionInvocation(FunctionName("nodes") _, varFor("p")) _) _,
            Variable("a") _
          ) _,
          GreaterThan(
            FunctionInvocation(FunctionName("length") _, varFor("p")) _,
            SignedDecimalIntegerLiteral("10") _
          ) _
        ),
        Projection(
          Expand(NodeByLabelScan("a", LazyLabel("X"), Set.empty)(solved), "a", SemanticDirection.OUTGOING, Seq.empty, "b", "r")(solved),
          expressions = Map("a" -> varFor("a"), "b" -> varFor("b"), "p" -> pathExpr, "r" -> varFor("r"))
        )(solved)
      )(solved)
    )
  }
}
