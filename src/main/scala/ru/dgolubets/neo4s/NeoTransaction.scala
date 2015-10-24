package ru.dgolubets.neo4s

import akka.http.scaladsl.model.HttpMethods
import akka.stream.scaladsl.Source
import ru.dgolubets.neo4s.internal.json.JsonSerializer
import ru.dgolubets.neo4s.internal.{UnderlyingConnection, UnderlyingTransaction}
import ru.dgolubets.neo4s.model.{CypherQuery, CypherStatement, CypherResult}
import ru.dgolubets.neo4s.model.stream.CypherStreamMessage

import scala.concurrent.Future

/**
 * Executes Neo4j queries in transaction.
 */
class NeoTransaction private[neo4s] (connection: UnderlyingConnection, transaction: UnderlyingTransaction = new UnderlyingTransaction)
                                    (implicit serializer: JsonSerializer = new JsonSerializer)
  extends NeoContext(connection, Some(transaction))(serializer) {

  /**
   * Commits a transaction.
   * @return
   */
  def commit(): Future[Unit] = runCypher(CypherQuery(Seq()), commit = true).map(_ => ())

  /**
   * Rollback a transaction.
   * @return
   */
  def rollback(): Future[Unit] = requestUri(false).map { uri =>
    connection.request("", uri, HttpMethods.DELETE)
  }

  /**
   * Executes a query and commits.
   * @param statement Cypher statement
   * @return aggregated query results
   */
  def runAndCommit(statement: CypherStatement): Future[CypherResult] = runCypher(CypherQuery(Seq(statement)), commit = true).map(_.head)

  /**
   * Executes a query and commits.
   * @param statements Cypher statements
   * @return aggregated query results
   */
  def runAndCommit(statements: CypherStatement*): Future[Seq[CypherResult]] = runCypher(CypherQuery(statements), commit = true)

  /**
   * Commits and streams query results.
   * @param statements Cypher statements
   * @return
   */
  def streamAndCommit(statements: CypherStatement*): Source[CypherStreamMessage, Unit] = streamCypher(CypherQuery(statements), commit = true)
}
