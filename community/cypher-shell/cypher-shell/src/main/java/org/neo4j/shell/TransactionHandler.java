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
package org.neo4j.shell;

import java.util.Map;
import java.util.Optional;

import org.neo4j.shell.exception.CommandException;
import org.neo4j.shell.state.BoltResult;

/**
 * An object capable of starting, committing, and rolling back transactions.
 */
public interface TransactionHandler
{

    /**
     * @throws CommandException if a new transaction could not be started
     */
    void beginTransaction() throws CommandException;

    /**
     * @throws CommandException if current transaction could not be committed
     */
    void commitTransaction() throws CommandException;

    /**
     * @throws CommandException if current transaction could not be rolled back
     */
    void rollbackTransaction() throws CommandException;

    /**
     * @return true if a transaction is currently open, false otherwise
     */
    boolean isTransactionOpen();
}
