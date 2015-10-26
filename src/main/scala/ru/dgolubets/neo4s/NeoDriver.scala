package ru.dgolubets.neo4s

import akka.actor._
import akka.stream._
import akka.http.scaladsl._
import ru.dgolubets.neo4s.internal.UnderlyingConnection

import scala.concurrent.Future

/**
 * Neo4j driver.
 */
class NeoDriver private[neo4s](customSystem: Option[ActorSystem]) extends AutoCloseable {

  protected var connections = List[UnderlyingConnection]()

  implicit val system = customSystem.getOrElse {
    ActorSystem("ru_dgolubets_neo4j")
  }

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
    new NeoConnection(UnderlyingConnection(this, host, port, None))
  }

  /**
   * Connects to a specified Neo4j instance.
   * @param host host name
   * @param port port number
   * @param username username
   * @param password password
   * @return connection
   */
  def connection(host: String, port: Int, username: String, password: String): NeoConnection = this.synchronized {
    val connection = UnderlyingConnection(this, host, port, Some(NeoCredentials(username, password)))

    // save connection reference
    connections = connection :: connections

    new NeoConnection(connection)
  }

  /**
   * Shutdowns the driver.
   */
  def shutdown(): Unit =  {
    Future.sequence(connections.map(_.shutdown())).onComplete { _ =>
      if(customSystem.isEmpty){
        // we own it, so shutdown
        system.shutdown()
      }
    }
  }

  override def close(): Unit = shutdown()
}

/**
 * Driver companion.
 */
object NeoDriver {

  /**
   * Creates a driver instance.
   * @return
   */
  def apply() = new NeoDriver(None)

  /**
   * Creates a driver instance with external actor system.
   * @param system actor system to use.
   * @return
   */
  def apply(system: ActorSystem) = new NeoDriver(Some(system))
}
