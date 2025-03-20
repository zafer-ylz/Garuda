package providers.bluesky

import providers.{ProviderType, SocialMediaProvider}
import models.{SocialAccount, SocialCollect}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Left, Right}

class BlueskyProvider extends SocialMediaProvider {
  override def providerType: ProviderType = ProviderType.Bluesky
  
  override def createConnection(account: SocialAccount): BlueskyConnection = {
    new BlueskyConnection(account)
  }
  
  override def validateCredentials(account: SocialAccount): Future[Either[String, Unit]] = {
    val connection = createConnection(account)
    connection.validateCredentials()
  }
  
  override def getMaxRuleLength(account: SocialAccount): Int = {
    // Bluesky n'a pas de limite de longueur pour les règles
    Int.MaxValue
  }
  
  override def getMaxRulesPerAccount(account: SocialAccount): Int = {
    // Bluesky n'a pas de limite pour le nombre de règles
    Int.MaxValue
  }
} 