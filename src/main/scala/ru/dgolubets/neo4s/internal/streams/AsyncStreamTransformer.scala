package ru.dgolubets.neo4s.internal.streams

import scala.concurrent.Future

/**
 * Describes async transform.
 */
trait AsyncStreamTransformer[In, Out] {
  /**
   * Takes a value and produces a list of values asynchronously.
   * Should produce an empty list is more data required.
   *
   * @param data
   * @return
   */
  def transform(data: In): Future[Seq[Out]]
}
