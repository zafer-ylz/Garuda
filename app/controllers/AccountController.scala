package controllers

import javax.inject._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import models.SocialAccount
import models.twitter.TwitterAccountForm
import models.bluesky.BlueskyAccountForm
import services.AccountService
import providers.ProviderType

@Singleton
class AccountController @Inject()(
	val controllerComponents: ControllerComponents,
	accountService: AccountService
) extends BaseController {
	
	// Formulaires
	private val twitterForm = Form(
		mapping(
			"name" -> text,
			"identifier" -> text,
			"password" -> text,
			"apiKey" -> text,
			"apiSecret" -> text,
			"accessToken" -> text,
			"accessTokenSecret" -> text
		)(TwitterAccountForm.apply)(TwitterAccountForm.unapply)
	)
	
	private val blueskyForm = Form(
		mapping(
			"name" -> text,
			"identifier" -> text,
			"password" -> text
		)(BlueskyAccountForm.apply)(BlueskyAccountForm.unapply)
	)
	
	def index = Action { implicit request: Request[AnyContent] =>
		val accounts = accountService.getAllAccounts
		Ok(views.html.accounts.index(accounts, twitterForm, blueskyForm))
	}
	
	def createTwitterAccount = Action { implicit request: Request[AnyContent] =>
		twitterForm.bindFromRequest().fold(
			formWithErrors => {
				val accounts = accountService.getAllAccounts
				BadRequest(views.html.accounts.index(accounts, formWithErrors, blueskyForm))
			},
			accountData => {
				accountService.createTwitterAccount(accountData)
				Redirect(routes.AccountController.index).flashing("success" -> "Compte Twitter créé avec succès")
			}
		)
	}
	
	def createBlueskyAccount = Action { implicit request: Request[AnyContent] =>
		blueskyForm.bindFromRequest().fold(
			formWithErrors => {
				val accounts = accountService.getAllAccounts
				BadRequest(views.html.accounts.index(accounts, twitterForm, formWithErrors))
			},
			accountData => {
				accountService.createBlueskyAccount(accountData)
				Redirect(routes.AccountController.index).flashing("success" -> "Compte Bluesky créé avec succès")
			}
		)
	}
	
	def edit(id: String) = Action { implicit request: Request[AnyContent] =>
		accountService.getAccount(id) match {
			case Some(account) =>
				account.providerType match {
					case ProviderType.Twitter =>
						val form = twitterForm.fill(TwitterAccountForm.fromAccount(account))
						Ok(views.html.accounts.edit(account, form))
					case ProviderType.Bluesky =>
						val form = blueskyForm.fill(BlueskyAccountForm.fromAccount(account))
						Ok(views.html.accounts.edit(account, form))
					case _ =>
						NotFound("Provider non supporté")
				}
			case None =>
				NotFound("Compte non trouvé")
		}
	}
	
	def update(id: String) = Action { implicit request: Request[AnyContent] =>
		accountService.getAccount(id) match {
			case Some(account) =>
				account.providerType match {
					case ProviderType.Twitter =>
						twitterForm.bindFromRequest().fold(
							formWithErrors => BadRequest(views.html.accounts.edit(account, formWithErrors)),
							accountData => {
								accountService.updateTwitterAccount(id, accountData)
								Redirect(routes.AccountController.index).flashing("success" -> "Compte Twitter mis à jour avec succès")
							}
						)
					case ProviderType.Bluesky =>
						blueskyForm.bindFromRequest().fold(
							formWithErrors => BadRequest(views.html.accounts.edit(account, formWithErrors)),
							accountData => {
								accountService.updateBlueskyAccount(id, accountData)
								Redirect(routes.AccountController.index).flashing("success" -> "Compte Bluesky mis à jour avec succès")
							}
						)
					case _ =>
						NotFound("Provider non supporté")
				}
			case None =>
				NotFound("Compte non trouvé")
		}
	}
	
	def delete(id: String) = Action { implicit request: Request[AnyContent] =>
		accountService.deleteAccount(id)
		Redirect(routes.AccountController.index).flashing("success" -> "Compte supprimé avec succès")
	}
	
	// Méthodes anciennes, maintenues pour compatibilité
	def listAccounts = Action { implicit request: Request[AnyContent] =>
		Redirect(routes.AccountController.index)
	}
	
	def createAccount = Action { implicit request: Request[AnyContent] =>
		Redirect(routes.AccountController.index)
	}
	
	def removeAccount(accountName: String) = Action { implicit request: Request[AnyContent] =>
		// Logique de suppression de compte par nom (à implémenter si nécessaire)
		Redirect(routes.AccountController.index)
	}
	
	def updateAccount(accountName: String) = Action { implicit request: Request[AnyContent] =>
		// Logique de mise à jour de compte par nom (à implémenter si nécessaire)
		Redirect(routes.AccountController.index)
	}
}
