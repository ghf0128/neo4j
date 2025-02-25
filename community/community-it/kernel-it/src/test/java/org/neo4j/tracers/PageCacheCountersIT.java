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
package org.neo4j.tracers;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.ToLongFunction;

import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.internal.helpers.Cancelable;
import org.neo4j.io.pagecache.tracing.PageCacheTracer;
import org.neo4j.io.pagecache.tracing.cursor.PageCursorCounters;
import org.neo4j.kernel.impl.coreapi.InternalTransaction;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.kernel.monitoring.tracing.Tracers;
import org.neo4j.test.TestDatabaseManagementServiceBuilder;
import org.neo4j.test.extension.Inject;
import org.neo4j.test.extension.testdirectory.TestDirectoryExtension;
import org.neo4j.test.utils.TestDirectory;
import org.neo4j.util.concurrent.Futures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

@TestDirectoryExtension
class PageCacheCountersIT
{
    @Inject
    private TestDirectory testDirectory;

    private GraphDatabaseService db;
    private ExecutorService executors;
    private int numberOfWorkers;
    private DatabaseManagementService managementService;

    @BeforeEach
    void setUp()
    {
        managementService = new TestDatabaseManagementServiceBuilder( testDirectory.homePath() ).build();
        db = managementService.database( DEFAULT_DATABASE_NAME );
        numberOfWorkers = Runtime.getRuntime().availableProcessors();
        executors = Executors.newFixedThreadPool( numberOfWorkers );
    }

    @AfterEach
    void tearDown() throws InterruptedException
    {
        executors.shutdown();
        executors.awaitTermination( 5, TimeUnit.SECONDS );
        managementService.shutdown();
    }

    @RepeatedTest( 5 )
    @Timeout( 60 )
    void pageCacheCountersAreSumOfPageCursorCounters() throws Exception
    {

        List<NodeCreator> nodeCreators = new ArrayList<>( numberOfWorkers );
        List<Future<?>> nodeCreatorFutures = new ArrayList<>( numberOfWorkers );
        PageCacheTracer pageCacheTracer = getPageCacheTracer( db );

        long initialPins = pageCacheTracer.pins();
        long initialHits = pageCacheTracer.hits();
        long initialUnpins = pageCacheTracer.unpins();
        long initialBytesRead = pageCacheTracer.bytesRead();
        long initialBytesWritten = pageCacheTracer.bytesWritten();
        long initialEvictions = pageCacheTracer.evictions();
        long initialFaults = pageCacheTracer.faults();
        long initialFlushes = pageCacheTracer.flushes();
        long initialMerges = pageCacheTracer.merges();

        startNodeCreators( nodeCreators, nodeCreatorFutures );
        while ( pageCacheTracer.pins() == 0 || pageCacheTracer.faults() == 0 || pageCacheTracer.unpins() == 0 )
        {
            TimeUnit.MILLISECONDS.sleep( 10 );
        }
        stopNodeCreators( nodeCreators, nodeCreatorFutures );

        assertThat( pageCacheTracer.pins() ).as(
                "Number of pins events in page cache tracer should equal to the sum of pin events in page cursor tracers." ).isGreaterThanOrEqualTo(
                sumCounters( nodeCreators, NodeCreator::getPins, initialPins ) );
        assertThat( pageCacheTracer.unpins() ).as(
                "Number of unpins events in page cache tracer should equal to the sum of unpin events in page cursor tracers." ).isGreaterThanOrEqualTo(
                sumCounters( nodeCreators, NodeCreator::getUnpins, initialUnpins ) );
        assertThat( pageCacheTracer.bytesRead() ).as(
                "Number of initialBytesRead in page cache tracer should equal to the sum of initialBytesRead in page cursor tracers." ).isGreaterThanOrEqualTo(
                sumCounters( nodeCreators, NodeCreator::getBytesRead, initialBytesRead ) );
        assertThat( pageCacheTracer.bytesWritten() ).as(
                "Number of bytesWritten in page cache tracer should equal to the sum of bytesWritten in page cursor tracers." ).isGreaterThanOrEqualTo(
                sumCounters( nodeCreators, NodeCreator::getBytesWritten, initialBytesWritten ) );
        assertThat( pageCacheTracer.evictions() ).as(
                "Number of evictions in page cache tracer should equal to the sum of evictions in page cursor tracers." ).isGreaterThanOrEqualTo(
                sumCounters( nodeCreators, NodeCreator::getEvictions, initialEvictions ) );
        assertThat( pageCacheTracer.faults() ).as(
                "Number of faults in page cache tracer should equal to the sum of faults in page cursor tracers." ).isGreaterThanOrEqualTo(
                sumCounters( nodeCreators, NodeCreator::getFaults, initialFaults ) );
        assertThat( pageCacheTracer.flushes() ).as(
                "Number of flushes in page cache tracer should equal to the sum of flushes in page cursor tracers." ).isGreaterThanOrEqualTo(
                sumCounters( nodeCreators, NodeCreator::getFlushes, initialFlushes ) );
        assertThat( pageCacheTracer.merges() ).as(
                "Number of merges in page cache tracer should equal to the sum of merges in page cursor tracers." ).isGreaterThanOrEqualTo(
                sumCounters( nodeCreators, NodeCreator::getMerges, initialMerges ) );
        assertThat( pageCacheTracer.hits() ).as(
                "Number of hits in page cache tracer should equal to the sum of hits in page cursor tracers." ).isGreaterThanOrEqualTo(
                sumCounters( nodeCreators, NodeCreator::getHits, initialHits ) );
    }

