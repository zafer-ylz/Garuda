package models

import providers.{ProviderType, SocialMediaRule}
import org.joda.time.DateTime

/**
 * Interface abstraite pour les collectes de messages sur les réseaux sociaux.
 */
trait SocialCollect {
  /**
   * Nom de la collecte
   */
  def name: String
  
  /**
   * Répertoire où stocker les données collectées
   */
  def directory: String
  
  /**
   * Nom du compte associé à cette collecte
   */
  def accountName: String
  
  /**
   * Type de provider utilisé pour cette collecte
   */
  def providerType: ProviderType.ProviderType
  
  /**
   * Obtient le type de provider (pour rétrocompatibilité)
   */
  def getProviderType: ProviderType.ProviderType = providerType
  
  /**
   * Date de création de la collecte
   */
  def createdAt: DateTime
  
  /**
   * Règles actives pour cette collecte
   */
  def activeRules: Seq[SocialMediaRule]
  
  /**
   * Règles inactives pour cette collecte
   */
  def nonActiveRules: Seq[SocialMediaRule]
  
  /**
   * Vérifie si la collecte est active
   */
  def isActive: Boolean
  
  /**
   * Active ou désactive la collecte
   */
  def setActive(active: Boolean): Unit
  
  /**
   * Initialise les règles pour cette collecte
   */
  def initRules(rules: Seq[SocialMediaRule]): Unit
  
  /**
   * Ajoute une règle à cette collecte
   */
  def addRule(rule: SocialMediaRule): Boolean
  
  /**
   * Ajoute plusieurs règles à cette collecte
   */
  def addRules(rules: Seq[SocialMediaRule]): Boolean
  
  /**
   * Supprime des règles de cette collecte
   */
  def removeRules(rules: Seq[SocialMediaRule]): Boolean
  
  /**
   * Ferme la collecte (arrête la collecte de données)
   */
  def close(): Unit = { setActive(false) }
  
  /**
   * Obtient les règles temporaires
   */
  def getTemporaryRules: Option[Seq[models.TemporaryRule]] = None
  
  /**
   * Définit les règles temporaires
   */
  def setTemporaryRules(temporaryRules: Seq[models.TemporaryRule]): Unit = {}
  
  /**
   * Supprime des règles temporaires
   */
  def removeTemporaryRulesFromList(temporaryRulesToRemove: Seq[models.TemporaryRule]): Unit = {}
} 