package ru.dgolubets.neo4s.internal.json

import play.api.libs.json._
import ru.dgolubets.neo4s.model._

/**
 * Play json formats for CyValue classes.
 */
private[neo4s] trait CyFormats {

  implicit lazy val cyFormat: Format[CyValue] = new Format[CyValue] {

    override def writes(cypher: CyValue): JsValue = cypher match {
      case CyNull => JsNull
      case CyNumber(value) => JsNumber(value)
      case CyString(value) => JsString(value)
      case CyBoolean(value) => JsBoolean(value)
      case CyArray(value) => JsArray(value.map(writes))
      case CyObject(value) => JsObject(value.map { case (k,v) => (k, writes(v))})
    }


    override def reads(json: JsValue): JsResult[CyValue] = json match {
      case JsNull => JsSuccess(CyNull)
      case JsNumber(value) => JsSuccess(CyNumber(value))
      case JsString(value) => JsSuccess(CyString(value))
      case JsBoolean(value) => JsSuccess(CyBoolean(value))
      case JsArray(value) => JsSuccess(CyArray(value.map(v => reads(v).get)))
      case JsObject(value) => JsSuccess(CyObject(value.map {case (k,v) => (k, reads(v).get)}.toMap))
      case _ => JsError()
    }
  }

  implicit lazy val cyObjectReads: Reads[CyObject] = new Reads[CyObject] {
    override def reads(json: JsValue): JsResult[CyObject] = JsSuccess(json.as[CyValue].asInstanceOf[CyObject])
  }

  implicit lazy val cyArrayReads: Reads[CyArray] = new Reads[CyArray] {
    override def reads(json: JsValue): JsResult[CyArray] = JsSuccess(json.as[CyValue].asInstanceOf[CyArray])
  }
}
