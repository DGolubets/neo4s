package ru.dgolubets.neo4s

import akka.http.scaladsl.model.Uri
import akka.stream.scaladsl._
import play.api.libs.json._
import ru.dgolubets.neo4s.internal._
import ru.dgolubets.neo4s.internal.streams.Flows
import ru.dgolubets.neo4s.model.{CypherQuery, CypherStatement, CypherResult}
import ru.dgolubets.neo4s.model.stream.{CypherStreamMessage, CypherStreamError, CypherStreamCommit}
import scala.concurrent.Future

/**
 * Executes Neo4j queries.
 * @param connection connection associated with the context
 * @param transaction transaction associated with the context (if any)
 */
class NeoContext private[neo4s](protected val connection: UnderlyingConnection,
                                protected val transaction: Option[UnderlyingTransaction] = None) extends Logging {
  import json.ImplicitFormats._

  protected implicit val executionContext = connection.driver.system.dispatcher
  protected implicit val materializer = connection.driver.materializer

  /**
   * Executes a query immediately.
   * @param statement Cypher statement
   * @return aggregated results
   */
  def run(statement: CypherStatement): Future[CypherResult] = run(Seq(statement): _*).map(_.head)

  /**
   * Executes a query immediately and returns aggregated results.
   * @param statements Cypher statements
   * @return aggregated results
   */
  def run(statements: CypherStatement*): Future[Seq[CypherResult]] = runCypher(CypherQuery(statements))

  /**
   * Streams a query results back to a client.
   * @param statements Cypher statements
   * @return stream of results
   */
  def stream(statements: CypherStatement*): Source[CypherStreamMessage, Unit] = streamCypher(CypherQuery(statements))

  /**
   * Base method for executing a query and aggregating results.
   * @param query query
   * @param commit is commit query
   * @return aggregated results
   */
  protected def runCypher(query: CypherQuery, commit: Boolean = false): Future[Seq[CypherResult]] = {
    val result = streamString(query, commit).runFold(""){(a, b) => a + b }.map { jsonString =>
      log.trace(s"Cypher result: $jsonString")

      val json = Json.parse(jsonString)

      // if has errors - fail transaction and this request
      (json \ "errors").as[List[Map[String, String]]].map { error =>
        val exc = new RuntimeException(error("message"))
        throw exc
      }

      // parse transaction id
      transaction.map { t =>
        (json \ "commit").asOpt[String].map { uri =>
          t.tryInitialize(parseTransactionId(uri))
        }
      }

      // parse results
      (json \ "results").as[List[CypherResult]]
    }

    result.onFailure {
      case exc => transaction.map(_.tryFail(exc))
    }

    if(commit) {
      transaction.map { t =>
        result.onSuccess {
          case _ => t.tryCommit()
        }
      }
    }

    result
  }

  /**
   * Base method for executing a query and streaming results.
   * @param query query
   * @param commit is commit query
   * @return stream of results
   */
  protected def streamCypher(query: CypherQuery, commit: Boolean = false): Source[CypherStreamMessage, Unit] = {
    streamString(query, commit).via(Flows.jsonFlow).map { result =>
      result match {
        case CypherStreamCommit(uri) =>
          transaction.map(_.tryInitialize(parseTransactionId(uri)))
        case CypherStreamError(_, msg) =>
          transaction.map(_.tryFail(new RuntimeException(msg)))
        case _ =>
      }

      if(commit){
        // when streaming, commit transaction instantly on request, even if errors are further in the stream
        transaction.map(_.tryCommit())
      }

      result
    }.collect {
      case m: CypherStreamMessage => m
    }
  }

  /**
   * Lowest level request stream.
   * @param query query
   * @param commit is commit query
   * @return stream of json string parts
   */
  private def streamString(query: CypherQuery, commit: Boolean): Source[String, Unit] = {
    Source(requestUri(commit)).map { uri =>
      // prepare the request
      val jsonRequest = Json.toJson(query)
      connection.request(jsonRequest.toString(), uri)
    }.flatten(FlattenStrategy.concat)
  }

  /**
   * Resolves a request URI
   * @param commit is commit request
   * @return
   */
  protected def requestUri(commit: Boolean): Future[Uri]= {
    transaction match {
      case None => Future.successful("/db/data/transaction/commit")
      case Some(t) =>
        // multiple run requests may access the transaction in parallel
        // so lock it to safely access it's state

        if(t.tryInitializing()){
          // if we got here, this request is responsible for obtaining transaction id
          Future.successful(s"/db/data/transaction")
        }
        else {
          // here the transaction has been picked up by other request
          // just wait till we have an id

          // if transaction has failed, id will fail too along with all following requests
          if(commit)
            t.id.map(id => s"/db/data/transaction/$id/commit")
          else
            t.id.map(id => s"/db/data/transaction/$id")
        }
    }
  }

  /**
   * Extracts transaction id from url.
   * Throws on error.
   *
   * @param uri commit uri
   * @return
   */
  private def parseTransactionId(uri: String): Long = {
    val pattern = """.+/(\d+)\/(?:commit)?$""".r
    uri match {
      case pattern(id) =>
        id.toLong
      case _ =>
        sys.error(s"Could not parse a transaction id from URI: $uri")
    }
  }
}



