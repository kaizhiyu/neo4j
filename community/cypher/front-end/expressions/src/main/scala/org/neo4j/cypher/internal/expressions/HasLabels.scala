/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.cypher.internal.expressions

import org.neo4j.cypher.internal.util.InputPosition

case class HasLabels(expression: Expression, labels: Seq[LabelName])(val position: InputPosition) extends Expression {

  override def asCanonicalStringVal = s"${expression.asCanonicalStringVal}${labels.map(_.asCanonicalStringVal).mkString(":", ":", "")}"
}

case class HasLabelsOrTypes(expression: Expression, labelsOrTypes: Seq[LabelOrRelTypeName])(val position: InputPosition) extends Expression {

  override def asCanonicalStringVal = s"${expression.asCanonicalStringVal}${labelsOrTypes.map(_.asCanonicalStringVal).mkString(":", ":", "")}"
}

case class HasTypes(expression: Expression, types: Seq[RelTypeName])(val position: InputPosition) extends Expression {

  override def asCanonicalStringVal = s"${expression.asCanonicalStringVal}${types.map(_.asCanonicalStringVal).mkString(":", ":", "")}"
}
