package ru.dgolubets.neo4s.internal


import scala.concurrent._

/**
 * Transaction states.
 */
object TransactionState extends Enumeration {
  type TransactionState = Value
  val New, Initializing, Initialized, Commited, Rolledback, Error = Value
}

/**
 * Transaction state machine.
 */
class UnderlyingTransaction {
  private var _id: Promise[Long] = Promise()
  private var _state = TransactionState.New

  def state = _state
  def id = _id.future

  /**
   * Tries to start initializing a transaction.
   * @return true if successful
   */
  def tryInitializing(): Boolean = {
    lockIfElse(_state == TransactionState.New){
      _state = TransactionState.Initializing
      true
    }(false)
  }

  /**
   * Tries to initialize transaction.
   * @param id transaction id
   * @return true if successful
   */
  def tryInitialize(id: Long): Boolean = {
    lockIfElse(_state == TransactionState.Initializing){
      _id.success(id)
      _state = TransactionState.Initialized
      true
    }(false)
  }

  /**
   * Tries to commit transaction.
   * @return true if successful
   */
  def tryCommit(): Boolean = {
    lockIfElse(_state == TransactionState.Initialized){
      _state = TransactionState.Commited
      true
    }(false)
  }

  /**
   * Tries to rollback transaction.
   * @return true if successful
   */
  def tryRollback(): Boolean = {
    lockIfElse(_state == TransactionState.Initialized){
      _state = TransactionState.Rolledback
      true
    }(false)
  }

  /**
   * Tries to fail transaction.
   * @param exc error
   * @return true if successful
   */
  def tryFail(exc: Throwable): Boolean = {
    lockIfElse(_state != TransactionState.Error && _state != TransactionState.Commited && _state != TransactionState.Rolledback){
      if(_state == TransactionState.Initialized){
        _id = Promise.failed(exc)
      }
      else {
        _id.failure(exc)
      }
      _state = TransactionState.Error
      true
    }(false)
  }

  /**
   * Executes if-else blocks in thread safe way based on a condition.
   * @param condition if condition
   * @param ifBlock if block
   * @param elseBlock else block
   * @tparam T return type
   * @return if block expression result
   */
  def lockIfElse[T](condition: => Boolean)(ifBlock: => T)(elseBlock: => T): T = {
    if(condition){
      this.synchronized {
        if(condition) {
          ifBlock
        }
        else elseBlock
      }
    }
    else elseBlock
  }
}