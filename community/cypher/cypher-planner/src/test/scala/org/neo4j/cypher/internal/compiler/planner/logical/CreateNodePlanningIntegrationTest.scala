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
package org.neo4j.cypher.internal.compiler.planner.logical

import org.neo4j.cypher.internal.compiler.planner.LogicalPlanningTestSupport2
import org.neo4j.cypher.internal.ir.CreateNode
import org.neo4j.cypher.internal.logical.plans.AllNodesScan
import org.neo4j.cypher.internal.logical.plans.Apply
import org.neo4j.cypher.internal.logical.plans.Argument
import org.neo4j.cypher.internal.logical.plans.Create
import org.neo4j.cypher.internal.logical.plans.Eager
import org.neo4j.cypher.internal.logical.plans.EmptyResult
import org.neo4j.cypher.internal.util.test_helpers.CypherFunSuite

class CreateNodePlanningIntegrationTest extends CypherFunSuite with LogicalPlanningTestSupport2 {

  test("should plan single create") {
    planFor("CREATE (a)")._2 should equal(
      EmptyResult(
        Create(Argument(), List(CreateNode("a", Seq.empty, None)), Nil)
      )
    )
  }

  test("should plan for multiple creates") {
    planFor("CREATE (a), (b), (c)")._2 should equal(
      EmptyResult(
        Create(
          Argument(),
          List(
            CreateNode("a", Seq.empty, None),
            CreateNode("b", Seq.empty, None),
            CreateNode("c", Seq.empty, None)
          ),
          Nil
        )
      )
    )
  }

  test("should plan for multiple creates via multiple statements") {
    planFor("CREATE (a) CREATE (b) CREATE (c)")._2 should equal(
      EmptyResult(
        Create(
          Argument(),
          List(
            CreateNode("a",Seq.empty,None),
            CreateNode("b",Seq.empty,None),
            CreateNode("c",Seq.empty,None)
          ),
          Nil
        )
      )
    )
  }

  test("should plan single create with return") {
    planFor("CREATE (a) return a")._2 should equal(
      Create(Argument(), List(CreateNode("a", Seq.empty, None)), Nil)
    )
  }

  test("should plan create with labels") {
    planFor("CREATE (a:A:B)")._2 should equal(
      EmptyResult(
        Create(Argument(), List(CreateNode("a", Seq(labelName("A"), labelName("B")), None)), Nil)
      )
    )
  }

  test("should plan create with properties") {

    planFor("CREATE (a {prop: 42})")._2 should equal(
      EmptyResult(
        Create(
          Argument(),
          List(
            CreateNode("a", Seq.empty,
              Some(
                mapOfInt(("prop", 42))
              )
            )
          ),
          Nil
        )
      )
    )
  }

  test("should plan match and create") {
    planFor("MATCH (a) CREATE (b)")._2 should equal(
      EmptyResult(
        Create(AllNodesScan("a", Set.empty), List(CreateNode("b", Seq.empty, None)), Nil)
      )
    )
  }

  test("should plan create in tail") {
    planFor("MATCH (a) CREATE (b) WITH * MATCH(c) CREATE (d)")._2 should equal(
      EmptyResult(
        Create(
          Eager(
            Apply(
              Eager(
                Create(
                  AllNodesScan("a", Set.empty),
                  List(CreateNode("b", Seq.empty, None)), Nil
                )
              ),
              AllNodesScan("c", Set("a", "b"))
            )
          ),
          List(CreateNode("d", Seq.empty, None)), Nil
        )
      )
    )
  }
}
