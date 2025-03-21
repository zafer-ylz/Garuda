package models.bluesky

import models.{SocialCollect, TemporaryRule}
import org.joda.time.DateTime
import providers.{ProviderType, SocialMediaRule}
import providers.bluesky.BlueskyRule

import scala.collection.mutable.ListBuffer

/**
 * Implémentation de SocialCollect pour Bluesky
 */
class BlueskyCollect(
  override val name: String,
  override val directory: String,
  override val accountName: String,
  override val isActive: Boolean = false,
  override val createdAt: DateTime = new DateTime()
) extends SocialCollect {
  
  override def providerType: ProviderType.ProviderType = ProviderType.Bluesky
  
  // Variables pour stockage des données
  private val rules = new ListBuffer[BlueskyRule]()
  private var temporaryRules = List.empty[TemporaryRule]
  private var active: Boolean = isActive
  
  /**
   * Convertit une règle du modèle général au modèle spécifique à Bluesky
   */
  def convertRule(rule: models.Rule): BlueskyRule = {
    new BlueskyRule(
      Some(rule.id),
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
    rules ++= newRules.collect { case r: BlueskyRule => r }
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
  def addRule(rule: BlueskyRule): Boolean = {
    rules += rule
    true
  }
  
  /**
   * Supprime des règles de la collecte
   */
  def removeRules(rulesToRemove: List[BlueskyRule]): Boolean = {
    val idsToRemove = rulesToRemove.flatMap(_.id).toSet
    rules --= rules.filter(r => r.id.exists(idsToRemove.contains))
    true
  }
  
  /**
   * Renvoie les règles actives
   */
  def activeRules: List[BlueskyRule] = rules.filter(_.isActive).toList
  
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
  
  /**
   * Méthode spécifique à Bluesky pour obtenir le fichier de sortie
   */
  def getOutputFile: java.io.File = {
    val dir = new java.io.File(directory)
    if (!dir.exists()) {
      dir.mkdirs()
    }
    new java.io.File(dir, s"$name-${createdAt.toString("yyyyMMdd-HHmmss")}.json")
  }
}

/**
 * Objet companion pour les formulaires
 */
object BlueskyCollectForm {
  /**
   * Forms related
   */
  import play.api.data.Forms._
  import play.api.data._
  
  case class BlueskyCollectData(name: String, accountName: String)
  
  val form: Form[BlueskyCollectData] = Form(
    mapping(
      "Name" -> nonEmptyText,
      "Account" -> nonEmptyText
    )(BlueskyCollectData.apply)(BlueskyCollectData.unapply)
  )
} 