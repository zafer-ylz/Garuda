package modules

import models.SocialMediaMessage
import providers.ProviderType

/**
 * Interface pour les plugins de modules
 */
trait ModulePlugin {
  /**
   * Nom unique du plugin
   */
  def name: String
  
  /**
   * Description du plugin
   */
  def description: String
  
  /**
   * Version du plugin
   */
  def version: String
  
  /**
   * Liste des providers supportés
   */
  def supportedProviders: Seq[ProviderType]
  
  /**
   * Traite un message
   */
  def processMessage(message: SocialMediaMessage): Unit
  
  /**
   * Initialise le plugin
   */
  def initialize(): Unit
  
  /**
   * Arrête le plugin
   */
  def shutdown(): Unit
} 