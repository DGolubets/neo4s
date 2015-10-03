package ru.dgolubets.neo4s.model

import scala.util.Try

/**
 * Base type for all Cypher values.
 */
sealed trait CyValue {

  /**
   * Tries to convert a value to a type.
   * @param reads implicit converter
   */
  def asTry[T](implicit reads: CyReads[T]): Try[T] = reads.read(this)

  /**
   * Tries to convert a value to a type.
   * Returns None if failed.
   */
  def asOpt[T](implicit reads: CyReads[T]): Option[T] = asTry[T].toOption

  /**
   * Converts a value to a type.
   * Throws if failed.
   */
  def as[T](implicit reads: CyReads[T]): T = asTry[T].get
}

/**
 * Primitive Cypher value.
 */
sealed trait CyPrimitive extends CyValue


/**
 * Represents a Cypher null value.
 */
case object CyNull extends CyPrimitive

/**
 * Represents a Cypher string value.
 */
case class CyString(value: String) extends CyPrimitive

/**
 * Represents a Cypher number value.
 */
case class CyNumber(value: BigDecimal) extends CyPrimitive

/**
 * Represents a Cypher boolean value.
 */
case class CyBoolean(value: Boolean) extends CyPrimitive

/**
 * Represents a Cypher array.
 */
case class CyArray(value: Seq[CyValue] = List()) extends CyValue {
  override def toString: String = value.mkString("CyArray(", ", ", ")")
}

/**
 * Represents a Cypher object.
 */
case class CyObject(value: Map[String, CyValue] = Map()) extends CyValue {
  override def toString: String = value.map{ case (k, v) => s"$k -> $v" }.mkString("CyObject(", ", ", ")")
}