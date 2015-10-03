package ru.dgolubets.neo4s.internal.json

import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json._
import ru.dgolubets.neo4s.model._
import ru.dgolubets.neo4s._
/**
 * Tests Cypher <-> Json conversions.
 */
class CypherFormatsSpec extends WordSpec with Matchers {

  import ImplicitFormats._

  "Implicit primitive formats" should {

    def testFormats(json: JsValue, cypher: CyValue) = {
      json.as[CyValue] shouldBe cypher
      Json.toJson(cypher) shouldBe json
    }

    "convert JsNull to CyNull and back" in {
      testFormats(JsNull, CyNull)
    }

    "convert JsNumber to CyNumber and back" in {
      testFormats(JsNumber(1), CyNumber(1))
    }

    "convert JsBoolean to CyBoolean and back" in {
      testFormats(JsBoolean(true), CyBoolean(true))
    }

    "convert JsString to CyString and back" in {
      testFormats(JsString("str"), CyString("str"))
    }

    "convert JsArray to CyArray and back" in {
      testFormats(JsArray(Seq(JsNull)), CyArray(Seq(CyNull)))
    }

    "convert JsObject to CyObject and back" in {
      testFormats(JsObject(Seq("a" -> JsNull)), CyObject(Map("a" -> CyNull)))
    }
  }

  "Implicit formats" should {

    def testReads[T: Reads](jsonStr: String, expect: T) = {
      val json = Json.parse(jsonStr)
      Json.fromJson[T](json) shouldBe JsSuccess(expect)
    }

    def testWrites[T: Writes](obj: T, jsonStr: String) = {
      val json = Json.parse(jsonStr)
      Json.toJson(obj) shouldBe json
    }

    "write CypherQuery" in {
      val stmt = CypherStatement(s"MATCH (n) WHERE n.name = {name} RETURN n",
        Map("name" -> "some name"),
        includeStats = true, includeRows = true, includeGraph = true)

      testWrites(CypherQuery(Seq(stmt)), s"""{"statements": [{ "statement" : "${stmt.statement}", "parameters":{"name": "some name"}, "includeStats": true, "resultDataContents" : [ "row", "graph" ] }]}""")
    }

    "read CypherError" in {
      testReads(
        """{"code": "erorr code", "message": "error message"}""",
        CypherError("erorr code", "error message"))
    }

    "read CypherResult" in {
      testReads(
        """{ "columns": [ "bike", "p1", "p2" ], "data": [] }""",
        CypherResult(Seq("bike", "p1", "p2"), Seq(), None))
    }

    "read CypherResultStats" in {
      testReads(
        """{
          "contains_updates" : true,
          "nodes_created" : 1,
          "nodes_deleted" : 0,
          "properties_set" : 0,
          "relationships_created" : 0,
          "relationship_deleted" : 0,
          "labels_added" : 0,
          "labels_removed" : 0,
          "indexes_added" : 0,
          "indexes_removed" : 0,
          "constraints_added" : 0,
          "constraints_removed" : 0
        }""",
        CypherResultStats(true, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0))
    }

    "read CypherResultData" in {
      testReads(
        """{ "row": [], "graph":{ "nodes": [], "relationships":[] } } """,
        CypherResultData(Some(CypherRow()), Some(CypherGraph(Seq(), Seq()))))
    }

    "read CypherGraph" in {
      testReads(
        """{ "nodes": [], "relationships":[] } """,
        CypherGraph(Seq(), Seq()))
    }

    "read CypherRow" in {
      testReads(
        """[{"weight":10}, [{"weight":10},{"position":1}], [{"weight":20},{"position":2}]]""",
        CypherRow(
          CyObject(Map("weight" -> CyNumber(10))),
          CyArray(Seq(CyObject(Map("weight" -> CyNumber(10))), CyObject(Map("position" -> CyNumber(1))))),
          CyArray(Seq(CyObject(Map("weight" -> CyNumber(20))), CyObject(Map("position" -> CyNumber(2)))))
        ))
    }

    "read CypherNode" in {
      testReads(
        """{ "id": "1", "labels": ["Movie"], "properties":{ "title": "Matrix" } }""",
        CypherNode(1, Seq("Movie"), CyObject(Map("title" -> CyString("Matrix")))))
    }

    "read CypherRelationship" in {
      testReads(
        """{ "id": "1", "type": "ACTED_IN", "startNode" : "19", "endNode" : "21", "properties":{ "year": 1999 } }""",
        CypherRelationship(1, "ACTED_IN", 19, 21, CyObject(Map("year" -> CyNumber(1999)))))
    }
  }
}
