package ru.dgolubets.neo4s

import akka.http.scaladsl.model.{HttpMethods, Uri}
import akka.stream.scaladsl._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.SpanSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import play.api.libs.json.JsString
import ru.dgolubets.neo4s.internal.json.JsonSerializer
import ru.dgolubets.neo4s.internal.{UnderlyingConnection, UnderlyingTransaction}
import ru.dgolubets.neo4s.model._

import scala.concurrent.Future
import scala.language.postfixOps

class NeoTransactionSpec extends WordSpec with Matchers with ScalaFutures with SpanSugar with BeforeAndAfterAll with MockFactory {

  val driver = NeoDriver()

  trait TransactionTest {
    val serializer = mock[JsonSerializer]
    val connection = mock[UnderlyingConnection]
    val transaction = mock[UnderlyingTransaction]
    val transactionId = 10L

    (connection.driver _).expects().anyNumberOfTimes().returns(driver)

    val context = new NeoTransaction(connection, transaction)(serializer)
  }

  trait SuccessTransactionTest extends TransactionTest {
    val statements = Seq(CypherStatement(""), CypherStatement(""))
    val requestJson = JsString("request")
    val responseString = "response"
    val commit = Some(s"http://localhost/db/data/transaction/$transactionId/commit")
    val response = CypherQueryResponse(commit, Seq(), Nil)

    (serializer.formatRequest _).expects(CypherQuery(statements)).noMoreThanOnce().returns(requestJson)
    (serializer.parseResponse _).expects(responseString).noMoreThanOnce().returns(response)
  }

  trait FailureTransactionTest extends TransactionTest {
    val statements = Seq(CypherStatement(""), CypherStatement(""))
    val requestJson = JsString("request")
    val responseString = "response"
    val commit = Some(s"http://localhost/db/data/transaction/$transactionId/commit")
    val errors = Seq(CypherError("Error code", "Error message"))
    val response = CypherQueryResponse(None, Nil, errors)

    (serializer.formatRequest _).expects(CypherQuery(statements)).noMoreThanOnce().returns(requestJson)
    (serializer.parseResponse _).expects(responseString).noMoreThanOnce().returns(response)
  }

  "NeoTransaction" should {

    "initialize transaction" in new SuccessTransactionTest {
      inSequence {
        (transaction.tryInitializing _).expects().once().returning(true)
        (transaction.tryInitialize _).expects(transactionId).once().returning(true)
      }
      (connection.request _).expects(requestJson.toString, Uri("/db/data/transaction"), HttpMethods.POST).once().returns(Source(List(responseString)))
      val res = context.run(statements: _*)
      whenReady(res, timeout(4 seconds)) { r =>
      }
    }

    "use transaction" in new SuccessTransactionTest {
      inSequence {
        (transaction.tryInitializing _).expects().once().returning(false)
        (transaction.id _).expects().once().returning(Future.successful(transactionId))
        (transaction.tryInitialize _).expects(transactionId).once().returning(false)
      }
      (connection.request _).expects(requestJson.toString, Uri(s"/db/data/transaction/$transactionId"), HttpMethods.POST).once().returns(Source(List(responseString)))
      val res = context.run(statements: _*)
      whenReady(res, timeout(4 seconds)) { r =>
      }
    }

    "commit transaction" in new SuccessTransactionTest {
      inSequence {
        (transaction.tryInitializing _).expects().once().returning(false)
        (transaction.id _).expects().once().returning(Future.successful(transactionId))
        (transaction.tryInitialize _).expects(transactionId).once().returning(false)
        (transaction.tryCommit _).expects().once().returning(true)
      }
      (connection.request _).expects(requestJson.toString, Uri(s"/db/data/transaction/$transactionId/commit"), HttpMethods.POST).once().returns(Source(List(responseString)))
      val res = context.runAndCommit(statements: _*)
      whenReady(res, timeout(4 seconds)) { r =>
      }
    }

    "rollback transaction" in new SuccessTransactionTest {
      inSequence {
        (transaction.tryInitializing _).expects().once().returning(false)
        (transaction.id _).expects().once().returning(Future.successful(transactionId))
      }
      (connection.request _).expects(*, Uri(s"/db/data/transaction/$transactionId"), HttpMethods.DELETE).once().returns(Source.empty)
      val res = context.rollback()
      whenReady(res, timeout(4 seconds)) { r =>
      }
    }

    "fail transaction" in new FailureTransactionTest {
      inSequence {
        (transaction.tryInitializing _).expects().once().returning(false)
        (transaction.id _).expects().once().returning(Future.successful(transactionId))
        (transaction.tryFail _).expects(*).once().returning(true)
      }
      (connection.request _).expects(*, Uri(s"/db/data/transaction/$transactionId"), HttpMethods.POST).once().returns(Source(List(responseString)))
      val res = context.run(statements: _*)
      whenReady(res.failed, timeout(4 seconds)) { r =>
      }
    }
  }

  override def afterAll: Unit = {
    driver.shutdown()
  }
}
