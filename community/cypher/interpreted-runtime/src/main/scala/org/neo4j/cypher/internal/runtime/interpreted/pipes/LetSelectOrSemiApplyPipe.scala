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
package org.neo4j.cypher.internal.runtime.interpreted.pipes

import org.neo4j.cypher.internal.runtime.ClosingIterator
import org.neo4j.cypher.internal.runtime.CypherRow
import org.neo4j.cypher.internal.runtime.interpreted.commands.predicates.Predicate
import org.neo4j.cypher.internal.util.attribution.Id
import org.neo4j.values.storable.Values

case class LetSelectOrSemiApplyPipe(source: Pipe, inner: Pipe, letVarName: String, predicate: Predicate, negated: Boolean)
                                   (val id: Id = Id.INVALID_ID)
  extends PipeWithSource(source) {

  protected def internalCreateResults(input: ClosingIterator[CypherRow], state: QueryState): ClosingIterator[CypherRow] = {
    input.map {
      outerContext =>
        val predicateResult = predicate.isMatch(outerContext, state)
        val holds = (predicateResult.getOrElse(false)) || {
          val innerState = state.withInitialContext(outerContext)
          val innerResults = inner.createResults(innerState)
          val result = if (negated) !innerResults.hasNext else innerResults.hasNext
          innerResults.close()
          result
        }

        val output =
          if (!holds && predicateResult.isEmpty)
            Values.NO_VALUE
          else
            Values.booleanValue(holds)

        outerContext.set(letVarName, output)
        outerContext
    }
  }
}
