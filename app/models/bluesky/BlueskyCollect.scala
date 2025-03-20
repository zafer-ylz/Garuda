package models.bluesky

import models.SocialCollect
import providers.{ProviderType, SocialMediaRule}
import org.joda.time.DateTime
import java.io.File

/**
 * Implémentation d'une collecte Bluesky.
 */
case class BlueskyCollect(
  override val name: String,
  override val directory: String,
  override val accountName: String,
  override val createdAt: DateTime = DateTime.now()
) extends SocialCollect {
  private var rules: Seq[SocialMediaRule] = Seq.empty
  private var _isActive: Boolean = false
  
  override def providerType: ProviderType = ProviderType.Bluesky
  
  override def isActive: Boolean = _isActive
  
  override def activeRules: Seq[SocialMediaRule] = {
    rules.filter(_.isActive)
  }
  
  override def nonActiveRules: Seq[SocialMediaRule] = {
    rules.filterNot(_.isActive)
  }
  
  override def initRules(newRules: Seq[SocialMediaRule]): Unit = {
    this.rules = newRules
  }
  
  override def addRule(rule: SocialMediaRule): Boolean = {
    rules = rules :+ rule
    true
  }
  
  override def addRules(newRules: Seq[SocialMediaRule]): Boolean = {
    rules = rules ++ newRules
    true
  }
  
  override def removeRules(rulesToRemove: Seq[SocialMediaRule]): Boolean = {
    val rulesToRemoveIds = rulesToRemove.flatMap(_.id).toSet
    rules = rules.filterNot(rule => rule.id.exists(rulesToRemoveIds.contains))
    true
  }
  
  override def close(): Unit = {
    _isActive = false
  }
  
  /**
   * Méthode spécifique à Bluesky pour activer la collecte
   */
  def setActive(active: Boolean): Unit = {
    _isActive = active
  }
  
  /**
   * Méthode spécifique à Bluesky pour obtenir le fichier de sortie
   */
  def getOutputFile: File = {
    val dir = new File(directory)
    if (!dir.exists()) {
      dir.mkdirs()
    }
    new File(dir, s"$name-${createdAt.toString("yyyyMMdd-HHmmss")}.json")
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