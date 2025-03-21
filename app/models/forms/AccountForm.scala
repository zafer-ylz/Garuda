package models.forms

import play.api.data.Forms._
import play.api.data._
import providers.ProviderType

case class AccountData(
  name: String,
  providerType: ProviderType.Value,
  bearerToken: String,
  identifier: String,
  password: String
)

object AccountForm extends BaseForm[AccountData] {
  
  // Formatter personnalisé pour ProviderType
  private val providerTypeFormatter = new Formatter[ProviderType.Value] {
    def bind(key: String, data: Map[String, String]): Either[Seq[FormError], ProviderType.Value] = {
      data.get(key).map { value =>
        try {
          Right(ProviderType.withName(value))
        } catch {
          case _: NoSuchElementException => 
            Left(Seq(FormError(key, "error.invalidProviderType", Nil)))
        }
      }.getOrElse(Left(Seq(FormError(key, "error.required", Nil))))
    }

    def unbind(key: String, value: ProviderType.Value): Map[String, String] = 
      Map(key -> value.toString)
  }
  
  private val providerTypeMapping = Forms.of[ProviderType.Value](providerTypeFormatter)
  
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