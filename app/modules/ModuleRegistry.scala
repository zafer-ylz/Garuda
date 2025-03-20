package modules

import javax.inject.{Inject, Singleton}
import models.SocialCollect
import providers.ProviderType
import scala.collection.mutable
import play.api.Configuration
import play.api.Logging

/**
 * Registre des modules disponibles dans l'application
 */
@Singleton
class ModuleRegistry @Inject()(configuration: Configuration) extends Logging {
  
  // Map des modules disponibles par identifiant
  private val modules: mutable.Map[String, ModuleDefinition] = mutable.Map.empty
  
  // Map des instances actives de modules par collecte
  private val activeModules: mutable.Map[String, Seq[Module]] = mutable.Map.empty
  
  /**
   * Enregistre un module dans le registre
   */
  def registerModule(moduleDefinition: ModuleDefinition): Unit = {
    modules.put(moduleDefinition.id, moduleDefinition)
    logger.info(s"Module '${moduleDefinition.id}' registered: ${moduleDefinition.name}")
  }
  
  /**
   * Récupère un module par son identifiant
   */
  def getModule(id: String): Option[ModuleDefinition] = modules.get(id)
  
  /**
   * Récupère tous les modules enregistrés
   */
  def getAllModules(): Seq[ModuleDefinition] = modules.values.toSeq
  
  /**
   * Récupère les modules compatibles avec un provider donné
   */
  def getModulesForProvider(providerType: ProviderType): Seq[ModuleDefinition] = {
    modules.values.filter(_.supportedProviders.contains(providerType)).toSeq
  }
  
  /**
   * Démarre une instance de module pour une collecte donnée
   */
  def startModule(moduleId: String, collect: SocialCollect, config: ModuleConfiguration): Option[Module] = {
    modules.get(moduleId).flatMap { moduleDefinition =>
      if (moduleDefinition.supportedProviders.contains(collect.providerType)) {
        val moduleInstance = moduleDefinition.factory.createModule(collect, config)
        val collectModules = activeModules.getOrElse(collect.name, Seq.empty)
        activeModules.put(collect.name, collectModules :+ moduleInstance)
        Some(moduleInstance)
      } else {
        logger.warn(s"Module '${moduleDefinition.id}' does not support provider ${collect.providerType}")
        None
      }
    }
  }
  
  /**
   * Arrête un module actif
   */
  def stopModule(collectName: String, moduleId: String): Boolean = {
    activeModules.get(collectName).flatMap { modules =>
      val (modulesToStop, remainingModules) = modules.partition(_.getClass.getSimpleName == moduleId)
      modulesToStop.foreach(_.stop())
      activeModules.put(collectName, remainingModules)
      Some(modulesToStop.nonEmpty)
    }.getOrElse(false)
  }
  
  /**
   * Récupère les modules actifs pour une collecte
   */
  def getActiveModules(collectName: String): Seq[Module] = {
    activeModules.getOrElse(collectName, Seq.empty)
  }
  
  /**
   * Vérifie si un module est actif pour une collecte
   */
  def isModuleActive(collectName: String, moduleId: String): Boolean = {
    activeModules.get(collectName).exists(_.exists(_.getClass.getSimpleName == moduleId))
  }
}

/**
 * Définition d'un module
 */
case class ModuleDefinition(
  id: String,
  name: String,
  description: String,
  supportedProviders: Seq[ProviderType],
  factory: ModuleFactory,
  configurationClass: Class[_ <: ModuleConfiguration]
)

/**
 * Factory pour créer des instances de modules
 */
trait ModuleFactory {
  def createModule(collect: SocialCollect, config: ModuleConfiguration): Module
}

/**
 * Configuration de base pour un module
 */
trait ModuleConfiguration {
  def moduleId: String
  def collectName: String
} 