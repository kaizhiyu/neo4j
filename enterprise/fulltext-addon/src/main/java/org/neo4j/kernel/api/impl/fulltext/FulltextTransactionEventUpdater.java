/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.api.impl.fulltext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;

import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.logging.Log;

class FulltextTransactionEventUpdater implements TransactionEventHandler<Object>
{
    private final FulltextProvider fulltextProvider;
    private final Log log;
    private final FulltextUpdateApplier applier;

    FulltextTransactionEventUpdater( FulltextProvider fulltextProvider, Log log,
                                     FulltextUpdateApplier applier )
    {
        this.fulltextProvider = fulltextProvider;
        this.log = log;
        this.applier = applier;
    }

    @Override
    public Object beforeCommit( TransactionData data ) throws Exception
    {
        Map<Long,Map<String,Object>> nodeMap = new HashMap<Long,Map<String,Object>>();
        Map<Long,Map<String,Object>> relationshipMap = new HashMap<Long,Map<String,Object>>();
        Lock lock = fulltextProvider.readLockIndexConfiguration();
        FulltextTransactioncontext fulltextTransactioncontext = new FulltextTransactioncontext( nodeMap, relationshipMap, lock );

        String[] nodeProperties = fulltextProvider.getNodeProperties();
        data.removedNodeProperties().forEach( propertyEntry ->
        {
            try
            {
                nodeMap.put( propertyEntry.entity().getId(), propertyEntry.entity().getProperties( nodeProperties ) );
            }
            catch ( NotFoundException e )
            {
                //This means that the node was deleted.
            }
        } );
        data.assignedNodeProperties().forEach(
                propertyEntry -> nodeMap.put( propertyEntry.entity().getId(),
                        propertyEntry.entity().getProperties( nodeProperties ) ) );

        String[] relationshipProperties = fulltextProvider.getRelationshipProperties();
        data.removedRelationshipProperties().forEach( propertyEntry ->
        {
            try
            {
                relationshipMap.put( propertyEntry.entity().getId(),
                        propertyEntry.entity().getProperties( relationshipProperties ) );
            }
            catch ( NotFoundException e )
            {
                //This means that the relationship was deleted.
            }
        } );
        data.assignedRelationshipProperties().forEach(
                propertyEntry -> relationshipMap.put( propertyEntry.entity().getId(),
                        propertyEntry.entity().getProperties( relationshipProperties ) ) );
        return fulltextTransactioncontext;
    }

    @Override
    public void afterCommit( TransactionData data, Object state )
    {
        RuntimeException applyException = null;
        List<AsyncFulltextIndexOperation> completions = new ArrayList<>();
        FulltextTransactioncontext context = (FulltextTransactioncontext) state;
        try
        {
            try
            {
                Map<Long,Map<String,Object>> nodeMap = context.getNodeMap();
                Map<Long,Map<String,Object>> relationshipMap = context.getRelationshipMap();

                //update node indices
                for ( WritableFulltext nodeIndex : fulltextProvider.writableNodeIndices() )
                {
                    completions.add( applier.removePropertyData( data.removedNodeProperties(), nodeMap, nodeIndex ) );
                    completions.add( applier.updatePropertyData( nodeMap, nodeIndex ) );
                }

                //update relationship indices
                for ( WritableFulltext relationshipIndex : fulltextProvider.writableRelationshipIndices() )
                {
                    completions.add( applier.removePropertyData( data.removedRelationshipProperties(), relationshipMap, relationshipIndex ) );
                    completions.add( applier.updatePropertyData( relationshipMap, relationshipIndex ) );
                }
            }
            catch ( IOException e )
            {
                applyException = new RuntimeException( "Failed to submit all index updates.", e );
            }

            for ( AsyncFulltextIndexOperation completion : completions )
            {
                try
                {
                    completion.awaitCompletion();
                }
                catch ( ExecutionException e )
                {
                    if ( applyException == null )
                    {
                        applyException = new RuntimeException( "Failed to update fulltext index. See suppressed exceptions for details." );
                    }
                    applyException.addSuppressed( e );
                }
            }
            if ( applyException != null )
            {
                throw applyException;
            }
        }
        finally
        {
            context.release();
        }
    }

    @Override
    public void afterRollback( TransactionData data, Object state )
    {
        FulltextTransactioncontext context = (FulltextTransactioncontext) state;
        context.release();
    }

    private static class FulltextTransactioncontext
    {
        private final Map<Long,Map<String,Object>> nodeMap;
        private final Map<Long,Map<String,Object>> relationshipMap;
        private final Lock lock;

        private FulltextTransactioncontext( Map<Long,Map<String,Object>> nodeMap, Map<Long,Map<String,Object>> relationshipMap, Lock lock )
        {
            this.nodeMap = nodeMap;
            this.relationshipMap = relationshipMap;
            this.lock = lock;
        }

        public Map<Long,Map<String,Object>> getRelationshipMap()
        {
            return relationshipMap;
        }

        public Map<Long,Map<String,Object>> getNodeMap()
        {
            return nodeMap;
        }

        public void release()
        {
            lock.unlock();
        }
    }
}
