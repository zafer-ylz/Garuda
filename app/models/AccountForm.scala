package models

import play.api.data.Forms._
import play.api.data._
import providers.ProviderType

object AccountForm {
  case class AccountData(
    name: String,
    providerType: ProviderType,
    bearerToken: String,
    identifier: String,
    password: String
  )
  
  val form: Form[AccountData] = Form(
    mapping(
      "Name" -> nonEmptyText,
      "Provider Type" -> Forms.of[ProviderType],
      "Bearer Token" -> nonEmptyText,
      "Identifier" -> nonEmptyText,
      "Password" -> nonEmptyText
    )(AccountData.apply)(AccountData.unapply)
  )
  
  implicit def providerTypeFormat: Formatter[ProviderType] = new Formatter[ProviderType] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], ProviderType] = {
      data.get(key)
        .map(ProviderType.withName)
        .toRight(Seq(FormError(key, "error.required", Nil)))
    }
    
    override def unbind(key: String, value: ProviderType): Map[String, String] = {
      Map(key -> value.name)
    }
  }
} 