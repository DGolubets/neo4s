package ru.dgolubets.neo4s.internal.json

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.SpanSugar
import org.scalatest.{Matchers, WordSpec}
import ru.dgolubets.neo4s.model._
import ru.dgolubets.neo4s.model.stream._

import scala.language.postfixOps

class JsonStreamTransformerSpec extends WordSpec with Matchers with ScalaFutures with SpanSugar {

  "JsonStreamTransformer" should {

    "produce the right sequence of messages" in {
      val transformer = new JsonStreamTransformer
      val json =
        """
          |{
          |  "commit" : "http://localhost:7474/db/data/transaction/9/commit",
          |  "results" : [
          |  {
          |    "columns" : [ "n" ],
          |    "data" : [ {
          |      "row" : [ {
          |        "name" : "My Node"
          |      } ]
          |    }],
          |     "stats" : {
          |      "contains_updates" : true,
          |      "nodes_created" : 1,
          |      "nodes_deleted" : 0,
          |      "properties_set" : 0,
          |      "relationships_created" : 0,
          |      "relationship_deleted" : 0,
          |      "labels_added" : 0,
          |      "labels_removed" : 0,
          |      "indexes_added" : 0,
          |      "indexes_removed" : 0,
          |      "constraints_added" : 0,
          |      "constraints_removed" : 0
          |    }
          |  },
          |  {
          |    "columns" : [ "n1" ],
          |    "data" : [ {
          |      "row" : [ {
          |        "name" : "My Node1"
          |      } ],
          |      "graph" : {
          |        "nodes" : [ {
          |          "id" : "19",
          |          "labels" : [ "Bike" ],
          |          "properties" : {
          |            "weight" : 10
          |          }
          |        } ],
          |        "relationships" : [ {
          |          "id" : "9",
          |          "type" : "HAS",
          |          "startNode" : "19",
          |          "endNode" : "20",
          |          "properties" : {
          |            "position" : 1
          |          }
          |        } ]
          |      }
          |    },
          |    {
          |      "row" : [ {
          |        "name" : "My Node2"
          |      } ]
          |    }]
          |  }
          |  ],
          |  "transaction" : {
          |    "expires" : "Fri, 16 Oct 2015 22:19:06 +0000"
          |  },
          |  "errors" : [
          |   { "code": "123", "message": "456"}
          |  ]
          |}
        """.stripMargin
      val res = transformer.transform(json)

      whenReady(res, timeout(4 seconds)) { messages =>
        messages should contain theSameElementsInOrderAs List(
          CypherStreamCommit("http://localhost:7474/db/data/transaction/9/commit"),
          CypherStreamResultColumns(0, List("n")),
          CypherStreamResultData(0, Some(CypherRow(CyObject(Map("name" -> CyString("My Node")))) ), None),
          CypherStreamResultStats(0, CypherResultStats(true, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)),
          CypherStreamResultColumns(1, List("n1")),
          CypherStreamResultData(1,
            Some(CypherRow(CyObject(Map("name" -> CyString("My Node1")))) ),
            Some(CypherGraph(
              Seq(CypherNode(19, Seq("Bike"), CyObject(Map("weight" -> CyNumber(10))))),
              Seq(CypherRelationship(9, "HAS", 19, 20, CyObject(Map("position" -> CyNumber(1)))))
            ))),
          CypherStreamResultData(1, Some(CypherRow(CyObject(Map("name" -> CyString("My Node2")))) ), None),
          CypherStreamError("123", "456")
        )
      }
    }
  }
}
