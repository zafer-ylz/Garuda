package models.twitter

import models.SocialAccount
import org.joda.time.DateTime

/**
 * Formulaire pour la création et mise à jour d'un compte Twitter
 */
case class TwitterAccountForm(
  name: String,
  identifier: String,
  password: String,
  apiKey: String,
  apiSecret: String,
  accessToken: String,
  accessTokenSecret: String
)

object TwitterAccountForm {
  def apply(name: String, identifier: String, password: String, apiKey: String, apiSecret: String, accessToken: String, accessTokenSecret: String): TwitterAccountForm = {
    new TwitterAccountForm(name, identifier, password, apiKey, apiSecret, accessToken, accessTokenSecret)
  }
  
  def unapply(form: TwitterAccountForm): Option[(String, String, String, String, String, String, String)] = {
    Some((form.name, form.identifier, form.password, form.apiKey, form.apiSecret, form.accessToken, form.accessTokenSecret))
  }
  
  def fromAccount(account: SocialAccount): TwitterAccountForm = {
    val twitterAccount = account.asInstanceOf[TwitterAccount]
    TwitterAccountForm(
      twitterAccount.name,
      twitterAccount.identifier,
      twitterAccount.password,
      twitterAccount.apiKey,
      twitterAccount.apiSecret,
      twitterAccount.accessToken,
      twitterAccount.accessTokenSecret
    )
  }
} 