package controllers

import dao.{AccountDao, CollectDao, RuleDao, TemporaryRuleDao}
import javax.inject.{Inject, Singleton}
import models.SocialCollect
import providers.SocialMediaRule
import models.CollectForm.{CollectData, form => collectForm}
import models.TemporaryRuleForm.{TemporaryRuleData, form => ruleForm}
import models.{Rule, TemporaryRule}
import play.api.Configuration
import play.api.data._
import play.api.mvc._
import play.filters.csrf._
import services.ProviderManager

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

@Singleton
class CollectController @Inject()(
	accountDao: AccountDao,
	collectDao: CollectDao,
	ruleDao: RuleDao,
	temporaryRulesDao: TemporaryRuleDao,
	providerManager: ProviderManager,
	cc: MessagesControllerComponents,
	conf: Configuration
)(implicit executionContext: ExecutionContext) extends MessagesAbstractController(cc) {
	
	private val postUrlCreateCollect = routes.CollectController.createCollect
	private def postUrlCreateRule(collectName: String) = routes.CollectController.createRule(collectName)
	private def postUrlAffectRules(collectName: String) = routes.CollectController.affectRules(collectName)
	private def postRemoveAccountRulesUrl(collectName: String) = routes.CollectController.removeAccountRules(collectName)
	
	def listCollects: Action[AnyContent] = Action.async { implicit request: MessagesRequest[AnyContent] =>
		collectDao.all().map { collects =>
			accountDao.all().map { accounts =>
				Ok(views.html.listCollects(collects, accounts, collectForm, postUrlCreateCollect))
			}
		}.flatten
	}
	
	def seeCollect(collectName: String): Action[AnyContent] = Action.async { implicit request: MessagesRequest[AnyContent] =>
		val collect = updateRulesOfCollect(collectName)
		displayCollect(collect)
	}
	
	def startCollect(collectName: String): Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
		val collect = updateRulesOfCollect(collectName)
		val account = Await.result(accountDao.findByName(collect.accountName), Duration.Inf).get
		
		val collectStarted = account.startCollect(collect.asInstanceOf[models.Collect])
		
		val flash = {
			if (collectStarted.isRight) {
				new Flash(Map("success" -> "Collect started!"))
			} else {
				new Flash(Map("error" -> s"Impossible to start the collect: \n${collectStarted.left}"))
			}
		}
		
		Redirect(routes.CollectController.seeCollect(collect.name)).flashing(flash)
	}
	
	def stopCollect(collectName: String): Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
		val collect = updateRulesOfCollect(collectName)
		val account = Await.result(accountDao.findByName(collect.accountName), Duration.Inf).get
		
		val collectStopped = account.stopCollect(collect.asInstanceOf[models.Collect])
		
		val flash = {
			if (collectStopped) {
				new Flash(Map("success" -> "Collect stopped!"))
			} else {
				new Flash(Map("error" -> s"Impossible to stop the collect"))
			}
		}
		
		Redirect(routes.CollectController.seeCollect(collect.name)).flashing(flash)
	}
	
	def removeAccountRules(collectName: String): Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
		val nbOfRulesToRemove = request.body.asFormUrlEncoded.get("number_of_rules_to_remove").head.toInt
		if (nbOfRulesToRemove > 0) {
			val rulesIds = request.body.asFormUrlEncoded.get("account_rules_ids").flatMap(_.split(",")).map(_.toLong)
			
			val collect = updateRulesOfCollect(collectName)
			val account = Await.result(accountDao.findByName(collect.accountName), Duration.Inf).get
			
			val rulesToRemove = account.activeRules.filter(rule => rulesIds.contains(rule.id.getOrElse(-1L)))
				.map(r => new Rule(r.id.map(_.toLong), r.tag, r.content, r.collectName, r.createdAt))
			
			account.removeRules(collectName, rulesToRemove)
			
			Redirect(routes.CollectController.seeCollect(collect.name)).flashing("info" -> "Account rules removed.")
		} else {
			Redirect(routes.CollectController.seeCollect(collectName)).flashing("error" -> "No rule selected.")
		}
	}
	
	def affectRules(collectName: String): Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
		val activeIdRules = request.body.asFormUrlEncoded.get("active_ids").flatMap(_.split(","))
		val nonActiveIdRules = request.body.asFormUrlEncoded.get("non_active_ids").flatMap(_.split(","))
		
		val newActiveIdRules = activeIdRules.filter(_.startsWith("n")).map(_.substring(2).toLong)
		val newActiveIdTemporaryRules = activeIdRules.filter(_.startsWith("t")).map(_.substring(2).toLong)
		val newNonActiveIdRules = nonActiveIdRules.filter(_.startsWith("a")).map(_.substring(2).toLong)
		
		val collect = updateRulesOfCollect(collectName)
		val account = Await.result(accountDao.findByName(collect.accountName), Duration.Inf).get
		
		val newActiveRules = collect.nonActiveRules.filter(rule => newActiveIdRules.contains(rule.id.getOrElse(-1L)))
		val newActiveTemporaryRules = collect.getTemporaryRules.getOrElse(List.empty[TemporaryRule]).filter(rule => newActiveIdTemporaryRules.contains(rule.id.get))
		val newNonActiveRules = collect.activeRules.filter(rule => newNonActiveIdRules.contains(rule.id.getOrElse(-1L)))
		
		var flashData = Map[String, String]()
		
		// Make rules inactive
		if (newNonActiveRules.nonEmpty) {
			val rulesToRemove = newNonActiveRules.map(r => new Rule(r.id.map(_.toLong), r.tag, r.content, r.collectName, r.createdAt))
			val removeRulesResult = account.removeRules(collect.name, rulesToRemove)
			if (removeRulesResult.isRight) {
				// Update with DAO
				for (rule <- newNonActiveRules) {
					val ruleToUpdate = new Rule(rule.id.map(_.toLong), rule.tag, rule.content, rule.collectName, rule.createdAt)
					ruleToUpdate.setActive(false)
					ruleDao.update(rule.id.getOrElse(-1L), ruleToUpdate)
				}
			} else {
				flashData += "error" -> removeRulesResult.left.getOrElse("")
			}
		}
		
		// Make rules active
		if (newActiveRules.nonEmpty || newActiveTemporaryRules.nonEmpty) {
			val maxRuleLength = account.getMaxRuleLength
			val filteredNewActiveTemporaryRules = newActiveTemporaryRules.filter(rule => rule.content.length <= maxRuleLength)
			val filteredNewActiveRules = newActiveRules.filter(rule => rule.content.length <= maxRuleLength)
			                                  .map(r => new Rule(r.id.map(_.toLong), r.tag, r.content, r.collectName, r.createdAt))
			
			val addRulesResult = account.addRules(collectName, filteredNewActiveTemporaryRules, filteredNewActiveRules)
			if (addRulesResult.isRight) {
				collect.removeTemporaryRulesFromList(filteredNewActiveTemporaryRules)
				collect.removeRules(filteredNewActiveRules.map(r => 
					new providers.twitter.TwitterRule(r.id.map(_.toString), r.tag, r.content, r.collect, r.createdAt)))
				
				val rulesFromResult = addRulesResult.getOrElse(Seq.empty[Rule])
				
				val socialMediaRules = rulesFromResult.map(r => 
					new providers.twitter.TwitterRule(Option(r.id.toString), r.tag, r.content, r.collect, r.createdAt))
				
				collect.addRules(socialMediaRules)
				
				// Update with DAO
				temporaryRulesDao.batchDelete(filteredNewActiveTemporaryRules.map(_.id.get))
				ruleDao.batchDelete(filteredNewActiveRules.map(_.id.getOrElse(-1L)))
				ruleDao.batchInsert(rulesFromResult)
				
				// Inform user that some rules have not been added due to size incompatibility
				if (filteredNewActiveRules.size < newActiveRules.size
					|| filteredNewActiveTemporaryRules.size < newActiveTemporaryRules.size) {
					flashData += "info" -> "Some rules have not been added because their length is greater than the authorized length for this account type."
				}
			} else {
				flashData += "error" -> addRulesResult.left.getOrElse("")
			}
		}
		
		Redirect(routes.CollectController.seeCollect(collect.name)).flashing(new Flash(flashData))
	}
	
	def createCollect: Action[AnyContent] = Action.async { implicit request: MessagesRequest[AnyContent] =>
		val errorFunction = { formWithErrors: Form[CollectData] =>
			collectDao.all().map { collects =>
				accountDao.all().map { accounts =>
					BadRequest(views.html.listCollects(collects, accounts, formWithErrors, postUrlCreateCollect))
				}
			}.flatten
		}
		
		val successFunction = { collectData: CollectData =>
			collectDao.count(collectData.name).map { nb =>
				if (nb == 0) {
					val directory = conf.get[String]("garuda.directory") + "/" + collectData.name
					val account = Await.result(accountDao.findByName(collectData.account), Duration.Inf).get
					val collect = providerManager.createCollect(collectData.name, directory, collectData.account, account.getProviderType)
					collectDao.insert(collect).map(_ =>
						Redirect(routes.CollectController.listCollects).flashing("success" -> "Collect created!")
					)
				} else {
					collectDao.all().map { collects =>
						accountDao.all().map { accounts =>
							BadRequest(views.html.listCollects(collects, accounts, collectForm.fill(collectData).withError("Name", "Collect name already exists"), postUrlCreateCollect))
								.flashing("error" -> "Collect name already exists.")
						}
					}.flatten
				}
			}.flatten
		}
		
		val formValidationResult = collectForm.bindFromRequest()
		formValidationResult.fold(errorFunction, successFunction)
	}
	
	def removeCollect(collectName: String): Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
		val collect = updateRulesOfCollect(collectName)
		Await.result(collectDao.delete(collectName), Duration.Inf)
		
		val flash = {
			if (collect.isActive) {
				new Flash(Map("error" -> "The collect is started, stop it before removing it."))
			} else {
				new Flash(Map("info" -> s"The collect $collectName has been removed."))
			}
		}
		
		Redirect(routes.CollectController.listCollects).flashing(flash)
	}
	
	private def updateRulesOfCollect(collectName: String): SocialCollect = {
		val collect = Await.result(collectDao.findByName(collectName), Duration.Inf).get
		// Populate rules if not already done
		if (collect.rules.isEmpty) {
			// Retrieve the rules of the collect
			Await.result(ruleDao.findByCollectName(collectName).map { rules =>
				temporaryRulesDao.findByCollectName(collectName).map { temporaryRules =>
					collect.initRules(rules.map(r => 
						new providers.twitter.TwitterRule(Option(r.id.toString), r.tag, r.content, r.collect, r.createdAt)
					))
					collect.setTemporaryRules(temporaryRules)
				}
			}, Duration.Inf)
			// Update the rules of the account
			Await.result(accountDao.findByName(collect.accountName).map {
				case Some(account) => {
					account.initRules(collect.name)
					if (account.getActiveRules.nonEmpty) {
						// Based on the rules of account, set to non-active the rules that are not
						val activeIds = account.getActiveRules.map(_.id.getOrElse("-1").toLong)
						collect.rules.get.foreach(rule => rule.setActive(activeIds.contains(rule.id.getOrElse(-1L))))
					}
				}
				case None => {}
			}, Duration.Inf)
		}
		collect
	}
	
	private def displayCollect(collect: SocialCollect, flash: Flash = new Flash(Map())): Future[Result] = {
		accountDao.all().map { accounts =>
			accountDao.findByName(collect.accountName).map {
				case Some(account) => {
					val token = CSRF.getToken.get
					Ok(views.html.seeCollect(collect, account, accounts, ruleForm, postUrlCreateRule(collect.name),
						postUrlAffectRules(collect.name), postRemoveAccountRulesUrl(collect.name), token.value)).flashing(flash)
				}
				case None => InternalServerError(s"Account ${collect.accountName} not found.")
			}
		}.flatten
	}
}
