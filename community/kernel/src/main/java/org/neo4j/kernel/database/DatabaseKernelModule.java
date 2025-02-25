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
package org.neo4j.kernel.database;

import org.neo4j.collection.Dependencies;
import org.neo4j.kernel.impl.api.KernelImpl;
import org.neo4j.kernel.impl.api.KernelTransactions;
import org.neo4j.kernel.impl.api.TransactionCommitProcess;
import org.neo4j.kernel.impl.store.StoreFileListing;

class DatabaseKernelModule
{
    private final TransactionCommitProcess transactionCommitProcess;
    private final KernelImpl kernel;
    private final KernelTransactions kernelTransactions;
    private final StoreFileListing fileListing;

    DatabaseKernelModule( TransactionCommitProcess transactionCommitProcess, KernelImpl kernel,
            KernelTransactions kernelTransactions, StoreFileListing fileListing )
    {
        this.transactionCommitProcess = transactionCommitProcess;
        this.kernel = kernel;
        this.kernelTransactions = kernelTransactions;
        this.fileListing = fileListing;
    }

    public KernelImpl kernelAPI()
    {
        return kernel;
    }

    KernelTransactions kernelTransactions()
    {
        return kernelTransactions;
    }

    StoreFileListing fileListing()
    {
        return fileListing;
    }

    public void satisfyDependencies( Dependencies dependencies )
    {
        dependencies.satisfyDependencies( transactionCommitProcess, kernel, kernelTransactions, fileListing );
    }
}
