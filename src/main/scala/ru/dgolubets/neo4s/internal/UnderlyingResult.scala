package ru.dgolubets.neo4s.internal

import akka.stream.scaladsl.Source

/**
 * Created by Dima on 27.09.2015.
 */
class UnderlyingResult(val stream: Source[String, Unit])
