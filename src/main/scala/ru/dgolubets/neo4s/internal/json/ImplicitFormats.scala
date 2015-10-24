package ru.dgolubets.neo4s.internal.json

import play.api.libs.functional.syntax._
import play.api.libs.json._
import ru.dgolubets.neo4s.model._

/**
 * Json implicits for parsing and serializing model
 */
private object ImplicitFormats extends CyFormats {

  // reads

  implicit lazy val responseReads: Reads[CypherQueryResponse] = (
      (JsPath \ "commit").readNullable[String] and
      (JsPath \ "results").read[List[CypherResult]] and
      (JsPath \ "errors").read[List[CypherError]]
    )(CypherQueryResponse.apply _)

  implicit lazy val errorReads: Reads[CypherError] = (
      (JsPath \ "code").read[String] and
      (JsPath \ "message").read[String]
    )(CypherError.apply _)

  implicit lazy val resultReads: Reads[CypherResult] = (
      (JsPath \ "columns").read[List[String]] and
      (JsPath \ "data").read[List[CypherResultData]] and
      (JsPath \ "stats").readNullable[CypherResultStats]
    )(CypherResult.apply _)

  implicit lazy val resultStatsReads: Reads[CypherResultStats] = (
      (JsPath \ "contains_updates").read[Boolean] and
      (JsPath \ "nodes_created").read[Int] and
      (JsPath \ "nodes_deleted").read[Int] and
      (JsPath \ "properties_set").read[Int] and
      (JsPath \ "relationships_created").read[Int] and
      (JsPath \ "relationship_deleted").read[Int] and
      (JsPath \ "labels_added").read[Int] and
      (JsPath \ "labels_removed").read[Int] and
      (JsPath \ "indexes_added").read[Int] and
      (JsPath \ "indexes_removed").read[Int] and
      (JsPath \ "constraints_removed").read[Int] and
      (JsPath \ "constraints_removed").read[Int]
    )(CypherResultStats.apply _)

  implicit lazy val resultDataReads: Reads[CypherResultData] = (
      (JsPath \ "row").readNullable[CypherRow] and
      (JsPath \ "graph").readNullable[CypherGraph]
    )(CypherResultData.apply _)


  implicit lazy val resultGraphReads: Reads[CypherGraph] = (
      (JsPath \ "nodes").read[List[CypherNode]] and
      (JsPath \ "relationships").read[List[CypherRelationship]]
    )(CypherGraph.apply _)

  implicit lazy val nodeReads: Reads[CypherNode] = (
      (JsPath \ "id").read[String].map(_.toLong) and
      (JsPath \ "labels").read[List[String]] and
      (JsPath \ "properties").read[CyObject]
    )(CypherNode.apply _)

  implicit lazy val relationshipsReads: Reads[CypherRelationship] = (
      (JsPath \ "id").read[String].map(_.toLong) and
      (JsPath \ "type").read[String] and
      (JsPath \ "startNode").read[String].map(_.toLong) and
      (JsPath \ "endNode").read[String].map(_.toLong) and
      (JsPath \ "properties").read[CyObject]
    )(CypherRelationship.apply _)

  implicit lazy val resultDataRowReads: Reads[CypherRow] = new Reads[CypherRow] {
    override def reads(json: JsValue): JsResult[CypherRow] = json match {
      case arr: JsArray =>
        JsSuccess(CypherRow(arr.as[CyArray].value :_*))
      case _ =>
        JsError("Expected json array")
    }
  }

  // writes

  implicit lazy val queryWrites: Writes[CypherQuery] = new Writes[CypherQuery]{
    override def writes(o: CypherQuery): JsValue =
      Json.obj("statements" -> Json.toJson(o.statements))
  }

  implicit lazy val statementWrites: Writes[CypherStatement] = new Writes[CypherStatement] {
    override def writes(o: CypherStatement): JsValue =
      Json.obj(
        "statement" -> o.statement,
        "parameters" -> Json.toJson(o.params),
        "includeStats" -> o.includeStats,
        "resultDataContents" -> {
          var list = List[String]()

          if(o.includeGraph)
            list = "graph" :: list

          if(o.includeRows)
            list = "row" :: list

          list
        } )
  }

  implicit lazy val paramsWrites: Writes[Map[String, CypherParameterValue]] = new Writes[Map[String, CypherParameterValue]] {
    override def writes(o: Map[String, CypherParameterValue]): JsValue = JsObject(o.map { case (k,v) => (k, Json.toJson(v)) })
  }

  implicit lazy val paramWrites: Writes[CypherParameterValue] = new Writes[CypherParameterValue] {
    override def writes(o: CypherParameterValue): JsValue = Json.toJson(o.value)
  }
}
