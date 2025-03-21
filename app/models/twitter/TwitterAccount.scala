package models.twitter

import models.{BaseAccount, Collect, SocialCollect, Rule, TemporaryRule, AccountType}
import providers.{ProviderType, SocialMediaRule, StreamingConnection}
import providers.twitter.{TwitterProvider, TwitterRule}
import twitter.{TwitterConnection, TweetStreamListener}
import org.joda.time.DateTime
import scala.collection.JavaConverters._

/**
 * Implémentation d'un compte Twitter.
 */
case class TwitterAccount(
  override val name: String,
  accountType: AccountType,
  bearerToken: String,
  override val createdAt: DateTime = DateTime.now()
) extends BaseAccount {
  private var rules: Option[Seq[SocialMediaRule]] = None
  private val twitterConnection = new TwitterConnection(this)
  private var currentActiveCollect: Option[TwitterStreamConnection] = None
  
  override def providerType: ProviderType = ProviderType.Twitter
  
  var rateFilteredStreamConnecting: Int = accountType.rateFilteredStreamConnecting
  var rateFilteredStreamAddingOrDeletingFilters: Int = accountType.rateFilteredStreamAddingOrDeletingFilters
  var rateFilteredStreamListingFilters: Int = accountType.rateFilteredStreamListingFilters
  
  def resetRates(): Unit = {
    rateFilteredStreamConnecting = accountType.rateFilteredStreamConnecting
    rateFilteredStreamAddingOrDeletingFilters = accountType.rateFilteredStreamAddingOrDeletingFilters
    rateFilteredStreamListingFilters = accountType.rateFilteredStreamListingFilters
  }
  
  override def hasActiveCollect: Boolean = currentActiveCollect.isDefined
  
  override def getActiveCollect: Option[SocialCollect] = {
    currentActiveCollect.map(_.collect)
  }
  
  override def isCurrentActiveCollect(collect: SocialCollect): Boolean = {
    currentActiveCollect.isDefined && currentActiveCollect.get.collect.name == collect.name
  }
  
  override def startCollect(collect: SocialCollect): Either[String, StreamingConnection] = {
    if (currentActiveCollect.isEmpty) {
      val result = twitterConnection.startCollect(collect.asInstanceOf[TwitterCollect])
      if (result.isRight) {
        val connection = new TwitterStreamConnection(result.toOption.get, collect)
        currentActiveCollect = Some(connection)
        Right(connection)
      } else {
        result.asInstanceOf[Either[String, StreamingConnection]]
      }
    } else {
      Left(s"The collect ${currentActiveCollect.get.collect.name} is already active.")
    }
  }
  
  override def stopCollect(collect: SocialCollect): Boolean = {
    if (currentActiveCollect.isDefined && currentActiveCollect.get.collect.name == collect.name) {
      currentActiveCollect.get.shutdown()
      currentActiveCollect = None
      true
    } else {
      false
    }
  }
  
  override def initRules(collectName: String): Either[String, Seq[SocialMediaRule]] = {
    if (rules.isEmpty) {
      retrieveActiveRules(collectName)
    } else {
      Right(rules.get)
    }
  }
  
  override def retrieveActiveRules(collectName: String): Either[String, Seq[SocialMediaRule]] = {
    val activeRules = twitterConnection.getAllRules(collectName)
    if (activeRules.isRight) {
      val rulesConverted = activeRules.right.get.map(rule => 
        new TwitterRule(
          Option(rule.id.toString),
          rule.tag,
          rule.content,
          rule.collect,
          rule.createdAt
        )
      )
      rules = Some(rulesConverted)
      Right(rules.get)
    } else {
      Left(activeRules.left.getOrElse("Problem with Twitter API"))
    }
  }
  
  override def addRules(collectName: String, rules: Seq[SocialMediaRule]): Either[String, Seq[SocialMediaRule]] = {
    // Adaptation pour l'ancienne API
    val temporaryRulesList = Seq.empty[models.TemporaryRule]
    val rulesList = rules.map(rule => 
      models.Rule(
        rule.id.map(_.toLong).getOrElse(-1L),
        rule.tag,
        rule.content,
        rule.collectName
      )
    )
    
    val addedRules = twitterConnection.addRules(collectName, temporaryRulesList, rulesList.toList)
    
    if (addedRules.isRight) {
      val convertedRules = addedRules.right.get.map(rule => 
        new TwitterRule(
          Option(rule.id.toString),
          rule.tag,
          rule.content,
          rule.collectName,
          rule.createdAt
        )
      )
      this.rules = Some(convertedRules)
      Right(convertedRules)
    } else {
      Left(addedRules.left.getOrElse("Problem adding rules"))
    }
  }
  
  override def removeRules(collectName: String, rules: Seq[SocialMediaRule]): Either[String, Seq[SocialMediaRule]] = {
    // Conversion des règles pour l'ancienne API
    val rulesList = rules.map(rule => 
      models.Rule(
        rule.id.map(_.toLong).getOrElse(-1L),
        rule.tag,
        rule.content,
        rule.collectName
      )
    )
    
    val removedRules = twitterConnection.removeRules(rulesList.toList)
    val refreshedRules = retrieveActiveRules(collectName)
    
    refreshedRules
  }
  
  override def getActiveRules: Seq[SocialMediaRule] = {
    rules.getOrElse(Seq.empty)
  }
  
  override def getMaxRuleLength: Int = accountType.sizeOfRule
  
  override def cancel(): Boolean = {
    if (currentActiveCollect.isDefined) {
      currentActiveCollect.get.shutdown()
      currentActiveCollect = None
      true
    } else {
      false
    }
  }
}

/**
 * Classe qui adapte un TweetStreamListener à l'interface StreamingConnection
 */
class TwitterStreamConnection(
  val tweetStreamListener: TweetStreamListener, 
  val collect: SocialCollect
) extends StreamingConnection {
  override def stream(): StreamingConnection = {
    tweetStreamListener.stream()
    this
  }
  
  override def executeListeners(): Unit = {
    tweetStreamListener.executeListeners()
  }
  
  override def shutdown(): Unit = {
    tweetStreamListener.shutdown()
  }
  
  def isActive: Boolean = tweetStreamListener.isActive
} 