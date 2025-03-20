package models.twitter

import models.{SocialAccount, SocialCollect, AccountType}
import providers.{ProviderType, SocialMediaRule, StreamingConnection}
import providers.twitter.{TwitterProvider, TwitterRule}
import twitter.TwitterConnection
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
) extends SocialAccount {
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
  
  def isCurrentActiveCollect(collect: SocialCollect): Boolean = {
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
        Left(result.left.getOrElse("Unknown error starting collect"))
      }
    } else {
      Left(s"The collect ${currentActiveCollect.get.collect.name} is already active.")
    }
  }
  
  override def stopCollect(collect: SocialCollect): Boolean = {
    if (currentActiveCollect.isDefined && currentActiveCollect.get.collect.name == collect.name) {
      currentActiveCollect.get.shutdown()
      currentActiveCollect = None
      collect.close()
      true
    } else {
      false
    }
  }
  
  override def initRules(collectName: String): Either[String, Seq[SocialMediaRule]] = {
    if (rules.isEmpty) {
      retrieveActiveRules(collectName)
    } else {
      // Les règles ont déjà été récupérées, ne pas contacter l'API Twitter
      Right(rules.get)
    }
  }
  
  override def retrieveActiveRules(collectName: String): Either[String, Seq[SocialMediaRule]] = {
    // Les règles n'ont pas encore été récupérées
    val activeRules = twitterConnection.getAllRules(collectName)
    if (activeRules.isRight) {
      // Les règles ont été correctement récupérées
      val convertedRules = activeRules.getOrElse(Seq.empty).map(rule => 
        TwitterRule(Some(rule.id), rule.tag, rule.content, collectName, rule.createdAt)
      )
      rules = Some(convertedRules)
      Right(rules.get)
    } else {
      // Il y a eu un problème avec l'API Twitter
      Left(activeRules.left.getOrElse("Problem with Twitter API"))
    }
  }
  
  override def addRules(collectName: String, rules: Seq[SocialMediaRule]): Either[String, Seq[SocialMediaRule]] = {
    // Convertir en règles Twitter si nécessaire
    val twitterRules = rules.collect {
      case rule: TwitterRule => rule
      case rule => TwitterRule(rule.id, rule.tag, rule.content, rule.collectName, rule.createdAt)
    }
    
    val addedRules = twitterConnection.addRules(collectName, Seq.empty, twitterRules.collect { 
      case r: models.Rule => r 
    })
    
    if (addedRules.isRight) {
      // Si les règles ont été correctement mises à jour, mettre à jour les règles du compte
      this.rules = Some(addedRules.getOrElse(Seq.empty).map(rule => 
        TwitterRule(Some(rule.id), rule.tag, rule.content, rule.collect, rule.createdAt)
      ))
    }
    
    addedRules.map(_.map(rule => 
      TwitterRule(Some(rule.id), rule.tag, rule.content, rule.collect, rule.createdAt)
    ))
  }
  
  override def removeRules(collectName: String, rules: Seq[SocialMediaRule]): Either[String, Seq[SocialMediaRule]] = {
    // Convertir en règles Twitter si nécessaire
    val twitterRules = rules.collect {
      case rule: TwitterRule => rule
      case rule => TwitterRule(rule.id, rule.tag, rule.content, rule.collectName, rule.createdAt)
    }
    
    twitterConnection.removeRules(twitterRules.collect { case r: models.Rule => r })
    retrieveActiveRules(collectName)
  }
  
  /**
   * Méthode pour obtenir le token bearer (spécifique à Twitter)
   */
  def getBearerToken: String = bearerToken
}

/**
 * Classe qui adapte un TweetStreamListener à l'interface StreamingConnection
 */
class TwitterStreamConnection(
  val tweetStreamListener: twitter.TweetStreamListener, 
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
  
  override def isActive: Boolean = {
    tweetStreamListener.isActive
  }
}

/**
 * Objet companion pour les formulaires
 */
object TwitterAccountForm {
  /**
   * Forms related
   */
  import play.api.data.Forms._
  import play.api.data._
  
  val form: Form[TwitterAccount] = Form(
    mapping(
      "Name" -> nonEmptyText,
      "Account type" -> Forms.of[AccountType],
      "Bearer Token" -> nonEmptyText
    )(TwitterAccount.apply(_, _, _, DateTime.now()))(account => Some((account.name, account.accountType, account.bearerToken)))
  )
} 