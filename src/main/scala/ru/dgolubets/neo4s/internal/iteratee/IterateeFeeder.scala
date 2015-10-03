package ru.dgolubets.neo4s.internal.iteratee

import play.api.libs.iteratee.{Input, Iteratee, Step}

import scala.concurrent.Future

/**
 * Helper to feed iteratees.
 * Iteratee.feed method can return unfinished iteratee when it's async.
 * But this one returns only when that iteratee reported with Cont.
 */
class IterateeFeeder[In, Mat](private var iteratee: Iteratee[In, Mat]){
  import scala.concurrent.ExecutionContext.Implicits.global
  private object syncRoot

  @volatile
  var wantsMore: Boolean = true

  /**
   * Feeds iteratee a value and signals when it's finished.
   * @param input
   * @return
   */
  def feed(input: Input[In]): Future[Boolean] = syncRoot.synchronized {
    iteratee.fold {
      case Step.Cont(next) =>
        iteratee = next(input)
        iteratee.pureFold {
          case Step.Cont(_) =>
            true
          case Step.Done(_, _) =>
            false
          case Step.Error(err, _) =>
            sys.error(err)
        }
      case Step.Done(r, left) =>
        Future.successful(false)
      case Step.Error(err, left) =>
        sys.error(err)
    }.map { more =>
      wantsMore = more
      more
    }
  }

  def feed(input: In): Future[Boolean] = feed(Input.El(input))

  def finish(): Future[Mat] = {
    feed(Input.EOF).flatMap { _ =>
      iteratee.run
    }
  }
}
