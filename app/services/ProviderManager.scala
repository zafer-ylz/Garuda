package services

import javax.inject.{Inject, Singleton}
import models.{SocialAccount, SocialCollect}
import providers.{ProviderType, SocialMediaRule}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ProviderManager @Inject()(implicit executionContext: ExecutionContext) {
  
  /**
   * Crée une instance de SocialAccount en fonction du type de provider
   */
  def createAccount(name: String, providerType: ProviderType, credentials: Map[String, String]): SocialAccount = {
    providerType match {
      case ProviderType.Twitter => 
        new TwitterAccount(name, credentials("bearerToken"))
      case ProviderType.Bluesky =>
        new BlueskyAccount(name, credentials("identifier"), credentials("password"))
      case _ =>
        throw new IllegalArgumentException(s"Provider type $providerType not supported")
    }
  }
  
  /**
   * Crée une instance de SocialCollect en fonction du type de provider
   */
  def createCollect(name: String, directory: String, accountName: String, providerType: ProviderType): SocialCollect = {
    providerType match {
      case ProviderType.Twitter =>
        new TwitterCollect(name, directory, accountName)
      case ProviderType.Bluesky =>
        new BlueskyCollect(name, directory, accountName)
      case _ =>
        throw new IllegalArgumentException(s"Provider type $providerType not supported")
    }
  }
  
  /**
   * Convertit une règle générique en règle spécifique au provider
   */
  def convertRule(rule: SocialMediaRule, providerType: ProviderType): SocialMediaRule = {
    (rule.providerType, providerType) match {
      case (ProviderType.Twitter, ProviderType.Twitter) => rule
      case (ProviderType.Bluesky, ProviderType.Bluesky) => rule
      case (_, ProviderType.Twitter) =>
        TwitterRule(
          id = rule.id,
          tag = rule.tag,
          content = rule.content,
          collectName = rule.collectName,
          createdAt = rule.createdAt
        )
      case (_, ProviderType.Bluesky) =>
        BlueskyRule(
          id = rule.id,
          tag = rule.tag,
          content = rule.content,
          collectName = rule.collectName,
          createdAt = rule.createdAt
        )
      case _ =>
        throw new IllegalArgumentException(s"Cannot convert rule from ${rule.providerType} to $providerType")
    }
  }
  
  /**
   * Vérifie si une règle est valide pour un provider donné
   */
  def validateRule(rule: SocialMediaRule, providerType: ProviderType): Boolean = {
    rule.providerType == providerType
  }
} 