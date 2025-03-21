package services

import javax.inject._
import models._
import models.twitter.{TwitterAccount, TwitterAccountForm}
import models.bluesky.{BlueskyAccount, BlueskyAccountForm}
import providers.ProviderType
import models.AccountType
import org.joda.time.DateTime
import java.util.UUID

/**
 * Service pour la gestion des comptes sociaux
 */
@Singleton
class AccountService @Inject()() {
  // En l'absence de base de données, nous utilisons une collection en mémoire
  private var accounts: Map[String, SocialAccount] = Map.empty
  
  /**
   * Récupère tous les comptes sociaux
   */
  def getAllAccounts: Seq[SocialAccount] = {
    accounts.values.toSeq
  }
  
  /**
   * Récupère un compte par son id
   */
  def getAccount(id: String): Option[SocialAccount] = {
    accounts.get(id)
  }
  
  /**
   * Crée un compte Twitter
   */
  def createTwitterAccount(accountForm: TwitterAccountForm): SocialAccount = {
    val account = TwitterAccount(
      name = accountForm.name,
      accountType = AccountType.Developer,
      bearerToken = accountForm.accessToken,
      createdAt = DateTime.now()
    )
    
    val id = UUID.randomUUID().toString
    accounts = accounts + (id -> account)
    account
  }
  
  /**
   * Crée un compte Bluesky
   */
  def createBlueskyAccount(accountForm: BlueskyAccountForm): SocialAccount = {
    val account = BlueskyAccount(
      name = accountForm.name,
      identifier = accountForm.identifier,
      password = accountForm.password,
      createdAt = DateTime.now()
    )
    
    val id = UUID.randomUUID().toString
    accounts = accounts + (id -> account)
    account
  }
  
  /**
   * Met à jour un compte Twitter
   */
  def updateTwitterAccount(id: String, accountForm: TwitterAccountForm): Option[SocialAccount] = {
    getAccount(id).flatMap { existing =>
      if (existing.providerType == ProviderType.Twitter) {
        val updated = TwitterAccount(
          name = accountForm.name,
          accountType = AccountType.Developer,
          bearerToken = accountForm.accessToken,
          createdAt = existing.createdAt
        )
        
        accounts = accounts + (id -> updated)
        Some(updated)
      } else {
        None
      }
    }
  }
  
  /**
   * Met à jour un compte Bluesky
   */
  def updateBlueskyAccount(id: String, accountForm: BlueskyAccountForm): Option[SocialAccount] = {
    getAccount(id).flatMap { existing =>
      if (existing.providerType == ProviderType.Bluesky) {
        val updated = BlueskyAccount(
          name = accountForm.name,
          identifier = accountForm.identifier,
          password = accountForm.password,
          createdAt = existing.createdAt
        )
        
        accounts = accounts + (id -> updated)
        Some(updated)
      } else {
        None
      }
    }
  }
  
  /**
   * Supprime un compte
   */
  def deleteAccount(id: String): Boolean = {
    if (accounts.contains(id)) {
      accounts = accounts - id
      true
    } else {
      false
    }
  }
} 