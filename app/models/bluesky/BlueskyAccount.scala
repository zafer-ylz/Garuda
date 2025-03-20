package models.bluesky

import models.{BaseAccount, SocialCollect}
import providers.{ProviderType, SocialMediaRule, StreamingConnection}
import providers.bluesky.{BlueskyProvider, BlueskyRule}
import org.joda.time.DateTime
import providers.ProviderRegistry

/**
 * Implémentation d'un compte Bluesky.
 */
case class BlueskyAccount(
  override val name: String,
  identifier: String,
  password: String,
  override val createdAt: DateTime = DateTime.now()
) extends BaseAccount {
  private var rules: Option[Seq[SocialMediaRule]] = None
  private var currentActiveCollect: Option[BlueskyStreamConnection] = None
  
  override def providerType: ProviderType = ProviderType.Bluesky
  
  override def hasActiveCollect: Boolean = currentActiveCollect.isDefined
  
  override def getActiveCollect: Option[SocialCollect] = {
    currentActiveCollect.map(_.collect)
  }
  
  override def isCurrentActiveCollect(collect: SocialCollect): Boolean = {
    currentActiveCollect.isDefined && currentActiveCollect.get.collect.name == collect.name
  }
  
  override def startCollect(collect: SocialCollect): Either[String, StreamingConnection] = {
    if (currentActiveCollect.isEmpty) {
      ProviderRegistry.getProvider(ProviderType.Bluesky) match {
        case Some(provider) =>
          val blueskyProvider = provider.asInstanceOf[BlueskyProvider]
          val credentials = Map("identifier" -> identifier, "password" -> password)
          val account = models.SocialMediaAccount(name, providerType, credentials)
          
          blueskyProvider.startCollect(account, collect) match {
            case Right(connection) =>
              val adaptedConnection = new BlueskyStreamConnection(connection, collect)
              currentActiveCollect = Some(adaptedConnection)
              Right(adaptedConnection)
            case Left(error) => 
              Left(error)
          }
        case None =>
          Left("Bluesky provider not found")
      }
    } else {
      Left(s"The collect ${currentActiveCollect.get.collect.name} is already active.")
    }
  }
  
  override def stopCollect(collect: SocialCollect): Boolean = {
    if (currentActiveCollect.isDefined && currentActiveCollect.get.collect.name == collect.name) {
      currentActiveCollect.get.shutdown()
      currentActiveCollect = None
      collect.close()
      true
    } else {
      false
    }
  }
  
  override def initRules(collectName: String): Either[String, Seq[SocialMediaRule]] = {
    if (rules.isEmpty) {
      retrieveActiveRules(collectName)
    } else {
      Right(rules.get)
    }
  }
  
  override def retrieveActiveRules(collectName: String): Either[String, Seq[SocialMediaRule]] = {
    ProviderRegistry.getProvider(ProviderType.Bluesky) match {
      case Some(provider) =>
        val blueskyProvider = provider.asInstanceOf[BlueskyProvider]
        val credentials = Map("identifier" -> identifier, "password" -> password)
        val account = models.SocialMediaAccount(name, providerType, credentials)
        
        val activeRules = blueskyProvider.getActiveRules(account, collectName)
        if (activeRules.isRight) {
          rules = Some(activeRules.getOrElse(Seq.empty))
          Right(rules.get)
        } else {
          Left(activeRules.left.getOrElse("Problem with Bluesky API"))
        }
      case None =>
        Left("Bluesky provider not found")
    }
  }
  
  override def addRules(collectName: String, rules: Seq[SocialMediaRule]): Either[String, Seq[SocialMediaRule]] = {
    ProviderRegistry.getProvider(ProviderType.Bluesky) match {
      case Some(provider) =>
        val blueskyProvider = provider.asInstanceOf[BlueskyProvider]
        val credentials = Map("identifier" -> identifier, "password" -> password)
        val account = models.SocialMediaAccount(name, providerType, credentials)
        
        val addedRules = blueskyProvider.addRules(account, collectName, rules)
        if (addedRules.isRight) {
          this.rules = Some(addedRules.getOrElse(Seq.empty))
        }
        addedRules
      case None =>
        Left("Bluesky provider not found")
    }
  }
  
  override def removeRules(collectName: String, rules: Seq[SocialMediaRule]): Either[String, Seq[SocialMediaRule]] = {
    ProviderRegistry.getProvider(ProviderType.Bluesky) match {
      case Some(provider) =>
        val blueskyProvider = provider.asInstanceOf[BlueskyProvider]
        val credentials = Map("identifier" -> identifier, "password" -> password)
        val account = models.SocialMediaAccount(name, providerType, credentials)
        
        blueskyProvider.removeRules(account, rules)
        retrieveActiveRules(collectName)
      case None =>
        Left("Bluesky provider not found")
    }
  }
  
  /**
   * Méthode spécifique à Bluesky pour obtenir l'identifiant
   */
  def getIdentifier: String = identifier
  
  /**
   * Méthode spécifique à Bluesky pour obtenir le mot de passe
   */
  def getPassword: String = password
}

/**
 * Classe qui adapte un StreamingConnection générique à celui de Bluesky
 */
class BlueskyStreamConnection(
  val connection: StreamingConnection, 
  val collect: SocialCollect
) extends StreamingConnection {
  override def stream(): StreamingConnection = {
    connection.stream()
    this
  }
  
  override def executeListeners(): Unit = {
    connection.executeListeners()
  }
  
  override def shutdown(): Unit = {
    connection.shutdown()
  }
  
  override def isActive: Boolean = {
    connection.isActive
  }
} 