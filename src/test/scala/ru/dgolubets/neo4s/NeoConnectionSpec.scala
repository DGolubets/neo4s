package ru.dgolubets.neo4s

import akka.http.scaladsl.model.{HttpMethods, Uri}
import akka.stream.scaladsl._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.SpanSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import play.api.libs.json.JsString
import ru.dgolubets.neo4s.internal.UnderlyingConnection
import ru.dgolubets.neo4s.internal.json.JsonSerializer
import ru.dgolubets.neo4s.model._

import scala.language.postfixOps

class NeoConnectionSpec extends WordSpec with Matchers with ScalaFutures with SpanSugar with BeforeAndAfterAll with MockFactory {

  val driver = NeoDriver()

  trait ConnectionTest {
    val connection = mock[UnderlyingConnection]
    val serializer = mock[JsonSerializer]
    val uri = Uri("/db/data/transaction/commit")

    (connection.driver _).expects().anyNumberOfTimes().returns(driver)

    val context = new NeoConnection(connection)(serializer)
  }

  "NeoConnection" should {

    "return single query result" in new ConnectionTest {
      val statement = CypherStatement("")
      val requestJson = JsString("request")
      val responseString = "response"
      val result = CypherResult(Nil, Nil, None)
      val response = CypherQueryResponse(None, Seq(result), Nil)

      (serializer.formatRequest _).expects(CypherQuery(Seq(statement))).returns(requestJson)
      (serializer.parseResponse _).expects(responseString).returns(response)
      (connection.request _).expects(requestJson.toString, uri, HttpMethods.POST).once().returns(Source(List(responseString)))

      val res = context.run(statement)
      whenReady(res, timeout(4 seconds)) { r =>
        r shouldBe result
      }
    }

    "return multiple queries results" in new ConnectionTest {
      val statements = Seq(CypherStatement(""), CypherStatement(""))
      val requestJson = JsString("request")
      val responseString = "response"
      val results = Seq(
        CypherResult(Nil, Nil, None),
        CypherResult(Nil, Nil, None))
      val response = CypherQueryResponse(None, results, Nil)

      (serializer.formatRequest _).expects(CypherQuery(statements)).returns(requestJson)
      (serializer.parseResponse _).expects(responseString).returns(response)
      (connection.request _).expects(requestJson.toString, uri, HttpMethods.POST).once().returns(Source(List(responseString)))

      val res = context.run(statements: _*)
      whenReady(res, timeout(4 seconds)) { r =>
        r.seq shouldBe results
      }
    }

    "return error" in new ConnectionTest {
      val statements = Seq(CypherStatement(""), CypherStatement(""))
      val requestJson = JsString("request")
      val responseString = "response"
      val errors = Seq(CypherError("Error code", "Error message"))
      val response = CypherQueryResponse(None, Nil, errors)

      (serializer.formatRequest _).expects(CypherQuery(statements)).returns(requestJson)
      (serializer.parseResponse _).expects(responseString).returns(response)
      (connection.request _).expects(requestJson.toString, uri, HttpMethods.POST).once().returns(Source(List(responseString)))

      val res = context.run(statements: _*)
      whenReady(res.failed, timeout(4 seconds)) { r =>
        r.getMessage shouldBe errors.head.message
      }
    }
  }

  override def afterAll: Unit = {
    driver.shutdown()
  }
}
