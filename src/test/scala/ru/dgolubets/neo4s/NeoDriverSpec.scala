package ru.dgolubets.neo4s

import akka.actor.ActorSystem
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.SpanSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import ru.dgolubets.neo4s.internal.UnderlyingConnection

import scala.concurrent.{Future, TimeoutException}
import scala.language.postfixOps

class NeoDriverSpec extends WordSpec with Matchers with ScalaFutures with SpanSugar with BeforeAndAfterAll with MockFactory {

  "NeoDriver" should {

    "close connections on shutdown" in {
      val mockConnections = List(
        mock[UnderlyingConnection],
        mock[UnderlyingConnection])

      for (c <- mockConnections) {
        (c.shutdown _).expects().once().returns(Future.successful(()))
      }

      class TestDriver extends NeoDriver(None) {
        connections = mockConnections
      }
      val driver = new TestDriver()
      driver.shutdown()
    }

    "shutdown its own actor system" in {
      val driver = NeoDriver()
      driver.shutdown()
      driver.system.awaitTermination(2.seconds)
      driver.system.isTerminated shouldBe true
    }

    "keep alive external actor system" in {
      val system = ActorSystem()
      val driver = NeoDriver(system)
      driver.shutdown()
      intercept[TimeoutException] {
        system.awaitTermination(2.seconds)
      }
      system.shutdown()
    }
  }
}
