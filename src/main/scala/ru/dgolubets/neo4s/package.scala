package ru.dgolubets

import ru.dgolubets.neo4s.util.CypherStringContext

import scala.language.implicitConversions

package object neo4s {

  // here I import types that should be used often
  implicit def cypherStringContext(sc: StringContext): CypherStringContext = new CypherStringContext(sc)
}
