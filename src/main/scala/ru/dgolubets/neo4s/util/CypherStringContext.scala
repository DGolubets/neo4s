package ru.dgolubets.neo4s.util

import ru.dgolubets.neo4s.model.{CypherParameterValue, CypherStatement}

/**
 * Cypher query interpolator.
 * @param sc string context
 */
class CypherStringContext(val sc: StringContext) extends AnyVal {

  def cypher(args: CypherParameterValue*): CypherStatement = {
    val query = sc.parts.tail.zipWithIndex.foldLeft(sc.parts.head) {
      case (acc, (next, index)) => acc + s"{$index}" + next
    }
    val params = args.zipWithIndex.map {
      case (p, i) => i.toString -> p
    }.toMap

    CypherStatement(query.mkString, params)
  }
}