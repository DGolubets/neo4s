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
import ru.dgolubets.neo4s.model.stream.{CypherStreamCommit, CypherStreamError, CypherStreamMessageBase}

import scala.concurrent.Future
import scala.language.postfixOps

class NeoTransactionSpec extends WordSpec with Matchers with ScalaFutures with SpanSugar with BeforeAndAfterAll with MockFactory {

  val driver = NeoDriver()

  trait TransactionTest {
    val serializer = mock[JsonSerializer]
    val connection = mock[UnderlyingConnection]
    val transaction = mock[UnderlyingTransaction]
    val transactionId = 10L
    val statements = Seq(CypherStatement(""), CypherStatement(""))
    val requestJson = JsString("request")
    val requestString = requestJson.toString
    val responseString = "response"
    val responseSource = Source.single(responseString)
    val commit = Some(s"http://localhost/db/data/transaction/$transactionId/commit")

    (connection.driver _).expects().anyNumberOfTimes().returns(driver)
    (serializer.formatRequest _).expects(CypherQuery(statements)).noMoreThanOnce().returns(requestJson)

    def streamMessages = List[CypherStreamMessageBase]()

    class TestNeoTransaction extends NeoTransaction(connection, transaction) {
      override protected val jsonSerializer: JsonSerializer = serializer
      override protected val jsonStreamFlow: Flow[String, CypherStreamMessageBase, Unit] =
        Flow[String].fold(streamMessages)((a, b) => a).mapConcat(l => l)
    }

    val context = new TestNeoTransaction
  }

  trait SuccessTransactionTest extends TransactionTest {
    val response = CypherQueryResponse(commit, Seq(), Nil)

    (serializer.parseResponse _).expects(responseString).noMoreThanOnce().returns(response)
  }

  trait FailureTransactionTest extends TransactionTest {
    val errors = Seq(CypherError("Error code", "Error message"))
    val response = CypherQueryResponse(None, Nil, errors)

    (serializer.parseResponse _).expects(responseString).noMoreThanOnce().returns(response)
  }

  trait SuccessStreamTransactionTest extends TransactionTest {
    override def streamMessages = List[CypherStreamMessageBase](
        CypherStreamCommit("http://localhost/db/data/transaction/10/commit")
        )
  }

  trait FailureStreamTransactionTest extends TransactionTest {
    override def streamMessages = List[CypherStreamMessageBase](
        CypherStreamCommit("http://localhost/db/data/transaction/10/commit"),
        CypherStreamError("error code", "error message")
        )
  }

