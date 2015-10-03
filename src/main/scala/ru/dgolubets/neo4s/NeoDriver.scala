package ru.dgolubets.neo4s

import akka.actor._
import akka.stream._
import akka.http.scaladsl._
import ru.dgolubets.neo4s.internal.UnderlyingConnection

import scala.concurrent.Future

/**
 * Neo4j driver.
 */
class NeoDriver extends AutoCloseable {
  /**
   * Actor system for Akka Http
   */
  implicit val system = ActorSystem("ru_dgolubets_neo4j")

  /**
   * Default stream materializer
   */
  implicit val materializer: Materializer = ActorMaterializer()

  // execution context
  import system.dispatcher

  /**
   * Connects to a specified Neo4j instance.
   * @param host host name
   * @param port port number
   * @return connection
   */
  def connection(host: String, port: Int): NeoConnection = {
    new NeoConnection(new UnderlyingConnection(this, host, port, None))
  }

  /**
   * Connects to a specified Neo4j instance.
   * @param host host name
   * @param port port number
   * @param username username
   * @param password password
   * @return connection
   */
  def connection(host: String, port: Int, username: String, password: String): NeoConnection = {
    new NeoConnection(new UnderlyingConnection(this, host, port, Some(NeoCredentials(username, password))))
  }

  /**
   * Shutdowns the driver.
   */
  def shutdown(): Unit =  {
    Http().shutdownAllConnectionPools().onComplete { _ =>
      system.shutdown()
    }
  }

  override def close(): Unit = shutdown()
}

/**
 * Driver companion.
 */
object NeoDriver {
  def apply() = new NeoDriver
}
