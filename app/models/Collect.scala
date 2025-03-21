package models

import providers.{ProviderType, SocialMediaRule}
import providers.twitter.TwitterRule
import org.joda.time.DateTime
import models.twitter.TwitterCollect

import scala.collection.mutable

trait RuleContainer {
	def activeRules: List[Rule]
	def nonActiveRules: List[Rule] 
	def rules: Option[List[Rule]]
	def temporaryRules: Option[List[TemporaryRule]]
}

class Collect(val name: String, val directory: String, val accountName: String, val providerType: ProviderType, val isActive: Boolean = false, val createdAt: DateTime = new DateTime()) extends RuleContainer {
	private var _rules: Option[List[Rule]] = None
	private var _temporaryRules: Option[List[TemporaryRule]] = None
	
	// Méthodes d'adaptation entre Rule et SocialMediaRule
	private def adaptRule(rule: Rule): SocialMediaRule = {
		// Utilisation directe de la DateTime de Joda
		providerType match {
			case ProviderType.Twitter => 
				new TwitterRule(Option(rule.id.toString), rule.tag, rule.content, rule.collectName, rule.createdAt)
			case _ => 
				throw new UnsupportedOperationException(s"Provider ${providerType} not supported")
		}
	}
	
	private def adaptRules(rules: List[Rule]): List[SocialMediaRule] = {
		rules.map(adaptRule)
	}
	
	// Getters pour les propriétés
	def rules: Option[List[Rule]] = _rules
	def temporaryRules: Option[List[TemporaryRule]] = _temporaryRules
	
	// Méthodes pour les règles  
	def initRules(adaptedRules: List[Rule]): Unit = {
		adaptToSocialCollect match {
			case twitterCollect: TwitterCollect =>
				val convertedRules = adaptedRules.map(twitterCollect.convertRule)
				twitterCollect.initRules(convertedRules)
			case _ =>
				throw new UnsupportedOperationException(s"Provider type $providerType is not supported")
		}
	}
	
	// Surcharge de la méthode initRules pour prendre en compte les règles et les règles temporaires
	def initRules(rules: List[Rule], temporaryRules: List[TemporaryRule]): Unit = {
		_rules = Some(rules)
		_temporaryRules = Some(temporaryRules)
		
		// Si des règles sont présentes, initialiser aussi dans l'adaptateur social
		if (rules.nonEmpty) {
			initRules(rules)
		}
	}
	
	def activeRules: List[Rule] = {
		_rules.getOrElse(List.empty).filter(_.isActive)
	}
	
	def nonActiveRules: List[Rule] = {
		_rules.getOrElse(List.empty).filterNot(_.isActive)
	}
	
	def addRule(rule: Rule): Boolean = {
		adaptToSocialCollect match {
			case twitterCollect: TwitterCollect =>
				val convertedRule = twitterCollect.convertRule(rule)
				twitterCollect.addRule(convertedRule)
			case _ =>
				throw new UnsupportedOperationException(s"Provider type $providerType is not supported")
		}
	}
	
	def removeRules(rules: List[Rule]): Boolean = {
		adaptToSocialCollect match {
			case twitterCollect: TwitterCollect =>
				val convertedRules = rules.map(twitterCollect.convertRule)
				twitterCollect.removeRules(convertedRules)
			case _ =>
				throw new UnsupportedOperationException(s"Provider type $providerType is not supported")
		}
	}
	
	def removeTemporaryRules(rules: List[TemporaryRule]): Unit = {
		_temporaryRules = Some(_temporaryRules.getOrElse(List.empty).filterNot(r => rules.exists(_.id == r.id)))
	}
	
	def setTemporaryRules(rules: List[TemporaryRule]): Unit = {
		_temporaryRules = Some(rules)
	}
	