  "NeoTransaction" when {

    "runs non-stream query" should {

      "initialize transaction" in new SuccessTransactionTest {
        inSequence {
          (transaction.tryInitializing _).expects().once().returning(true)
          (transaction.tryInitialize _).expects(transactionId).once().returning(true)
        }
        (connection.request _).expects(requestString, Uri("/db/data/transaction"), HttpMethods.POST).once().returns(responseSource)
        val res = context.run(statements: _*)
        whenReady(res, timeout(4 seconds)) { _ => }
      }

      "use transaction" in new SuccessTransactionTest {
        inSequence {
          (transaction.tryInitializing _).expects().once().returning(false)
          (transaction.id _).expects().once().returning(Future.successful(transactionId))
          (transaction.tryInitialize _).expects(transactionId).once().returning(false)
        }
        (connection.request _).expects(requestString, Uri(s"/db/data/transaction/$transactionId"), HttpMethods.POST).once().returns(responseSource)
        val res = context.run(statements: _*)
        whenReady(res, timeout(4 seconds)) { _ => }
      }

      "commit transaction" in new SuccessTransactionTest {
        inSequence {
          (transaction.tryInitializing _).expects().once().returning(false)
          (transaction.id _).expects().once().returning(Future.successful(transactionId))
          (transaction.tryInitialize _).expects(transactionId).once().returning(false)
          (transaction.tryCommit _).expects().once().returning(true)
        }
        (connection.request _).expects(requestString, Uri(s"/db/data/transaction/$transactionId/commit"), HttpMethods.POST).once().returns(responseSource)
        val res = context.runAndCommit(statements: _*)
        whenReady(res, timeout(4 seconds)) { _ => }
      }

      "rollback transaction" in new SuccessTransactionTest {
        inSequence {
          (transaction.tryInitializing _).expects().once().returning(false)
          (transaction.id _).expects().once().returning(Future.successful(transactionId))
        }
        (connection.request _).expects(*, Uri(s"/db/data/transaction/$transactionId"), HttpMethods.DELETE).once().returns(Source.empty)
        val res = context.rollback()
        whenReady(res, timeout(4 seconds)) { _ => }
      }

      "fail transaction" in new FailureTransactionTest {
        inSequence {
          (transaction.tryInitializing _).expects().once().returning(false)
          (transaction.id _).expects().once().returning(Future.successful(transactionId))
          (transaction.tryFail _).expects(*).once().returning(true)
        }
        (connection.request _).expects(requestString, Uri(s"/db/data/transaction/$transactionId"), HttpMethods.POST).once().returns(responseSource)
        val res = context.run(statements: _*)
        whenReady(res.failed, timeout(4 seconds)) { _ => }
      }
    }

    "runs stream query" should {

      import driver.materializer

      "initialize transaction" in new SuccessStreamTransactionTest {
        inSequence {
          (transaction.tryInitializing _).expects().once().returning(true)
          (transaction.tryInitialize _).expects(transactionId).once().returning(true)
        }
        (connection.request _).expects(requestString, Uri("/db/data/transaction"), HttpMethods.POST).once().returns(responseSource)
        val res = context.stream(statements: _*).runForeach(_ => {})
        whenReady(res, timeout(4 seconds)) { _ => }
      }


      "use transaction" in new SuccessStreamTransactionTest {
        inSequence {
          (transaction.tryInitializing _).expects().once().returning(false)
          (transaction.id _).expects().once().returning(Future.successful(transactionId))
          (transaction.tryInitialize _).expects(transactionId).once().returning(false)
        }
        (connection.request _).expects(requestJson.toString, Uri(s"/db/data/transaction/$transactionId"), HttpMethods.POST).once().returns(Source(List(responseString)))
        val res = context.stream(statements: _*).runForeach(_ => {})
        whenReady(res, timeout(4 seconds)) { _ => }
      }

      "commit transaction" in new SuccessStreamTransactionTest {
        inSequence {
          (transaction.tryInitializing _).expects().once().returning(false)
          (transaction.id _).expects().once().returning(Future.successful(transactionId))
          (transaction.tryInitialize _).expects(transactionId).once().returning(false)
          (transaction.tryCommit _).expects().once().returning(true)
        }
        (connection.request _).expects(requestJson.toString, Uri(s"/db/data/transaction/$transactionId/commit"), HttpMethods.POST).once().returns(Source(List(responseString)))
        val res = context.streamAndCommit(statements: _*).runForeach(_ => {})
        whenReady(res, timeout(4 seconds)) { _ => }
      }

      "fail transaction" in new FailureStreamTransactionTest {
        inSequence {
          (transaction.tryInitializing _).expects().once().returning(false)
          (transaction.id _).expects().once().returning(Future.successful(transactionId))
          (transaction.tryInitialize _).expects(10L).once().returning(true)
          (transaction.tryFail _).expects(*).once().returning(true)
        }
        (connection.request _).expects(*, Uri(s"/db/data/transaction/$transactionId"), HttpMethods.POST).once().returns(Source(List(responseString)))
        val res = context.stream(statements: _*).runForeach(_ => {})
        whenReady(res, timeout(4 seconds)) { _ => }
      }

    }
  }

  override def afterAll: Unit = {
    driver.shutdown()
  }
}
