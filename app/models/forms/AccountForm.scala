package models.forms

import play.api.data.Forms._
import play.api.data._
import providers.ProviderType

object AccountForm extends BaseForm[AccountForm.AccountData] {
  case class AccountData(
    name: String,
    providerType: ProviderType,
    bearerToken: String,
    identifier: String,
    password: String
  )
  
  override val form: Form[AccountData] = Form(
    mapping(
      "Name" -> nonEmptyText,
      "Provider Type" -> Forms.of(enumFormatter(ProviderType)),
      "Bearer Token" -> nonEmptyText,
      "Identifier" -> nonEmptyText,
      "Password" -> nonEmptyText
    )(AccountData.apply)(AccountData.unapply)
  )
} 