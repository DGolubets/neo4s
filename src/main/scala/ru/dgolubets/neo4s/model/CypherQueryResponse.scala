package ru.dgolubets.neo4s.model

case class CypherError(code: String, message: String)

case class CypherResult(columns: Seq[String], data: Seq[CypherResultData], stats: Option[CypherResultStats])

case class CypherResultData(row: Option[CypherRow], graph: Option[CypherGraph])

case class CypherResultStats(containsUpdates: Boolean,
                             nodesCreated: Int,
                             nodesDeleted: Int,
                             propertiesSet: Int,
                             relationshipsCreated: Int,
                             relationshipDeleted: Int,
                             labelsAdded: Int,
                             labelsRemoved: Int,
                             indexesAdded: Int,
                             indexesRemoved: Int,
                             constraintsAdded: Int,
                             constraintsRemoved: Int)

case class CypherGraph(nodes: Seq[CypherNode], relationships: Seq[CypherRelationship])

case class CypherNode(id: Long, labels: Seq[String], properties: CyObject)

case class CypherRelationship(id: Long, tpe: String, startNode: Long, endNode: Long, properties: CyObject)

case class CypherRow(seq: CyValue*) {
  override def toString: String = seq.mkString("CypherRow(", ", ", ")")
}