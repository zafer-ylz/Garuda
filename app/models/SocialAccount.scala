package models

import providers.{ProviderType, SocialMediaRule, StreamingConnection}
import org.joda.time.DateTime

/**
 * Interface abstraite pour les comptes de réseaux sociaux.
 */
trait SocialAccount {
  /**
   * Nom du compte
   */
  def name: String
  
  /**
   * Type du provider (Twitter, Bluesky, etc.)
   */
  def providerType: ProviderType
  
  /**
   * Date de création du compte
   */
  def createdAt: DateTime
  
  /**
   * Vérifie si ce compte a une collecte active
   */
  def hasActiveCollect: Boolean
  
  /**
   * Récupère la collecte active si elle existe
   */
  def getActiveCollect: Option[SocialCollect]
  
  /**
   * Démarre une collecte sur ce compte
   */
  def startCollect(collect: SocialCollect): Either[String, StreamingConnection]
  
  /**
   * Arrête une collecte sur ce compte
   */
  def stopCollect(collect: SocialCollect): Boolean
  
  /**
   * Initialise les règles pour ce compte
   */
  def initRules(collectName: String): Either[String, Seq[SocialMediaRule]]
  
  /**
   * Récupère les règles actives depuis l'API
   */
  def retrieveActiveRules(collectName: String): Either[String, Seq[SocialMediaRule]]
  
  /**
   * Ajoute des règles pour ce compte
   */
  def addRules(collectName: String, rules: Seq[SocialMediaRule]): Either[String, Seq[SocialMediaRule]]
  
  /**
   * Supprime des règles pour ce compte
   */
  def removeRules(collectName: String, rules: Seq[SocialMediaRule]): Either[String, Seq[SocialMediaRule]]
} 