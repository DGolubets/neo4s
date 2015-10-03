package ru.dgolubets.neo4s.model

import scala.collection.generic
import scala.language.higherKinds
import scala.util._

/**
 * Value reader.
 * @tparam T type to read
 */
trait CyReads[T] {
  def read(value: CyValue): Try[T]
}

/**
 * Default reads.
 */
object CyReads {

  implicit object BigDecimalReads extends CyReads[BigDecimal]{
    override def read(value: CyValue): Try[BigDecimal] = value match {
      case CyNumber(n) => Success(n)
      case _ => Failure(new RuntimeException("Not a number"))
    }
  }

  implicit object LongReads extends CyReads[Long]{
    override def read(value: CyValue): Try[Long] = value match {
      case CyNumber(n) => Success(n.toLong)
      case _ => Failure(new RuntimeException("Not a number"))
    }
  }

  implicit object IntReads extends CyReads[Int]{
    override def read(value: CyValue): Try[Int] = value match {
      case CyNumber(n) => Success(n.toInt)
      case _ => Failure(new RuntimeException("Not a number"))
    }
  }

  implicit object ShortReads extends CyReads[Short]{
    override def read(value: CyValue): Try[Short] = value match {
      case CyNumber(n) => Success(n.toShort)
      case _ => Failure(new RuntimeException("Not a number"))
    }
  }

  implicit object ByteReads extends CyReads[Byte]{
    override def read(value: CyValue): Try[Byte] = value match {
      case CyNumber(n) => Success(n.toByte)
      case _ => Failure(new RuntimeException("Not a number"))
    }
  }

  implicit object DoubleReads extends CyReads[Double]{
    override def read(value: CyValue): Try[Double] = value match {
      case CyNumber(n) => Success(n.toDouble)
      case _ => Failure(new RuntimeException("Not a number"))
    }
  }

  implicit object FloatReads extends CyReads[Float]{
    override def read(value: CyValue): Try[Float] = value match {
      case CyNumber(n) => Success(n.toFloat)
      case _ => Failure(new RuntimeException("Not a number"))
    }
  }

  implicit object StringReads extends CyReads[String]{
    override def read(value: CyValue): Try[String] = value match {
      case CyString(str) => Success(str)
      case _ => Failure(new RuntimeException("Not a string"))
    }
  }

  implicit object BooleanReads extends CyReads[Boolean]{
    override def read(value: CyValue): Try[Boolean] = value match {
      case CyBoolean(str) => Success(str)
      case _ => Failure(new RuntimeException("Not a string"))
    }
  }

  implicit def traversableReads[F[_], A](implicit bf: generic.CanBuildFrom[F[_], A, F[A]], ra: CyReads[A]) = new CyReads[F[A]] {
    override def read(value: CyValue): Try[F[A]] = value match {
      case CyArray(values) =>
        Try {
          val b = bf()
          for (v <- values) {
            b += ra.read(v).get
          }
          b.result()
        }
      case _ => Failure(new RuntimeException("Not an array"))
    }
  }
}