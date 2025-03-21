package models.bluesky

import models.SocialAccount
import org.joda.time.DateTime

/**
 * Formulaire pour la création et mise à jour d'un compte Bluesky
 */
case class BlueskyAccountForm(
  name: String,
  identifier: String,
  password: String
)

object BlueskyAccountForm {
  def apply(name: String, identifier: String, password: String): BlueskyAccountForm = {
    new BlueskyAccountForm(name, identifier, password)
  }
  
  def unapply(form: BlueskyAccountForm): Option[(String, String, String)] = {
    Some((form.name, form.identifier, form.password))
  }
  
  def fromAccount(account: SocialAccount): BlueskyAccountForm = {
    val blueskyAccount = account.asInstanceOf[BlueskyAccount]
    BlueskyAccountForm(
      blueskyAccount.name,
      blueskyAccount.identifier,
      blueskyAccount.password
    )
  }
} 