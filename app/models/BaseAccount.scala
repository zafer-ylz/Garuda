package models

import org.joda.time.DateTime
import providers.{ProviderType, SocialMediaRule, StreamingConnection}

/**
 * Classe de base abstraite pour tous les comptes sociaux
 */
abstract class BaseAccount extends SocialAccount {
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
   * Implémentation par défaut pour la compatibilité avec la nouvelle structure
   */
  def startCollect(collect: Collect): Either[String, StreamingConnection] = {
    startCollect(collect.adaptToSocialCollect)
  }
  
  /**
   * Arrête une collecte
   */
  def stopCollect(collect: SocialCollect): Boolean
  
  /**
   * Implémentation par défaut pour la compatibilité avec la nouvelle structure
   */
  def stopCollect(collect: Collect): Boolean = {
    stopCollect(collect.adaptToSocialCollect)
  }
  
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
   * Implémentation par défaut pour la compatibilité avec la nouvelle structure
   */
  def addRules(collectName: String, temporaryRules: List[TemporaryRule], rules: List[Rule]): Either[String, Seq[Rule]] = {
    // Conversion des règles standard en SocialMediaRule
    val socialMediaRules = rules.map(r => 
      new providers.twitter.TwitterRule(
        Option(r.id.toString), 
        r.tag, 
        r.content, 
        r.collectName, 
        convertToJodaDateTime(r.createdAt)
      )
    )
    
    // Ajout des règles
    addRules(collectName, socialMediaRules).map(result => 
      result.map(r => {
        val id = r.id.map(_.toLong).getOrElse(-1L)
        val rule = new Rule(id, r.tag, r.content, r.collectName)
        rule.setActive(r.isActive)
        rule
      })
    )
  }
  
  /**
   * Supprime des règles d'une collecte
   */
  def removeRules(collectName: String, rules: Seq[SocialMediaRule]): Either[String, Seq[SocialMediaRule]]
  
  /**
   * Implémentation par défaut pour la compatibilité avec la nouvelle structure
   */
  def removeRules(collectName: String, rules: List[Rule]): Either[String, Seq[Rule]] = {
    // Conversion des règles standard en SocialMediaRule
    val socialMediaRules = rules.map(r => 
      new providers.twitter.TwitterRule(
        Option(r.id.toString), 
        r.tag, 
        r.content, 
        r.collectName, 
        convertToJodaDateTime(r.createdAt)
      )
    )
    
    // Suppression des règles
    removeRules(collectName, socialMediaRules).map(result => 
      result.map(r => {
        val id = r.id.map(_.toLong).getOrElse(-1L)
        val rule = new Rule(id, r.tag, r.content, r.collectName)
        rule.setActive(r.isActive)
        rule
      })
    )
  }
  
  /**
   * Annule les opérations en cours
   */
  def cancel(): Boolean
  
  /**
   * Obtient les règles actives
   */
  def getActiveRules: Seq[SocialMediaRule] = Seq.empty
  
  /**
   * Implémentation de activeRules pour la compatibilité
   */
  def activeRules: Seq[SocialMediaRule] = getActiveRules
  
  /**
   * Implémentation de activeRulesList pour la compatibilité avec l'ancien code
   */
  def activeRulesList: List[Rule] = {
    getActiveRules.map(r => {
      val id = r.id.map(_.toLong).getOrElse(-1L)
      val rule = new Rule(id, r.tag, r.content, r.collectName)
      rule.setActive(r.isActive)
      rule
    }).toList
  }
  
  /**
   * Obtient la longueur maximale de règle autorisée pour ce compte
   */
  def getMaxRuleLength: Int = 1024
  
  /**
   * Conversion de LocalDateTime à DateTime pour la compatibilité
   */
  protected def convertToJodaDateTime(localDateTime: java.time.LocalDateTime): DateTime = {
    new DateTime(
      localDateTime.getYear,
      localDateTime.getMonthValue,
      localDateTime.getDayOfMonth,
      localDateTime.getHour,
      localDateTime.getMinute,
      localDateTime.getSecond
    )
  }
} 