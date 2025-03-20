package modules.exporter.postgresql

import java.io.File
import java.nio.file._
import models.SocialCollect
import modules.Module
import play.api.Logging

class PostgresFileListener(
  val observableFile: File,
  val config: PostgresConfig,
  val module: Module
) extends Logging {
  private val watchService = FileSystems.getDefault.newWatchService()
  private val path = observableFile.toPath
  private var isRunning = false
  
  // Enregistre le répertoire pour la surveillance
  path.register(
    watchService,
    StandardWatchEventKinds.ENTRY_CREATE,
    StandardWatchEventKinds.ENTRY_MODIFY
  )
  
  /**
   * Démarre l'écoute des fichiers
   */
  def start(): Unit = {
    isRunning = true
    new Thread(() => {
      while (isRunning) {
        try {
          val key = watchService.take()
          for (event <- key.pollEvents().asScala) {
            val kind = event.kind()
            val path = event.context().asInstanceOf[Path]
            
            if (kind == StandardWatchEventKinds.ENTRY_CREATE || 
                kind == StandardWatchEventKinds.ENTRY_MODIFY) {
              val file = this.path.resolve(path).toFile
              if (!file.getAbsolutePath.endsWith("Errors")) {
                val reader = new PostgresFileReader(file.getAbsolutePath, config)
                reader.readFile()
              }
            }
          }
          key.reset()
        } catch {
          case e: Exception =>
            logger.error("Error watching files", e)
        }
      }
    }).start()
  }
  
  /**
   * Arrête l'écoute des fichiers
   */
  def stop(): Unit = {
    isRunning = false
    watchService.close()
  }
}
