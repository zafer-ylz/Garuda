package utils

import play.api.libs.json._

/**
 * Utilitaire moderne pour extraire des valeurs de JSON
 * Remplace l'ancien GetJson basé sur liftweb
 */
object JsonExtractor {
  /**
   * Extrait une chaîne de caractères d'un JsValue
   */
  def string(json: JsValue, path: JsPath = JsPath): Option[String] = {
    (json \ path.toJsonString).asOpt[String]
  }
  
  /**
   * Extrait un booléen d'un JsValue
   */
  def boolean(json: JsValue, path: JsPath = JsPath): Option[Boolean] = {
    (json \ path.toJsonString).asOpt[Boolean]
  }
  
  /**
   * Vérifie si un champ existe dans un JsValue
   */
  def exists(json: JsValue, path: JsPath = JsPath): Boolean = {
    (json \ path.toJsonString) != JsUndefined
  }
  
  /**
   * Extrait un entier d'un JsValue
   */
  def integer(json: JsValue, path: JsPath = JsPath): Option[Int] = {
    (json \ path.toJsonString).asOpt[Int]
  }
  
  /**
   * Extrait un long d'un JsValue
   */
  def long(json: JsValue, path: JsPath = JsPath): Option[Long] = {
    (json \ path.toJsonString).asOpt[Long]
  }
  
  /**
   * Extrait un tableau d'un JsValue
   */
  def array[T](json: JsValue, path: JsPath = JsPath)(implicit reads: Reads[T]): Option[Seq[T]] = {
    (json \ path.toJsonString).asOpt[Seq[T]]
  }
  
  /**
   * Extrait un objet d'un JsValue
   */
  def obj[T](json: JsValue, path: JsPath = JsPath)(implicit reads: Reads[T]): Option[T] = {
    (json \ path.toJsonString).asOpt[T]
  }
} 