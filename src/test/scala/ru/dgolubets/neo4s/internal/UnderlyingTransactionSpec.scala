package ru.dgolubets.neo4s.internal

import org.scalatest.{Matchers, WordSpec}

import scala.util.{Failure, Success}

class UnderlyingTransactionSpec extends WordSpec with Matchers {

  "UnderlyingTransaction" should {

    "transition should be in a New state" in {
      val transaction = new UnderlyingTransaction()

      transaction.state shouldBe TransactionState.New
      transaction.id.value shouldBe None
    }

    "transition into Initializing state" in {
      val transaction = new UnderlyingTransaction()

      transaction.tryInitializing() shouldBe true
      transaction.state shouldBe TransactionState.Initializing
      transaction.id.value shouldBe None
    }

    "transition into Initialized state" in {
      val transaction = new UnderlyingTransaction()
      transaction.tryInitializing()

      val id = 10
      transaction.tryInitialize(id) shouldBe true
      transaction.state shouldBe TransactionState.Initialized
      transaction.id.value shouldBe Some(Success(id))
    }

    "transition into Failed state" in {
      val transaction = new UnderlyingTransaction()
      transaction.tryInitializing()
      transaction.tryInitialize(10)

      val error = new RuntimeException()
      transaction.tryFail(error) shouldBe true
      transaction.state shouldBe TransactionState.Error
      transaction.id.value shouldBe Some(Failure(error))
    }

    "transition into Commited state" in {
      val transaction = new UnderlyingTransaction()
      transaction.tryInitializing()
      transaction.tryInitialize(10)

      transaction.tryCommit() shouldBe true
      transaction.state shouldBe TransactionState.Commited
    }

    "transition into Rollbacked state" in {
      val transaction = new UnderlyingTransaction()
      transaction.tryInitializing()
      transaction.tryInitialize(10)

      transaction.tryRollback() shouldBe true
      transaction.state shouldBe TransactionState.Rolledback
    }

    "stay the same state if already completed" in {
      val transaction = new UnderlyingTransaction()
      transaction.tryInitializing()
      transaction.tryInitialize(10)
      transaction.tryCommit()

      transaction.tryInitializing() shouldBe false
      transaction.tryInitialize(0) shouldBe false
      transaction.tryRollback() shouldBe false
      transaction.tryFail(new RuntimeException) shouldBe false

      transaction.state shouldBe TransactionState.Commited
    }
  }
}