	/**
	 * Pour la compatibilité avec l'ancien code
	 */
	def adaptToSocialCollect: SocialCollect = {
		providerType match {
			case ProviderType.Twitter => 
				val twitterCollect = new TwitterCollect(name, directory, accountName)
				// Si nous avons des règles, on les adapte
				if (_rules.isDefined) {
					val adaptedRules = adaptRules(_rules.get)
					twitterCollect.initRules(adaptedRules)
				}
				// Si nous avons des règles temporaires, on les adapte
				if (_temporaryRules.isDefined) {
					twitterCollect.setTemporaryRules(_temporaryRules.get)
				}
				twitterCollect.setActive(isActive)
				twitterCollect
			case _ => 
				throw new UnsupportedOperationException(s"Provider ${providerType} not supported")
		}
	}
	
	/**
	 * Fonction copy pour créer une nouvelle instance avec des paramètres modifiés
	 */
	def copy(newName: String = this.name, 
			 newDirectory: String = this.directory, 
			 newAccountName: String = this.accountName, 
			 newProviderType: ProviderType = this.providerType,
			 newIsActive: Boolean = this.isActive,
			 newCreatedAt: DateTime = this.createdAt): Collect = {
		new Collect(newName, newDirectory, newAccountName, newProviderType, newIsActive, newCreatedAt)
	}
	
	/**
	 * Propriété pour accéder à accountName comme account pour compatibilité
	 */
	def account: String = accountName
}

/**
 * Objet companion pour créer des instances de Collect
 */
object Collect {
	/**
	 * Méthode apply pour créer une instance de Collect
	 */
	def apply(name: String, directory: String, account: String, createdAt: DateTime): Collect = {
		// Par défaut, on suppose que c'est Twitter puisque c'était le premier provider supporté
		new Collect(name, directory, account, ProviderType.Twitter, false, createdAt)
	}
	
	/**
	 * Méthode unapply pour l'extraction de pattern
	 */
	def unapply(collect: Collect): Option[(String, String, String, DateTime)] = {
		Some((collect.name, collect.directory, collect.accountName, collect.createdAt))
	}
}

// Cette classe représente une règle pour les collectes
case class Rule(
	id: Long,
	tag: String,
	content: String,
	collectName: String,
	createdAt: DateTime = new DateTime(),
	private var _isActive: Boolean = false
) {
	/**
	 * Définit si la règle est active
	 */
	def setActive(active: Boolean): Unit = {
		_isActive = active
	}
	
	/**
	 * Indique si la règle est active
	 */
	def isActive: Boolean = _isActive
	
	/**
	 * Accesseur pour la compatibilité avec collectName/collect
	 */
	def collect: String = collectName
	
	/**
	 * Propriété pour l'accès via la nouvelle architecture
	 */
	def active: Boolean = _isActive
}

/**
 * Objet companion pour la classe Rule
 */
object Rule {
	/**
	 * Méthode factory pour créer des instances de Rule
	 */
	def apply(id: Long, tag: String, content: String, collectName: String,
		createdAt: DateTime = new DateTime(), isActive: Boolean = false): Rule = {
		val rule = new Rule(id, tag, content, collectName, createdAt, isActive)
		if (isActive) rule.setActive(true)
		rule
	}
	
	/**
	 * Méthode tupled pour Slick
	 */
	def tupled: ((Long, String, String, String, DateTime, Boolean)) => Rule = {
		case (id, tag, content, collectName, createdAt, isActive) =>
			new Rule(id, tag, content, collectName, createdAt, isActive)
	}
	
	/**
	 * Méthode unapply pour Slick
	 */
	def unapply(rule: Rule): Option[(Long, String, String, String, DateTime, Boolean)] = {
		Some((rule.id, rule.tag, rule.content, rule.collectName, rule.createdAt, rule.isActive))
	}
}

object CollectForm {
	/**
	 * Forms related
	 */
	import play.api.data.Forms._
	import play.api.data._
	
	case class CollectData(name: String, account: String)
	
	val form: Form[CollectData] = Form(
		mapping(
			"Name" -> nonEmptyText,
			"Account" -> nonEmptyText
		)(CollectData.apply)(CollectData.unapply)
	)
}
