package ru.dgolubets.neo4s.model

/**
 * Cypher query statement.
 * @param statement query string
 * @param params query parameters
 */
case class CypherStatement(statement: String, params: Map[String, CypherParameterValue] = Map(),
                           includeStats: Boolean = false,
                           includeRows: Boolean = true,
                           includeGraph: Boolean = false){

  def withStats = copy(includeStats = true)
  def withGraph = copy(includeGraph = true)
}