    private static void stopNodeCreators( List<NodeCreator> nodeCreators, List<Future<?>> nodeCreatorFutures )
            throws java.util.concurrent.ExecutionException
    {
        nodeCreators.forEach( NodeCreator::cancel );
        Futures.getAll( nodeCreatorFutures );
    }

    private void startNodeCreators( List<NodeCreator> nodeCreators, List<Future<?>> nodeCreatorFutures )
    {
        for ( int i = 0; i < numberOfWorkers; i++ )
        {
            NodeCreator nodeCreator = new NodeCreator( db );
            nodeCreators.add( nodeCreator );
            nodeCreatorFutures.add( executors.submit( nodeCreator ) );
        }
    }

    private static long sumCounters( List<NodeCreator> nodeCreators, ToLongFunction<NodeCreator> mapper, long initialValue )
    {
        return nodeCreators.stream().mapToLong( mapper ).sum() + initialValue;
    }

    private static PageCacheTracer getPageCacheTracer( GraphDatabaseService db )
    {
        Tracers tracers = ((GraphDatabaseAPI) db).getDependencyResolver().resolveDependency( Tracers.class );
        return tracers.getPageCacheTracer();
    }

    private static class NodeCreator implements Runnable, Cancelable
    {
        private volatile boolean canceled;

        private final GraphDatabaseService db;
        private long pins;
        private long unpins;
        private long hits;
        private long bytesRead;
        private long bytesWritten;
        private long evictions;
        private long faults;
        private long flushes;
        private long merges;
        NodeCreator( GraphDatabaseService db )
        {
            this.db = db;
        }

        @Override
        public void run()
        {
            ThreadLocalRandom localRandom = ThreadLocalRandom.current();
            while ( !canceled )
            {
                try ( Transaction transaction = db.beginTx() )
                {
                    Node node = transaction.createNode();
                    node.setProperty( "name", RandomStringUtils.random( localRandom.nextInt( 100 ) ) );
                    node.setProperty( "surname", RandomStringUtils.random( localRandom.nextInt( 100 ) ) );
                    node.setProperty( "age", localRandom.nextInt( 100 ) );
                    storeCounters( ((InternalTransaction) transaction).kernelTransaction().cursorContext().getCursorTracer() );
                    transaction.commit();
                }
            }
        }

        private void storeCounters( PageCursorCounters pageCursorCounters )
        {
            Objects.requireNonNull( pageCursorCounters );
            pins += pageCursorCounters.pins();
            unpins += pageCursorCounters.unpins();
            hits += pageCursorCounters.hits();
            bytesRead += pageCursorCounters.bytesRead();
            bytesWritten += pageCursorCounters.bytesWritten();
            evictions += pageCursorCounters.evictions();
            faults += pageCursorCounters.faults();
            flushes += pageCursorCounters.flushes();
            merges += pageCursorCounters.merges();
        }

        @Override
        public void cancel()
        {
            canceled = true;
        }

        long getPins()
        {
            return pins;
        }

        long getUnpins()
        {
            return unpins;
        }

        public long getHits()
        {
            return hits;
        }

        long getBytesRead()
        {
            return bytesRead;
        }

        long getBytesWritten()
        {
            return bytesWritten;
        }

        long getEvictions()
        {
            return evictions;
        }

        long getFaults()
        {
            return faults;
        }

        long getFlushes()
        {
            return flushes;
        }

        public long getMerges()
        {
            return merges;
        }
    }
}
