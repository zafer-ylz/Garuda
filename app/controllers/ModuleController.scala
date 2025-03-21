package controllers

import cache.ModuleCache
import dao.CollectDao
import javax.inject.Inject
import models.Modules
import modules.dao.{ModuleFileProcessedDao, PostgresConfigurationDao}
import modules.exporter.postgresql.{PostgresConfiguration, PostgresConfigurationForm, PostgresModule}
import play.api.data._
import play.api.mvc._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

class ModuleController @Inject()(postgresConfigurationDao: PostgresConfigurationDao, collectDao: CollectDao,
								 moduleFileProcessedDao: ModuleFileProcessedDao, cc: MessagesControllerComponents)
								(implicit executionContext: ExecutionContext) extends MessagesAbstractController(cc) {
	
	/**
	 * See the modules for a given collect.
	 *
	 * @param collectName the collect for which to see the modules.
	 * @return
	 */
	def seeModules(collectName: String): Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
		val collect = Await.result(collectDao.findByName(collectName), Duration.Inf).get
		// Get modules
		val modules = new Modules()
		
		val postgresConfig = Await.result(postgresConfigurationDao.findByCollect(collect.name), Duration.Inf)
		if (postgresConfig.isDefined) {
			modules.postgresExporterConfiguration = postgresConfig
		}
		val postgresExporterModule = ModuleCache.get(collectName)
		if (postgresExporterModule.isDefined) {
			modules.postgresExporterModule = Some(postgresExporterModule.get.asInstanceOf[PostgresModule])
		}
		// Generate form if already existing
		val postgresExporterForm = if (modules.postgresExporterConfiguration.isDefined) {
			PostgresConfigurationForm.form.fill(modules.postgresExporterConfiguration.get)
		} else {
			PostgresConfigurationForm.form
		}
		// Generate view
		Ok(views.html.modules.modulesCollect(collect, modules, postgresExporterForm))
	}
	
	/**
	 * Add the configuration for a postgres exporter module.
	 *
	 * @param collectName the collect for which to add the configuration.
	 * @return
	 */
	def addPostgresConfiguration(collectName: String): Action[AnyContent] = Action.async { implicit request: MessagesRequest[AnyContent] =>
		val errorFunction = { formWithErrors: Form[PostgresConfiguration] =>
			// This is the bad case, where the form had validation errors.
			// Let's show the user the form again, with the errors highlighted.
			// Note how we pass the form with errors to the template.
			val flash = formWithErrors.errors.foldLeft("")((s, e) => s"$s${e.key}: ${e.message}\n")
			Future(Redirect(routes.ModuleController.seeModules(collectName)).flashing("error" -> flash))
		}
		
		val successFunction = { postgresConfiguration: PostgresConfiguration =>
			// This is the good case, where the form was successfully parsed as an Account object.
			postgresConfigurationDao.insert(postgresConfiguration).map(_ =>
				Redirect(routes.ModuleController.seeModules(collectName)).flashing("success" -> "PostgreSQL exporter module created!")
			)
		}
		
		val form = PostgresConfigurationForm.form
		val formValidationResult = form.bindFromRequest()
		formValidationResult.fold(errorFunction, successFunction)
	}
	
	/**
	 * Update the configuration for a postgres exporter module.
	 *
	 * @param collectName the collect for which to update the configuration.
	 * @return
	 */
	def updatePostgresConfiguration(collectName: String): Action[AnyContent] = Action.async { implicit request: MessagesRequest[AnyContent] =>
		val errorFunction = { formWithErrors: Form[PostgresConfiguration] =>
			// This is the bad case, where the form had validation errors.
			// Let's show the user the form again, with the errors highlighted.
			// Note how we pass the form with errors to the template.
			val flash = formWithErrors.errors.foldLeft("")((s, e) => s"$s${e.key}: ${e.message}\n")
			Future(Redirect(routes.ModuleController.seeModules(collectName)).flashing("error" -> flash))
		}
		
		val successFunction = { postgresConfiguration: PostgresConfiguration =>
			// This is the good case, where the form was successfully parsed as an Account object.
			postgresConfigurationDao.update(collectName, postgresConfiguration).map(_ =>
				Redirect(routes.ModuleController.seeModules(collectName)).flashing("success" -> "PostgreSQL exporter module updated!")
			)
		}
		
		val form = PostgresConfigurationForm.form
		val formValidationResult = form.bindFromRequest()
		formValidationResult.fold(errorFunction, successFunction)
	}
	
	/**
	 * Start the postgres exporter module for a given collect.
	 *
	 * @param collectName the collect for which to start the module.
	 * @return
	 */
	def startPostgresExporterModule(collectName: String): Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
		val postgresExporterModule = ModuleCache.get(collectName)
		if (postgresExporterModule.isDefined) {
			Redirect(routes.ModuleController.seeModules(collectName)).flashing("error" -> "A PostgreSQL exporter module is already running for this collect.")
		} else {
			val postgresConfiguration = Await.result(postgresConfigurationDao.findByCollect(collectName), Duration.Inf)
			if (postgresConfiguration.isDefined) {
				val collect = Await.result(collectDao.findByName(collectName), Duration.Inf).get
				val adaptedCollect = collect.adaptToSocialCollect
				val module = new PostgresModule(adaptedCollect, postgresConfiguration.get.toPostgresConfig, moduleFileProcessedDao)
				ModuleCache.add(collectName, module)
				Redirect(routes.ModuleController.seeModules(collectName)).flashing("success" -> "PostgreSQL exporter module started!")
			} else {
				Redirect(routes.ModuleController.seeModules(collectName)).flashing("error" -> "There is no configuration for a PostgreSQL exporter module for this collect.")
			}
		}
	}
	
	/**
	 * Stop the postgres exporter module for a given collect.
	 *
	 * @param collectName the collect for which to stop the module.
	 * @return
	 */
	def stopPostgresExporterModule(collectName: String): Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
		val postgresExporterModule = ModuleCache.get(collectName)
		if (postgresExporterModule.isDefined) {
			postgresExporterModule.get.stop()
			Redirect(routes.ModuleController.seeModules(collectName)).flashing("success" -> "PostgreSQL exporter module stopped!")
		} else {
			Redirect(routes.ModuleController.seeModules(collectName)).flashing("error" -> "There is no running PostgreSQL exporter module for this collect.")
		}
	}
}
