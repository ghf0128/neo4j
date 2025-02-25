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
package org.neo4j.graphdb.traversal;

import org.neo4j.annotations.api.PublicApi;

/**
 * Interface for filters preventing the traversal from visiting already seen parts of the graph. Implementations can
 * allow for different heuristics to use to determine what may be re-visited.
 */
@PublicApi
public interface UniquenessFilter
{
    /**
     * The check whether or not to expand the first branch is a separate
     * method because it may contain checks which would be unnecessary for
     * all other checks. So it's purely an optimization.
     *
     * @param branch the first branch to check, i.e. the branch representing
     *               the start node in the traversal.
     * @return whether or not {@code branch} is unique, and hence can be
     *         visited in this traversal.
     */
    boolean checkFirst( TraversalBranch branch );

    /**
     * Checks whether or not {@code branch} is unique, and hence can be
     * visited in this traversal.
     *
     * @param branch the {@link TraversalBranch} to check for uniqueness.
     * @return whether or not {@code branch} is unique, and hence can be
     *         visited in this traversal.
     */
    boolean check( TraversalBranch branch );
}
