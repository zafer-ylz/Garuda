package controllers

import dao.{AccountDao, CollectDao, RuleDao, TemporaryRuleDao}
import javax.inject.{Inject, Singleton}
import models.SocialCollect
import providers.SocialMediaRule
import models.CollectForm.{CollectData, form => collectForm}
import models.TemporaryRuleForm.{TemporaryRuleData, form => ruleForm}
import models.{Collect, Rule, TemporaryRule}
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
		displayCollect(collect.asInstanceOf[Collect])
	}
	
	def startCollect(collectName: String): Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
		val collect = updateRulesOfCollect(collectName)
		val account = Await.result(accountDao.findByName(collect.accountName), Duration.Inf).get
		
		val collectStarted = account.startCollect(collect.asInstanceOf[Collect])
		
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
		
		val collectStopped = account.stopCollect(collect.asInstanceOf[Collect])
		
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
			
			// Conversion de SocialMediaRule à Rule
			val rulesToRemove = account.activeRules.filter(rule => rulesIds.contains(rule.id.map(_.toLong).getOrElse(-1L)))
				.map(r => {
					val id = r.id.map(_.toLong).getOrElse(-1L)
					val rule = new Rule(id, r.tag, r.content, r.collectName)
					rule.setActive(r.isActive)
					rule
				}).toList
			
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
		
		val collect = updateRulesOfCollect(collectName).asInstanceOf[Collect]
		val account = Await.result(accountDao.findByName(collect.accountName), Duration.Inf).get
		
		val newActiveRules = collect.nonActiveRules.filter(rule => newActiveIdRules.contains(rule.id))
		val newActiveTemporaryRules = collect.temporaryRules.getOrElse(List.empty[TemporaryRule]).filter(rule => newActiveIdTemporaryRules.contains(rule.id.get))
		val newNonActiveRules = collect.activeRules.filter(rule => newNonActiveIdRules.contains(rule.id.map(_.toLong).getOrElse(-1L)))
		
		var flashData = Map[String, String]()
		
		// Make rules inactive
		if (newNonActiveRules.nonEmpty) {
			val removeRulesResult = account.removeRules(collect.name, newNonActiveRules)
			if (removeRulesResult.isRight) {
				// Update with DAO
				for (rule <- newNonActiveRules) {
					rule.setActive(false)
					ruleDao.update(rule.id, rule)
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
			
			val addRulesResult = account.addRules(collectName, filteredNewActiveTemporaryRules, filteredNewActiveRules)
			if (addRulesResult.isRight) {
				collect.removeTemporaryRules(filteredNewActiveTemporaryRules)
				collect.removeRules(filteredNewActiveRules)
				
				val rulesFromResult = addRulesResult.getOrElse(Seq.empty[Rule])
				
				// Update with DAO
				temporaryRulesDao.batchDelete(filteredNewActiveTemporaryRules.map(_.id.get))
				ruleDao.batchDelete(filteredNewActiveRules.map(_.id))
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
	
	private def updateRulesOfCollect(collectName: String): Collect = {
		val collect = Await.result(collectDao.findByName(collectName), Duration.Inf).get
		// Populate rules if not already done
		if (collect.rules.isEmpty) {
			// Retrieve the rules of the collect
			Await.result(ruleDao.findByCollectName(collectName).map { rules =>
				temporaryRulesDao.findByCollectName(collectName).map { temporaryRules =>
					collect.initRules(rules, temporaryRules)
				}
			}, Duration.Inf)
			// Update the rules of the account
			Await.result(accountDao.findByName(collect.accountName).map {
				case Some(account) => {
					account.initRules(collect.name)
					if (account.activeRules.nonEmpty) {
						// Based on the rules of account, set to non-active the rules that are not
						val activeIds = account.activeRules.map(_.id.map(_.toLong).getOrElse(-1L))
						collect.rules.get.foreach(rule => rule.setActive(activeIds.contains(rule.id)))
					}
				}
				case None => {}
			}, Duration.Inf)
		}
		collect
	}
	
	def createRule(collectName: String): Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
		// Code à implémenter pour créer une règle
		Redirect(routes.CollectController.seeCollect(collectName))
	}
	
	/**
	 * Met à jour le compte associé à une collecte
	 */
	def updateCollect(collectName: String): Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
		val collectData = collectForm.bindFromRequest().get
		val collect = Await.result(collectDao.findByName(collectName), Duration.Inf).get
		
		// Vérifier si le compte existe
		val accountExists = Await.result(accountDao.findByName(collectData.account), Duration.Inf).isDefined
		
		if (accountExists) {
			val updatedCollect = new Collect(collect.name, collect.directory, collectData.account, collect.providerType, collect.isActive)
			Await.result(collectDao.update(collectName, updatedCollect), Duration.Inf)
			Redirect(routes.CollectController.seeCollect(collectName)).flashing("success" -> "Compte mis à jour avec succès")
		} else {
			Redirect(routes.CollectController.seeCollect(collectName)).flashing("error" -> s"Le compte ${collectData.account} n'existe pas")
		}
	}
	
	private def displayCollect(collect: Collect, flash: Flash = new Flash(Map())): Future[Result] = {
		accountDao.all().map { accounts =>
			accountDao.findByName(collect.accountName).map {
				case Some(account) => {
					implicit val request = Request(FakeRequest(), "")
					val token = CSRF.getToken(request).get
					Ok(views.html.seeCollect(collect, account, accounts, ruleForm, postUrlCreateRule(collect.name),
						postUrlAffectRules(collect.name), postRemoveAccountRulesUrl(collect.name), token.value)).flashing(flash)
				}
				case None => InternalServerError(s"Account ${collect.accountName} not found.")
			}
		}.flatten
	}
}
