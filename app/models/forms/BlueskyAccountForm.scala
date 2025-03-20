package models.forms

import play.api.data.Forms._
import play.api.data._
import models.bluesky.BlueskyAccount
import org.joda.time.DateTime

object BlueskyAccountForm extends BaseForm[BlueskyAccount] {
  override val form: Form[BlueskyAccount] = Form(
    mapping(
      "Name" -> nonEmptyText,
      "Identifier" -> nonEmptyText,
      "Password" -> nonEmptyText
    )(BlueskyAccount.apply(_, _, _, DateTime.now()))(account => Some((account.name, account.identifier, account.password)))
  )
} 