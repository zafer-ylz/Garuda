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
  def providerType: ProviderType.ProviderType
  
  /**
   * Date de création du compte
   */
  def createdAt: DateTime
  
  /**
   * Identifiant du compte
   */
  def id: Option[String] = None
  
  /**
   * Identifiant utilisateur
   */
  def identifier: String = ""
  
  /**
   * Vérifie si ce compte a une collecte active
   */
  def hasActiveCollect: Boolean
  
  /**
   * Récupère la collecte active si elle existe
   */
  def getActiveCollect: Option[SocialCollect]
  
  /**
   * Vérifie si la collecte spécifiée est la collecte active
   */
  def isCurrentActiveCollect(collect: SocialCollect): Boolean
  
  /**
   * Démarre une collecte sur ce compte
   */
  def startCollect(collect: Collect): Either[String, StreamingConnection]
  
  /**
   * Démarre une collecte sur ce compte (pour la nouvelle API)
   */
  def startCollect(collect: SocialCollect): Either[String, StreamingConnection] = 
    throw new UnsupportedOperationException("Méthode non implémentée")
  
  /**
   * Arrête une collecte sur ce compte
   */
  def stopCollect(collect: Collect): Boolean
  
  /**
   * Arrête une collecte sur ce compte (pour la nouvelle API)
   */
  def stopCollect(collect: SocialCollect): Boolean =
    throw new UnsupportedOperationException("Méthode non implémentée")
  
  /**
   * Initialise les règles pour ce compte. Contacte l'API uniquement si les règles n'ont pas déjà été récupérées.
   */
  def initRules(collectName: String): Either[String, Seq[SocialMediaRule]]
  
  /**
   * Récupère les règles actives depuis l'API
   */
  def retrieveActiveRules(collectName: String): Either[String, Seq[SocialMediaRule]]
  
  /**
   * Ajoute des règles pour ce compte
   */
  def addRules(collectName: String, temporaryRules: List[TemporaryRule], rules: List[Rule]): Either[String, Seq[Rule]]
  
  /**
   * Supprime des règles pour ce compte
   */
  def removeRules(collectName: String, rules: List[Rule]): Either[String, Seq[Rule]]
  
  /**
   * Annule les opérations en cours
   */
  def cancel(): Boolean = false
  
  /**
   * Obtient les règles actives
   */
  def activeRules: Seq[SocialMediaRule] = Seq.empty
  
  /**
   * Obtient les règles actives sous forme de List[Rule] pour la compatibilité
   */
  def activeRulesList: List[Rule] = List.empty
  
  /**
   * Obtient le type de provider
   */
  def getProviderType: ProviderType.ProviderType = providerType
  
  /**
   * Obtient la longueur maximale de règle autorisée pour ce compte
   */
  def getMaxRuleLength: Int = 1024
} 