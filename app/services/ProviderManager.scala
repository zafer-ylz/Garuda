package services

import javax.inject.{Inject, Singleton}
import models.{SocialAccount, SocialCollect, Collect}
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
   * Crée une collecte pour le provider spécifié.
   * 
   * @param name Nom de la collecte
   * @param directory Répertoire de stockage des données
   * @param accountName Nom du compte associé
   * @param providerType Type de provider
   * @return Une instance de Collect configurée pour le provider spécifié
   */
  def createCollect(name: String, directory: String, accountName: String, providerType: ProviderType): Collect = {
    // Crée une instance de la nouvelle classe Collect avec le provider approprié
    new Collect(name, directory, accountName, providerType)
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
  
  /**
   * Vérifie si un provider est supporté
   * 
   * @param provider Type de provider à vérifier
   * @return true si le provider est supporté, false sinon
   */
  def isProviderSupported(provider: ProviderType): Boolean = {
    provider match {
      case ProviderType.Twitter => true
      case ProviderType.Bluesky => true
      case _ => false
    }
  }
} 