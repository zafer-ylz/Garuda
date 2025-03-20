package modules.exporter.postgresql

import modules.FileListener
import modules.MessageAdapter
import twitter.ObservableFile

/**
 * Écouteur de fichier pour PostgreSQL
 */
class PostgresFileListener(override var observableFile: ObservableFile,
                          val config: PostgresConfiguration,
                          val module: PostgresModule) extends FileListener(observableFile) {
  
  private var postgresInsertion: PostgresInsertion = _
  
  /**
   * Action à exécuter lorsqu'une ligne est ajoutée au fichier observé
   */
  override def onEvent(line: String): Unit = {
    // Utilise l'adaptateur de message pour convertir au format standardisé
    MessageAdapter.convertMessage(line, module.collect.providerType).foreach { message =>
      // Envoie le message au module pour traitement
      module.processMessage(message)
    }
  }
  
  /**
   * Action à exécuter lors du changement de fichier
   */
  override def onFileChange(): Unit = {
    if (postgresInsertion != null) {
      postgresInsertion.insertBatch()
      postgresInsertion.close()
      postgresInsertion = null
    }
  }
  
  /**
   * Action à exécuter à l'arrêt de l'écouteur
   */
  override def onStop(): Unit = {
    onFileChange()
  }
}
