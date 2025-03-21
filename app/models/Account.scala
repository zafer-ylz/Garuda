package models

import providers.{ProviderType, SocialMediaRule, StreamingConnection}
import org.joda.time.DateTime
import models.twitter.{TwitterAccount, TwitterCollect}
import java.time.LocalDateTime

/**
 * Cette classe est maintenue pour des raisons de compatibilité avec l'ancien code.
 * Utiliser SocialAccount à la place pour les nouveaux développements.
 */
case class Account(name: String, accountType: AccountType, bearerToken: String) {
	private var rules: Option[Seq[Rule]] = None
	
	private var twitterAccountAdapter: Option[TwitterAccount] = None
	
	private def getTwitterAccount(): TwitterAccount = {
		if (twitterAccountAdapter.isEmpty) {
			twitterAccountAdapter = Some(TwitterAccount(name, accountType, bearerToken))
		}
		twitterAccountAdapter.get
	}
	
	// Méthode pour adapter SocialMediaRule à Rule
	private def adaptRule(rule: SocialMediaRule): Rule = {
		val id = rule.id.map(_.toLong).getOrElse(-1L)
		val newRule = new Rule(id, rule.tag, rule.content, rule.collectName)
		newRule.setActive(rule.isActive)
		newRule
	}
	
	// Méthode pour adapter Rule à SocialMediaRule
	private def adaptToSocialMediaRule(rule: Rule): SocialMediaRule = {
		new providers.twitter.TwitterRule(
			Option(rule.id.toString), 
			rule.tag, 
			rule.content, 
			rule.collectName, 
			convertToJodaDateTime(rule.createdAt)
		)
	}
	
	// Conversion de LocalDateTime à DateTime pour la compatibilité
	private def convertToJodaDateTime(localDateTime: LocalDateTime): DateTime = {
		new DateTime(
			localDateTime.getYear,
			localDateTime.getMonthValue,
			localDateTime.getDayOfMonth,
			localDateTime.getHour,
			localDateTime.getMinute,
			localDateTime.getSecond
		)
	}
	
	// Méthodes pour maintenir la compatibilité avec l'ancien code
	def isCurrentActiveCollect(collect: Collect): Boolean = {
		getTwitterAccount().isCurrentActiveCollect(collect.adaptToSocialCollect)
	}
	
	/**
	 * Récupère la collecte active pour la compatibilité avec l'ancien code
	 */
	def currentActiveCollect: Option[SocialCollect] = {
		getTwitterAccount().getActiveCollect
	}
	
	def startCollect(collect: Collect): Either[String, StreamingConnection] = {
		getTwitterAccount().startCollect(collect.adaptToSocialCollect)
	}
	
	def stopCollect(collect: Collect): Boolean = {
		getTwitterAccount().stopCollect(collect.adaptToSocialCollect)
	}
	
	def initRules(collectName: String): Either[String, Seq[Rule]] = {
		getTwitterAccount().initRules(collectName).map(rules => rules.map(adaptRule))
	}
	
	def retrieveActiveRules(collectName: String): Either[String, Seq[Rule]] = {
		getTwitterAccount().retrieveActiveRules(collectName).map(rules => rules.map(adaptRule))
	}
	
	def addRules(collectName: String, temporaryRules: Seq[TemporaryRule], rules: Seq[Rule]): Either[String, Seq[Rule]] = {
		val socialMediaRules = rules.map(adaptToSocialMediaRule)
		getTwitterAccount().addRules(collectName, socialMediaRules).map(rules => rules.map(adaptRule))
	}
	
	def removeRules(collectName: String, rules: Seq[Rule]): Either[String, Seq[Rule]] = {
		val socialMediaRules = rules.map(adaptToSocialMediaRule)
		getTwitterAccount().removeRules(collectName, socialMediaRules).map(rules => rules.map(adaptRule))
	}
	
	// Méthodes d'accès pour la compatibilité
	def activeRules: Seq[Rule] = {
		getTwitterAccount().getActiveRules.map(adaptRule)
	}
	
	def maxRuleLength: Int = {
		getTwitterAccount().getMaxRuleLength
	}
	
	def getProviderType: ProviderType = {
		ProviderType.Twitter
	}
	
	def getMaxRuleLength: Int = {
		maxRuleLength
	}
}

/**
 * Extension de la classe Rule pour ajouter la méthode setActive
 */
class RuleWithActive(id: Long, tag: String, content: String, collect: String, createdAt: DateTime = DateTime.now()) 
	extends Rule(id, tag, content, collect, createdAt) {
	
	private var _isActive: Boolean = false
	
	def setActive(active: Boolean): Unit = {
		_isActive = active
	}
	
	def isActive: Boolean = _isActive
}

object AccountForm {
	
	/**
	 * Forms related
	 */
	import play.api.data.Forms._
	import play.api.data._
	import play.api.data.format.Formatter
	
	implicit def matchFilterFormat: Formatter[AccountType] = new Formatter[AccountType] {
		override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], AccountType] = {
			data.get(key)
				.map(AccountType.withName)
				.toRight(Seq(FormError(key, "error.required", Nil)))
		}
		
		override def unbind(key: String, value: AccountType): Map[String, String] = {
			Map(key -> value.name)
		}
	}
	
	val form: Form[Account] = Form(
		mapping(
			"Name" -> nonEmptyText,
			"Account type" -> Forms.of[AccountType],
			"Bearer Token" -> nonEmptyText
		)(Account.apply)(Account.unapply)
	)
}
