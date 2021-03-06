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
package org.neo4j.internal.index.label;

import org.neo4j.collection.PrimitiveLongResourceIterator;
import org.neo4j.io.pagecache.tracing.cursor.PageCursorTracer;

/**
 * Reader of a token scan store containing token-->entities mappings.
 */
public interface TokenScanReader
{
    /**
     * Used as a marker to ignore the "fromId" in calls to {@link #entitiesWithAnyOfTokens(long, int[], PageCursorTracer)}.
     */
    long NO_ID = -1;

    /**
     * @param tokenId token id.
     * @param cursorTracer underlying page cursor tracer
     * @return entity ids with the given {@code tokenId}.
     */
    PrimitiveLongResourceIterator entitiesWithToken( int tokenId, PageCursorTracer cursorTracer );

    /**
     * Sets the client up for a token scan on <code>tokenId</code>
     *
     * @param tokenId token id
     */
    TokenScan entityTokenScan( int tokenId, PageCursorTracer cursorTracer );

    /**
     * @param tokenIds token ids.
     * @param cursorTracer underlying page cursor tracer
     * @return entity ids with any of the given token ids.
     */
    default PrimitiveLongResourceIterator entitiesWithAnyOfTokens( int[] tokenIds, PageCursorTracer cursorTracer )
    {
        return entitiesWithAnyOfTokens( NO_ID, tokenIds, cursorTracer );
    }

    /**
     * @param fromId entity id to start at, exclusive, i.e. the given {@code fromId} will not be included in the result.
     * @param tokenIds token ids.
     * @param cursorTracer underlying page cursor tracer
     * @return entity ids with any of the given token ids.
     */
    PrimitiveLongResourceIterator entitiesWithAnyOfTokens( long fromId, int[] tokenIds, PageCursorTracer cursorTracer );
}
