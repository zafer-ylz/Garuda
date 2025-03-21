package models.twitter

import models.{SocialCollect, TemporaryRule}
import providers.{ProviderType, SocialMediaRule}
import providers.twitter.TwitterRule
import twitter.ObservableFile
import org.joda.time.DateTime
import java.time.LocalDateTime

/**
 * Implémentation d'une collecte Twitter.
 */
case class TwitterCollect(
  override val name: String,
  override val directory: String,
  override val accountName: String,
  override val createdAt: DateTime = DateTime.now()
) extends SocialCollect {
  private var rules: Seq[SocialMediaRule] = Seq.empty
  private var _isActive: Boolean = false
  private var observableFile: ObservableFile = new ObservableFile(None)
  private var _temporaryRules: Option[Seq[TemporaryRule]] = None
  
  override def providerType: ProviderType = ProviderType.Twitter
  
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
    observableFile.setNone()
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
  
  override def getTemporaryRules: Option[Seq[TemporaryRule]] = _temporaryRules
  
  override def setTemporaryRules(temporaryRules: Seq[TemporaryRule]): Unit = {
    _temporaryRules = Some(temporaryRules)
  }
  
  override def removeTemporaryRulesFromList(temporaryRulesToRemove: Seq[TemporaryRule]): Unit = {
    if (_temporaryRules.isDefined) {
      val idsToRemove = temporaryRulesToRemove.flatMap(_.id).toSet
      _temporaryRules = Some(_temporaryRules.get.filterNot(rule => rule.id.exists(idsToRemove.contains)))
    }
  }
  
  /**
   * Convertit un Rule en TwitterRule
   */
  def convertRule(rule: models.Rule): TwitterRule = {
    TwitterRule(
      Option(rule.id.toString),
      rule.tag,
      rule.content,
      rule.collectName,
      new DateTime(
        rule.createdAt.getYear,
        rule.createdAt.getMonthValue,
        rule.createdAt.getDayOfMonth,
        rule.createdAt.getHour,
        rule.createdAt.getMinute
      )
    )
  }
  
  /**
   * Convertit un TwitterRule en Rule
   */
  def convertToRule(rule: TwitterRule): models.Rule = {
    val ruleId = rule.id.map(_.toLong).getOrElse(-1L)
    val newRule = models.Rule(
      ruleId,
      rule.tag, 
      rule.content, 
      rule.collectName,
      LocalDateTime.of(
        rule.createdAt.getYear,
        rule.createdAt.getMonthOfYear,
        rule.createdAt.getDayOfMonth,
        rule.createdAt.getHourOfDay,
        rule.createdAt.getMinuteOfHour,
        rule.createdAt.getSecondOfMinute
      )
    )
    newRule.setActive(rule.isActive)
    newRule
  }
  
  /**
   * Méthode spécifique à Twitter pour activer la collecte
   */
  def setActive(active: Boolean): Unit = {
    _isActive = active
  }
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