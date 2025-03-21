package models.tweet

import net.liftweb.json._

/**
 * Classe utilitaire pour extraire des valeurs depuis du JSON
 */
object GetJson {
  implicit val formats: DefaultFormats.type = DefaultFormats

  def optionString(json: JValue): Option[String] = {
    json match {
      case JString(s) => Some(s)
      case JNothing | JNull => None
      case x => Some(x.values.toString)
    }
  }

  def optionInt(json: JValue): Option[Int] = {
    json match {
      case JInt(i) => Some(i.toInt)
      case JDouble(d) => Some(d.toInt)
      case JString(s) => try { Some(s.toInt) } catch { case _: Throwable => None }
      case JNothing | JNull => None
      case _ => None
    }
  }

  def optionDouble(json: JValue): Option[Double] = {
    json match {
      case JDouble(d) => Some(d)
      case JInt(i) => Some(i.toDouble)
      case JString(s) => try { Some(s.toDouble) } catch { case _: Throwable => None }
      case JNothing | JNull => None
      case _ => None
    }
  }

  def optionBoolean(json: JValue): Option[Boolean] = {
    json match {
      case JBool(b) => Some(b)
      case JString(s) => try { Some(s.toBoolean) } catch { case _: Throwable => None }
      case JNothing | JNull => None
      case _ => None
    }
  }
} 