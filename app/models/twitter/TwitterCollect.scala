package models.twitter

import models.{SocialCollect, TemporaryRule}
import providers.{ProviderType, SocialMediaRule}
import providers.twitter.TwitterRule
import twitter.ObservableFile
import org.joda.time.DateTime

import scala.collection.mutable.ListBuffer

/**
 * Implémentation de SocialCollect pour Twitter
 */
class TwitterCollect(
  override val name: String,
  override val directory: String,
  override val accountName: String,
  override val isActive: Boolean = false,
  override val createdAt: DateTime = new DateTime()
) extends SocialCollect {
  
  override def providerType: ProviderType.ProviderType = ProviderType.Twitter
  
  // Variables pour stockage des données
  private val rules = new ListBuffer[TwitterRule]()
  private var temporaryRules = List.empty[TemporaryRule]
  private var active: Boolean = isActive
  
  /**
   * Convertit une règle du modèle général au modèle spécifique à Twitter
   */
  def convertRule(rule: models.Rule): TwitterRule = {
    new TwitterRule(
      Some(rule.id.toString),
      rule.tag,
      rule.content,
      rule.collectName,
      rule.createdAt
    )
  }
  
  /**
   * Initialise les règles de la collecte
   */
  def initRules(newRules: List[SocialMediaRule]): Unit = {
    rules.clear()
    rules ++= newRules.collect { case r: TwitterRule => r }
  }
  
  /**
   * Définit les règles temporaires
   */
  def setTemporaryRules(newRules: List[TemporaryRule]): Unit = {
    temporaryRules = newRules
  }
  
  /**
   * Ajoute une règle à la collecte
   */
  def addRule(rule: TwitterRule): Boolean = {
    rules += rule
    true
  }
  
  /**
   * Supprime des règles de la collecte
   */
  def removeRules(rulesToRemove: List[TwitterRule]): Boolean = {
    val idsToRemove = rulesToRemove.flatMap(_.id).toSet
    rules --= rules.filter(r => r.id.exists(idsToRemove.contains))
    true
  }
  
  /**
   * Renvoie les règles actives
   */
  def activeRules: List[TwitterRule] = rules.filter(_.isActive).toList
  
  override def isActive: Boolean = active
  
  override def setActive(newActive: Boolean): Unit = {
    this.active = newActive
  }
  
  /**
   * Ferme la collecte
   */
  override def close(): Unit = {
    active = false
  }
  
  private var observableFile: ObservableFile = new ObservableFile(None)
  
  override def nonActiveRules: Seq[SocialMediaRule] = {
    rules.filterNot(_.isActive).toList
  }
  
  override def addRules(newRules: Seq[SocialMediaRule]): Boolean = {
    rules ++= newRules.collect { case r: TwitterRule => r }
    true
  }
  
  override def removeTemporaryRulesFromList(temporaryRulesToRemove: Seq[TemporaryRule]): Unit = {
    if (temporaryRules.nonEmpty) {
      val idsToRemove = temporaryRulesToRemove.flatMap(_.id).toSet
      temporaryRules = temporaryRules.filterNot(rule => rule.id.exists(idsToRemove.contains)).toList
    }
  }
  
  /**
   * Méthode spécifique à Twitter pour récupérer l'ObservableFile
   */
  def getObservableFile: ObservableFile = observableFile
  
  /**
   * Méthode spécifique à Twitter pour définir l'ObservableFile
   */
  def setObservableFile(file: ObservableFile): Unit = {
    observableFile = file
  }
  
  override def getTemporaryRules: Option[Seq[TemporaryRule]] = Some(temporaryRules)
}

/**
 * Objet companion pour les formulaires
 */
object TwitterCollectForm {
  /**
   * Forms related
   */
  import play.api.data.Forms._
  import play.api.data._
  
  case class TwitterCollectData(name: String, accountName: String)
  
  val form: Form[TwitterCollectData] = Form(
    mapping(
      "Name" -> nonEmptyText,
      "Account" -> nonEmptyText
    )(TwitterCollectData.apply)(TwitterCollectData.unapply)
  )
} 