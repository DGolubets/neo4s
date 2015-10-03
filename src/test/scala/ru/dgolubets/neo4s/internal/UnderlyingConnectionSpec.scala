package ru.dgolubets.neo4s.internal

import akka.http.scaladsl.model._
import akka.stream.scaladsl.Source
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.SpanSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import ru.dgolubets.neo4s.NeoDriver

import scala.util._

class UnderlyingConnectionSpec extends WordSpec with Matchers with ScalaFutures with SpanSugar with BeforeAndAfterAll {

  val driver = NeoDriver()

  class ConnectionFailingResponses extends UnderlyingConnection(driver, "localhost", 1, None) {
    override protected def requestStream(request: HttpRequest): Source[Try[HttpResponse], Unit] =
      Source.single(Failure(new RuntimeException("Some error")))
  }

  "UnderlyingConnection request" should {

    "return error on HTTP error" in {

      val connection = new ConnectionFailingResponses

      import driver.materializer

      val result = connection.request("", "/fakeuri").runForeach{_ => }
      whenReady(result.failed, timeout(4 seconds)){ _ =>
        // should fail
      }
    }
  }

  override def afterAll: Unit = {
    driver.shutdown()
  }
}
