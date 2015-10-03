package ru.dgolubets.neo4s.internal.streams

import akka.stream.scaladsl._
import ru.dgolubets.neo4s.internal.json.JsonStreamTransformer

/**
 * Created by Dima on 28.09.2015.
 */
object Flows {
  lazy val jsonFlow = Flow[String].transform(() => new AsyncStreamTransform(new JsonStreamTransformer))
}
