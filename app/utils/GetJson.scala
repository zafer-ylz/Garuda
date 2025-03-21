package utils

import play.api.libs.json.{JsArray, JsBoolean, JsNull, JsNumber, JsObject, JsString, JsValue}

/**
 * Utilitaire pour extraire des données à partir d'objets JSON
 */
object GetJson {
  /**
   * Extrait une chaîne optionnelle à partir d'un JsValue
   * 
   * @param jsValue Le JsValue à partir duquel extraire la chaîne
   * @return Option[String] contenant la valeur si elle existe et est une chaîne
   */
  def optionString(jsValue: JsValue): Option[String] = jsValue match {
    case JsString(value) => Some(value)
    case JsNumber(value) => Some(value.toString)
    case JsNull => None
    case _ => None
  }
  
  /**
   * Extrait un entier optionnel à partir d'un JsValue
   * 
   * @param jsValue Le JsValue à partir duquel extraire l'entier
   * @return Option[Int] contenant la valeur si elle existe et est un entier
   */
  def optionInt(jsValue: JsValue): Option[Int] = jsValue match {
    case JsNumber(value) => Some(value.toInt)
    case JsString(value) => try { Some(value.toInt) } catch { case _: NumberFormatException => None }
    case JsNull => None
    case _ => None
  }
  
  /**
   * Extrait un long optionnel à partir d'un JsValue
   * 
   * @param jsValue Le JsValue à partir duquel extraire le long
   * @return Option[Long] contenant la valeur si elle existe et est un long
   */
  def optionLong(jsValue: JsValue): Option[Long] = jsValue match {
    case JsNumber(value) => Some(value.toLong)
    case JsString(value) => try { Some(value.toLong) } catch { case _: NumberFormatException => None }
    case JsNull => None
    case _ => None
  }
  
  /**
   * Extrait un booléen optionnel à partir d'un JsValue
   * 
   * @param jsValue Le JsValue à partir duquel extraire le booléen
   * @return Option[Boolean] contenant la valeur si elle existe et est un booléen
   */
  def optionBoolean(jsValue: JsValue): Option[Boolean] = jsValue match {
    case JsBoolean(value) => Some(value)
    case JsString(value) => Some(value.toLowerCase == "true")
    case JsNumber(value) => Some(value != 0)
    case JsNull => None
    case _ => None
  }
  
  /**
   * Extrait un array optionnel à partir d'un JsValue
   * 
   * @param jsValue Le JsValue à partir duquel extraire l'array
   * @return Option[JsArray] contenant la valeur si elle existe et est un array
   */
  def optionArray(jsValue: JsValue): Option[JsArray] = jsValue match {
    case array: JsArray => Some(array)
    case JsNull => None
    case _ => None
  }
  
  /**
   * Extrait un objet optionnel à partir d'un JsValue
   * 
   * @param jsValue Le JsValue à partir duquel extraire l'objet
   * @return Option[JsObject] contenant la valeur si elle existe et est un objet
   */
  def optionObject(jsValue: JsValue): Option[JsObject] = jsValue match {
    case obj: JsObject => Some(obj)
    case JsNull => None
    case _ => None
  }
  
  /**
   * Extrait un double optionnel à partir d'un JsValue
   * 
   * @param jsValue Le JsValue à partir duquel extraire le double
   * @return Option[Double] contenant la valeur si elle existe et est un double
   */
  def optionDouble(jsValue: JsValue): Option[Double] = jsValue match {
    case JsNumber(value) => Some(value.toDouble)
    case JsString(value) => try { Some(value.toDouble) } catch { case _: NumberFormatException => None }
    case JsNull => None
    case _ => None
  }
} 