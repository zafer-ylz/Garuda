package models

import org.joda.time.DateTime
import providers.{ProviderType, StreamingConnection}

/**
 * Classe de base abstraite pour tous les comptes sociaux
 */
abstract class BaseAccount {
  /**
   * Nom du compte
   */
  val name: String
  
  /**
   * Date de création du compte
   */
  val createdAt: DateTime
  
  /**
   * Type de provider associé à ce compte
   */
  def providerType: ProviderType
  
  /**
   * Vérifie si le compte a une collecte active
   */
  def hasActiveCollect: Boolean
  
  /**
   * Obtient la collecte active si elle existe
   */
  def getActiveCollect: Option[SocialCollect]
  
  /**
   * Vérifie si la collecte fournie est la collecte active
   */
  def isCurrentActiveCollect(collect: SocialCollect): Boolean
  
  /**
   * Démarre une collecte
   */
  def startCollect(collect: SocialCollect): Either[String, StreamingConnection]
  
  /**
   * Arrête une collecte
   */
  def stopCollect(collect: SocialCollect): Boolean
  
  /**
   * Initialise les règles pour une collecte
   */
  def initRules(collectName: String): Either[String, Seq[SocialMediaRule]]
  
  /**
   * Récupère les règles actives pour une collecte
   */
  def retrieveActiveRules(collectName: String): Either[String, Seq[SocialMediaRule]]
  
  /**
   * Ajoute des règles à une collecte
   */
  def addRules(collectName: String, rules: Seq[SocialMediaRule]): Either[String, Seq[SocialMediaRule]]
  
  /**
   * Supprime des règles d'une collecte
   */
  def removeRules(collectName: String, rules: Seq[SocialMediaRule]): Either[String, Seq[SocialMediaRule]]
  
  /**
   * Annule les opérations en cours
   */
  def cancel(): Boolean
  
  /**
   * Obtient les règles actives
   */
  def getActiveRules: Seq[SocialMediaRule] = Seq.empty
  
  /**
   * Obtient la longueur maximale de règle autorisée pour ce compte
   */
  def getMaxRuleLength: Int = 1024
} 