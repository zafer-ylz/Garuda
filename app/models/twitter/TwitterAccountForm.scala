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
    // On récupère les identifiants à partir des credentials dans le bearerToken
    // Logique fictive, dans une vraie application ces valeurs seraient stockées dans TwitterAccount
    val credentialsMap = Map(
      "identifier" -> "twitter_user",
      "password" -> "********",
      "apiKey" -> "api_key_value",
      "apiSecret" -> "api_secret_value",
      "accessToken" -> "access_token_value",
      "accessTokenSecret" -> "access_token_secret_value"
    )
    
    TwitterAccountForm(
      twitterAccount.name,
      credentialsMap("identifier"),
      credentialsMap("password"),
      credentialsMap("apiKey"),
      credentialsMap("apiSecret"),
      credentialsMap("accessToken"),
      credentialsMap("accessTokenSecret")
    )
  }
} 