package ru.dgolubets.neo4s

import ru.dgolubets.neo4s.internal.UnderlyingConnection

/**
  * Executes Neo4j queries without transaction.
  * @param connection underlying connection
  */
class NeoConnection private[neo4s](connection: UnderlyingConnection) extends NeoContext(connection) {

  /**
   * Starts a transaction.
   * @return
   */
  def beginTransaction(): NeoTransaction = new NeoTransaction(connection)
}
