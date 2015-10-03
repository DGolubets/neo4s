package ru.dgolubets.neo4s.model

import org.scalatest.{Matchers, WordSpec}
import ru.dgolubets.neo4s._

/**
 * Tests cypher statement parameters.
 */
class CypherParametersSpec extends WordSpec with Matchers {

  "Cypher parameter implicit conversion" should {

    def testParameter[T](p: T, expect: CyValue)(implicit conv: T => CypherParameterValue): Unit = {
      val stmt = cypher"""$p"""
      stmt.params.values.head.value shouldEqual expect
    }

    "accept Long" in {
      testParameter(1: Long, CyNumber(1))
    }

    "accept Int" in {
      testParameter(1: Int, CyNumber(1))
    }

    "accept Short" in {
      testParameter(1: Short, CyNumber(1))
    }

    "accept Byte" in {
      testParameter(1: Byte, CyNumber(1))
    }

    "accept Boolean" in {
      testParameter(true, CyBoolean(true))
    }

    "accept String" in {
      testParameter("abc", CyString("abc"))
    }

    "accept Option" in {
      testParameter(Some(1), CyNumber(1))
    }

    "accept primitive array" in {
      testParameter(Seq(1, 2 ,3), CyArray(Seq(CyNumber(1), CyNumber(2) ,CyNumber(3))))
    }

    "accept a set of properties" in {
      testParameter(
        CypherProperties("a" -> 1, "b" -> "test", "c" -> Seq(1, 2, 3)),
        CyObject(Map("a" -> CyNumber(1), "b" -> CyString("test"), "c" -> CyArray(Seq(CyNumber(1), CyNumber(2), CyNumber(3))))))
    }

    "deny invalid types" in {
      // todo: it should be done via reflection emit, cos invalid types won't compile
      /*
      The following should fail at compile time:

      cypher"""${ CypherProperties("a" -> 1, "b" -> CypherProperties("a" -> 1)) }""" // nested properties
      cypher"""${ Seq(1, 2, "3") }""" // heterogeneous array
      */
    }
  }
}
