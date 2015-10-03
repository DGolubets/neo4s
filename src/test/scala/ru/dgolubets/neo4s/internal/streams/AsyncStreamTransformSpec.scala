package ru.dgolubets.neo4s.internal.streams

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.stream.testkit.scaladsl._
import akka.testkit.TestKit
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.SpanSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import scala.concurrent.Future


class AsyncStreamTransformSpec extends WordSpec with Matchers with ScalaFutures with SpanSugar with BeforeAndAfterAll {

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  class SimpleTransformer[From, To](f: From => Seq[To]) extends AsyncStreamTransformer[From, To] {
    override def transform(data: From): Future[Seq[To]] = Future {
      f(data)
    }
  }

  "AsyncStreamTransformer" should {

    "produce expected elements" in {
      val dataTransform = () => new AsyncStreamTransform[Int, Int](new SimpleTransformer(i => List(i, i)))
      val source = Source(List(1, 2, 3)).transform(dataTransform)
      source
        .runWith(TestSink.probe[Int])
        .request(6)
        .expectNext(1, 1, 2, 2, 3, 3)
        .expectComplete()
    }

    "produce error" in {
      val error = new RuntimeException("error")
      val dataTransform = () => new AsyncStreamTransform[Int, Int](new SimpleTransformer(i => throw error))
      val source = Source(List(1, 2, 3, 4, 5)).transform(dataTransform)
      source
        .runWith(TestSink.probe[Int])
        .request(6)
        .expectError(error)
    }
  }
}
