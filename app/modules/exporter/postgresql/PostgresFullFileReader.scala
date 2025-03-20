package modules.exporter.postgresql

import modules.FullFileReader
import modules.MessageAdapter

/**
 * Lecteur de fichier complet pour PostgreSQL
 */
class PostgresFullFileReader(fileName: String, 
                           val config: PostgresConfiguration,
                           val module: PostgresModule) extends FullFileReader(fileName) {
  
  private val postgresInsertion = new PostgresInsertion(config)
  
  /**
   * Action à exécuter lorsqu'une ligne est lue du fichier
   */
  override def onLine(line: String): Unit = {
    try {
      // Utilise l'adaptateur de message pour convertir au format standardisé
      MessageAdapter.convertMessage(line, module.collect.providerType).foreach { message =>
        // Envoie le message au module pour traitement
        module.processMessage(message)
      }
    } catch {
      case t: Throwable => logger.warn("Error : " + t.getMessage, t)
    }
  }
  
  /**
   * Action à exécuter à la fermeture du fichier
   */
  override def onClose(): Unit = {
    postgresInsertion.insertBatch()
    postgresInsertion.close()
  }
}
