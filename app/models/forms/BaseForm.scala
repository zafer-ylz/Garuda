package models.forms

import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formatter

/**
 * Classe de base abstraite pour les formulaires
 */
trait BaseForm[T] {
  /**
   * Le formulaire associé à cette classe
   */
  val form: Form[T]
  
  /**
   * Méthode utilitaire pour créer un formatter pour un type énuméré
   */
  protected def enumFormatter[E <: Enumeration](`enum`: E): Formatter[E#Value] = new Formatter[E#Value] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], E#Value] = {
      data.get(key)
        .map(s => try {
          Right(`enum`.withName(s))
        } catch {
          case _: NoSuchElementException => Left(Seq(FormError(key, "error.enum.invalid", Nil)))
        })
        .getOrElse(Left(Seq(FormError(key, "error.required", Nil))))
    }
    
    override def unbind(key: String, value: E#Value): Map[String, String] = {
      Map(key -> value.toString)
    }
  }
} 