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
package org.neo4j.consistency.checking.labelscan;

import org.neo4j.consistency.checking.CheckerEngine;
import org.neo4j.consistency.checking.RecordCheck;
import org.neo4j.consistency.checking.full.RelationshipInUseWithCorrectRelationshipTypeCheck;
import org.neo4j.consistency.report.ConsistencyReport;
import org.neo4j.consistency.store.RecordAccess;
import org.neo4j.consistency.store.synthetic.TokenScanDocument;
import org.neo4j.internal.index.label.EntityTokenRange;
import org.neo4j.io.pagecache.tracing.cursor.PageCursorTracer;

import static org.neo4j.internal.schema.PropertySchemaType.COMPLETE_ALL_TOKENS;

public class RelationshipTypeScanCheck implements RecordCheck<TokenScanDocument,ConsistencyReport.RelationshipTypeScanConsistencyReport>
{
    @Override
    public void check( TokenScanDocument record, CheckerEngine<TokenScanDocument,ConsistencyReport.RelationshipTypeScanConsistencyReport> engine,
            RecordAccess records, PageCursorTracer cursorTracer )
    {
        EntityTokenRange range = record.getEntityTokenRange();
        for ( long relationshipId : range.entities() )
        {
            long[] types = range.tokens( relationshipId );
            engine.comparativeCheck( records.relationship( relationshipId, cursorTracer ),
                    new RelationshipInUseWithCorrectRelationshipTypeCheck<>( types, COMPLETE_ALL_TOKENS, true ) );
        }
    }
}
