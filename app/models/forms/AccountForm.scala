package models.forms

import play.api.data.Forms._
import play.api.data._
import play.api.data.format.Formatter
import providers.ProviderType

case class AccountData(
  name: String,
  providerType: ProviderType.ProviderType,
  bearerToken: String,
  identifier: String,
  password: String
)

object AccountForm extends BaseForm[AccountData] {
  
  // Formatter personnalisé pour ProviderType
  private val providerTypeFormatter = new Formatter[ProviderType.ProviderType] {
    def bind(key: String, data: Map[String, String]): Either[Seq[FormError], ProviderType.ProviderType] = {
      data.get(key).map { value =>
        ProviderType.fromId(value) match {
          case Some(providerType) => Right(providerType)
          case None => Left(Seq(FormError(key, "error.invalidProviderType", Nil)))
        }
      }.getOrElse(Left(Seq(FormError(key, "error.required", Nil))))
    }

    def unbind(key: String, value: ProviderType.ProviderType): Map[String, String] = 
      Map(key -> value.toString)
  }
  
  private val providerTypeMapping = Forms.of[ProviderType.ProviderType](providerTypeFormatter)
  
  override val form: Form[AccountData] = Form(
    mapping(
      "Name" -> nonEmptyText,
      "Provider Type" -> providerTypeMapping,
      "Bearer Token" -> nonEmptyText,
      "Identifier" -> nonEmptyText,
      "Password" -> nonEmptyText
    )(AccountData.apply)(AccountData.unapply _)
  )
} 