package ru.dgolubets.neo4s.internal

import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.stream.scaladsl._
import ru.dgolubets.neo4s.{NeoCredentials, NeoDriver}

import scala.collection.mutable.ListBuffer
import scala.util.{Try, Failure, Success}

/**
 * Neo4j connection interface.
 */
trait UnderlyingConnection {

  /**
   * Driver managing this connection.
   * @return
   */
  def driver: NeoDriver

  /**
   * Starts a request to Neo4j endpoint.
   * @param json json to send
   * @param uri endpoint uri
   * @param method http method
   * @return stream of strings
   */
  def request(json: String, uri: Uri, method: HttpMethod = HttpMethods.POST): Source[String, Unit]
}

object UnderlyingConnection {
  def apply(driver: NeoDriver, host: String, port: Int, credentials: Option[NeoCredentials]): UnderlyingConnection =
    new UnderlyingConnectionImpl(driver, host, port, credentials)
}

/**
 * Manages an HTTP connection pool to Neo4j instance.
 * @param driver driver instance
 * @param host host name
 * @param port port number
 * @param credentials credentials if any
 */
private class UnderlyingConnectionImpl(val driver: NeoDriver, host: String, port: Int, credentials: Option[NeoCredentials])
  extends UnderlyingConnection with Logging {

  import driver.system
  import driver.materializer


  private val cachedPool = Http().cachedHostConnectionPool[Int](host, port)

  private val headers = ListBuffer[HttpHeader](
    Accept(MediaTypes.`application/json`)
  )

  credentials match {
    case Some(NeoCredentials(username, password)) =>
      headers += Authorization(BasicHttpCredentials(username, password))
    case _ =>
  }

  protected def requestStream(request: HttpRequest):  Source[Try[HttpResponse], Unit] = {
    Source.single(request -> 0).via(cachedPool).map{ case (resp, key) => resp }
  }

  /**
   * Starts a request to Neo4j endpoint.
   * @param json json to send
   * @param uri endpoint uri
   * @param method http method
   * @return stream of strings
   */
  def request(json: String, uri: Uri, method: HttpMethod = HttpMethods.POST): Source[String, Unit] = {
    log.trace(s"Cypher request: $method $uri $json")
    requestStream(HttpRequest(
      method = method,
      uri = uri,
      headers = headers.toList,
      entity = HttpEntity(ContentTypes.`application/json`, json)))
      .map {
        case Success(response) =>
          response.entity.dataBytes
        case Failure(err) =>
          Source.failed(err)
      }
      .flatten(FlattenStrategy.concat)
      .map(_.decodeString("UTF-8"))
  }
}
