package ru.dgolubets.neo4s.model

import org.scalatest.{Matchers, WordSpec}

/**
 * Tests default CyReads.
 */
class DefaultReadsSpec extends WordSpec with Matchers {

  "Default Reads" should {

    "read Long" in {
      CyNumber(1).as[Long] shouldBe 1
    }

    "read Int" in {
      CyNumber(1).as[Int] shouldBe 1
    }

    "read Short" in {
      CyNumber(1).as[Short] shouldBe 1
    }

    "read Byte" in {
      CyNumber(1).as[Byte] shouldBe 1
    }

    "read Double" in {
      CyNumber(3.14).as[Double] shouldBe 3.14d
    }

    "read Float" in {
      CyNumber(3.14).as[Float] shouldBe 3.14f
    }

    "read BigDecimal" in {
      CyNumber(3.14).as[BigDecimal] shouldBe 3.14
    }

    "read Boolean" in {
      CyBoolean(true).as[Boolean] shouldBe true
    }

    "read String" in {
      CyString("test").as[String] shouldBe "test"
    }

    "read Seq" in {
      CyArray(Seq(CyNumber(1), CyNumber(2))).as[Seq[Int]] shouldBe Seq(1, 2)
    }

    "return error for invalid value" in {
      CyNumber(1).asTry[String].isFailure shouldBe true
    }
  }
}
