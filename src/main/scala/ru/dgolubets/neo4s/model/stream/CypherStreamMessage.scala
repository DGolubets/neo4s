package ru.dgolubets.neo4s.model.stream

import ru.dgolubets.neo4s.model._

/**
 * Base trait for streamed result
 */
sealed trait CypherStreamMessageBase

/**
 * Base trait for streamed results visible to user.
 */
sealed trait CypherStreamMessage extends CypherStreamMessageBase

/**
 * Commit uri.
 * @param uri URI string
 */
case class CypherStreamCommit(uri: String) extends CypherStreamMessageBase

/**
 * Error.
 * @param code error code
 * @param message error message
 */
case class CypherStreamError(code: String, message: String) extends CypherStreamMessage

/**
 * Result columns.
 * @param resultIndex result index corresponding to statement index in the query
 * @param columns columns list
 */
case class CypherStreamResultColumns(resultIndex: Int, columns: Seq[String]) extends CypherStreamMessage

/**
 * Result rows.
 * @param resultIndex result index corresponding to statement index in the query
 * @param row result row
 * @param graph result graph
 */
case class CypherStreamResultData(resultIndex: Int, row: Option[CypherRow], graph: Option[CypherGraph]) extends CypherStreamMessage

/**
 * Result statistics.
 * @param resultIndex result index corresponding to statement index in the query
 * @param stats statistics
 */
case class CypherStreamResultStats(resultIndex: Int, stats: CypherResultStats) extends CypherStreamMessage
