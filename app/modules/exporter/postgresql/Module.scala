package modules.exporter.postgresql

import models.SocialMediaMessage

/**
 * Interface pour les modules d'exportation PostgreSQL
 */
trait Module {
  /**
   * Traite un message
   */
  def processMessage(message: SocialMediaMessage): Unit
  
  /**
   * Ferme les ressources du module
   */
  def close(): Unit
} 