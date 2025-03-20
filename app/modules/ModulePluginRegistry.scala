package modules

import play.api.Logging
import providers.ProviderType

import scala.collection.mutable

/**
 * Registre des plugins de modules
 */
object ModulePluginRegistry extends Logging {
  private val plugins = mutable.Map[String, ModulePlugin]()
  
  /**
   * Enregistre un nouveau plugin
   */
  def registerPlugin(plugin: ModulePlugin): Unit = {
    if (plugins.contains(plugin.name)) {
      logger.warn(s"Plugin ${plugin.name} is already registered")
      return
    }
    
    try {
      plugin.initialize()
      plugins(plugin.name) = plugin
      logger.info(s"Plugin ${plugin.name} v${plugin.version} registered successfully")
    } catch {
      case e: Exception =>
        logger.error(s"Failed to initialize plugin ${plugin.name}", e)
        throw e
    }
  }
  
  /**
   * Désenregistre un plugin
   */
  def unregisterPlugin(pluginName: String): Unit = {
    plugins.get(pluginName).foreach { plugin =>
      try {
        plugin.shutdown()
        plugins.remove(pluginName)
        logger.info(s"Plugin $pluginName unregistered successfully")
      } catch {
        case e: Exception =>
          logger.error(s"Failed to shutdown plugin $pluginName", e)
          throw e
      }
    }
  }
  
  /**
   * Récupère un plugin par son nom
   */
  def getPlugin(pluginName: String): Option[ModulePlugin] = {
    plugins.get(pluginName)
  }
  
  /**
   * Récupère tous les plugins supportant un provider donné
   */
  def getPluginsForProvider(providerType: ProviderType): Seq[ModulePlugin] = {
    plugins.values.filter(_.supportedProviders.contains(providerType)).toSeq
  }
  
  /**
   * Récupère tous les plugins enregistrés
   */
  def getAllPlugins: Seq[ModulePlugin] = {
    plugins.values.toSeq
  }
  
  /**
   * Arrête tous les plugins
   */
  def shutdownAll(): Unit = {
    plugins.keys.foreach(unregisterPlugin)
  }
} 