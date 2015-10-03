package ru.dgolubets.neo4s.model

/**
 *
 * @param statements
 */
case class CypherQuery(statements: Seq[CypherStatement])
