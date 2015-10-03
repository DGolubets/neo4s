package ru.dgolubets.neo4s.model

/**
 * Parameters factory.
 * Has a lot of implicits for easy parameters creation.
 * The main goal it to type check parameters shape at compile type.
 */
object CypherParameterValue {
  def apply(value: CyPrimitive) = new CypherPrimitiveParameterValue(value)
  def apply(value: Seq[CyPrimitive]) = new CypherPrimitiveArrayParameterValue(value)
  def apply(value: CypherProperties) = new CypherParameterValue(CyObject(value.value.map{ case (k, v) => (k, v.value)}))

  import scala.language.implicitConversions

  implicit def convertPrimitive(value: String): CypherPrimitiveParameterValue = apply(CyString(value))
  implicit def convertPrimitive(value: BigDecimal): CypherPrimitiveParameterValue = apply(CyNumber(value))
  implicit def convertPrimitive(value: Long): CypherPrimitiveParameterValue = apply(CyNumber(value))
  implicit def convertPrimitive(value: Int): CypherPrimitiveParameterValue = apply(CyNumber(value))
  implicit def convertPrimitive(value: Short): CypherPrimitiveParameterValue = apply(CyNumber(value.toInt))
  implicit def convertPrimitive(value: Byte): CypherPrimitiveParameterValue = apply(CyNumber(value.toInt))
  implicit def convertPrimitive(value: Double): CypherPrimitiveParameterValue = apply(CyNumber(value))
  implicit def convertPrimitive(value: Float): CypherPrimitiveParameterValue = apply(CyNumber(value.toDouble))
  implicit def convertPrimitive(value: Boolean): CypherPrimitiveParameterValue = apply(CyBoolean(value))

  implicit def convertOption[T](value: Option[T])(implicit conv: T => CypherParameterValue): CypherParameterValue = value match {
    case None => apply(CyNull)
    case Some(v) => conv(v)
  }

  implicit def convertSeq[T](value: Seq[T])(implicit conv: T => CypherPrimitiveParameterValue): CypherPrimitiveArrayParameterValue =
    apply(value.map(v => conv(v).value))

  implicit def convertCyPrimitive(value: CyPrimitive): CypherParameterValue = apply(value)
  implicit def convertCyPrimitiveSeq(value: Seq[CyPrimitive]): CypherParameterValue = apply(value)

  implicit def convertProperties(value: CypherProperties): CypherParameterValue = apply(value)
}

/**
 * Parameter value.
 * @param value underlying cypher value
 */
sealed class CypherParameterValue protected(val value: CyValue) {
  override def hashCode(): Int = value.hashCode()

  override def equals(obj: scala.Any): Boolean = obj match {
    case other: CypherParameterValue => value.equals(other.value)
    case _ => false
  }

  override def toString: String = value.toString
}

/**
 * Primitive parameter value.
 * @param value underlying cypher value
 */
final class CypherPrimitiveParameterValue(override val value: CyPrimitive) extends CypherParameterValue(value)

/**
 * Primitive parameter value.
 * @param array primitive array
 */
final class CypherPrimitiveArrayParameterValue(array: Seq[CyPrimitive]) extends CypherParameterValue(CyArray(array))

/**
 * Describes parameter valid in CypherProperties.
 * Only primitive types and homogeneous arrays.
 *
 * @param value underlying cypher value
 */
final class CypherPropertyParameterValue private(value: CyValue) extends CypherParameterValue(value)

object CypherPropertyParameterValue {
  def apply(other: CypherParameterValue) = new CypherPropertyParameterValue(other.value)

  import scala.language.implicitConversions
  implicit def convertPrimitive[T](value: T)(implicit conv: T => CypherPrimitiveParameterValue) = apply(conv(value))
  implicit def convertPrimitiveSeq[T](value: Seq[T])(implicit conv: Seq[T] => CypherPrimitiveArrayParameterValue) = apply(conv(value))
}

/**
 * Represents a set of Cypher properties.
 */
class CypherProperties(val value: Map[String, CypherPropertyParameterValue] = Map()) {

  override def hashCode(): Int = value.hashCode()

  override def equals(obj: scala.Any): Boolean = obj match {
    case other: CypherProperties => value.equals(other.value)
    case _ => false
  }

  override def toString: String = value.toString()
}

object CypherProperties {
  def apply(pairs: (String, CypherPropertyParameterValue)*) = new CypherProperties(pairs.toMap)
}
