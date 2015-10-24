package ru.dgolubets.neo4s.internal.json

import ru.dgolubets.neo4s.model._
import ru.dgolubets.neo4s.model.stream._
import ru.dgolubets.neo4s.internal.json.ImplicitFormats._
import ru.dgolubets.neo4s.internal.iteratee.IterateeFeeder
import ru.dgolubets.neo4s.internal.streams.AsyncStreamTransformer

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Produces streamed model from json stream.
 */
class JsonStreamTransformer extends AsyncStreamTransformer[String, CypherStreamMessageBase] {

  /**
   * Transforms a data into cypher streams.
   * @param data
   * @return
   */
  override def transform(data: String): Future[Seq[CypherStreamMessageBase]] = iterateeFeeder.feed(data).map { _ =>
    results.dequeueAll(_ => true)
  }

  private val results = mutable.Queue[CypherStreamMessageBase]()
  private val iterateeFeeder = {

    import play.api.libs.iteratee._
    import play.extras.iteratees._
    import JsonEnumeratees._
    import JsonIteratees._
    import play.api.libs.json._

    var resultIndex = 0

    def parseError(obj: JsObject): Unit = {
      val res = CypherStreamError((obj \ "code").as[String], (obj \ "message").as[String])
      results.enqueue(res)
    }

    def parseCommit(obj: JsString): Unit = {
      val res = CypherStreamCommit(obj.value)
      results.enqueue(res)
    }

    def parseResultColumns(obj: JsArray): Unit = {
      val res = CypherStreamResultColumns(resultIndex, obj.value.map(_.as[String]))
      results.enqueue(res)
    }

    def parseResultData(obj: JsObject): Unit = {
      val res = CypherStreamResultData(resultIndex, (obj \ "row").asOpt[CypherRow], (obj \ "graph").asOpt[CypherGraph])
      results.enqueue(res)
    }

    def parseResultStats(obj: JsObject): Unit = {
      val res = CypherStreamResultStats(resultIndex, obj.as[CypherResultStats])
      results.enqueue(res)
    }

    val resultIteratee: Iteratee[CharString, Unit] =
      jsObject(_ match {
        case "columns" =>
          jsSimpleArray.map(parseResultColumns)
        case "data" =>
          jsArray(jsValues(jsSimpleObject)) &>> Iteratee.foreach(parseResultData)
        case "stats" =>
          jsSimpleObject.map(parseResultStats)
        case _ =>
          jsValue
      }
      ) &>> Iteratee.skipToEof

    val rootIteratee: Iteratee[CharString, Unit] =
      jsObject(_ match {
        case "commit" =>
          jsNullOr(jsString.map(parseCommit))
        case "errors" =>
          jsArray(jsValues(jsSimpleObject)) &>> Iteratee.foreach(parseError)
        case "results" =>
          jsArray(_ => resultIteratee.map(_ => resultIndex += 1)) &>> Iteratee.skipToEof
        case _ =>
          jsValue
      }
      ) &>> Iteratee.skipToEof


    val iteratee: Iteratee[String, Unit] = Enumeratee.map[String](str => CharString.fromString(str)) &>> rootIteratee

    new IterateeFeeder(iteratee)
  }
}
