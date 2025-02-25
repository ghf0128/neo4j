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
package org.neo4j.dbms.database;

import java.util.List;

import org.neo4j.configuration.Config;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.ConstraintType;
import org.neo4j.internal.helpers.collection.Iterables;
import org.neo4j.internal.helpers.collection.Iterators;
import org.neo4j.util.Preconditions;

import static org.neo4j.configuration.GraphDatabaseSettings.SYSTEM_DATABASE_NAME;

/**
 * Common code for all system graph components, apart from test implementations and the central collection class {@link SystemGraphComponents}.
 */
public abstract class AbstractSystemGraphComponent implements SystemGraphComponent
{
    protected final Config config;

    public AbstractSystemGraphComponent( Config config )
    {
        this.config = config;
    }

    protected void initializeSystemGraphConstraints( Transaction tx )
    {
    }

    protected void initializeSystemGraphModel( Transaction tx ) throws Exception
    {
    }

    protected void verifySystemGraph( GraphDatabaseService system ) throws Exception
    {
    }

    private void initializeSystemGraphConstraints( GraphDatabaseService system )
    {
        try ( Transaction tx = system.beginTx() )
        {
            initializeSystemGraphConstraints( tx );
            tx.commit();
        }
    }

    protected void initializeSystemGraphModel( GraphDatabaseService system ) throws Exception
    {
        try ( Transaction tx = system.beginTx() )
        {
            initializeSystemGraphModel( tx );
            tx.commit();
        }
    }

    protected void postInitialization( GraphDatabaseService system, boolean wasInitialized ) throws Exception
    {
    }

    @Override
    public void initializeSystemGraph( GraphDatabaseService system, boolean firstInitialization ) throws Exception
    {
        boolean mayUpgrade = config.get( GraphDatabaseSettings.allow_single_automatic_upgrade );

        Preconditions.checkState( system.databaseName().equals( SYSTEM_DATABASE_NAME ),
                "Cannot initialize system graph on database '" + system.databaseName() + "'" );

        Status status = detect( system );
        if ( status == Status.UNINITIALIZED )
        {
            initializeSystemGraphConstraints( system );
            initializeSystemGraphModel( system );
            postInitialization( system, true );
        }
        else if ( status == Status.CURRENT || (status == Status.REQUIRES_UPGRADE && !mayUpgrade) )
        {
            verifySystemGraph( system );
            postInitialization( system, false );
        }
        else if ( (mayUpgrade && status == Status.REQUIRES_UPGRADE) || status == Status.UNSUPPORTED_BUT_CAN_UPGRADE )
        {
            upgradeToCurrent( system );
        }
        else
        {
            throw new IllegalStateException( String.format( "Unsupported component state for '%s': %s", componentName(), status.description() ) );
        }
    }

    protected static void initializeSystemGraphConstraint( Transaction tx, Label label, String property )
    {
        // Makes the creation of constraints for security idempotent
        if ( !hasUniqueConstraint( tx, label, property ) )
        {
            checkForClashingIndexes( tx, label, property );
            tx.schema().constraintFor( label ).assertPropertyIsUnique( property ).create();
        }
    }

    protected static boolean hasUniqueConstraint( Transaction tx, Label label, String property )
    {
        return Iterators.stream( tx.schema().getConstraints( label ).iterator() )
                        .anyMatch( constraintDefinition ->
                                           Iterables.asList( constraintDefinition.getPropertyKeys() ).equals( List.of( property ) ) &&
                                           constraintDefinition.isConstraintType( ConstraintType.UNIQUENESS )
                        );
    }

    private static void checkForClashingIndexes( Transaction tx, Label label, String property )
    {
        tx.schema().getIndexes( label )
          .forEach( index ->
                    {
                        List<String> propertyKeys = Iterables.asList( index.getPropertyKeys() );
                        if ( propertyKeys.size() == 1 && propertyKeys.get( 0 ).equals( property ) )
                        {
                            index.drop();
                        }
                    } );
    }
}
