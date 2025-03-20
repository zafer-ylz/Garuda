package modules.exporter.postgresql

import java.io.{BufferedReader, FileReader}
import models.SocialMediaMessage
import play.api.Logging

class PostgresFileReader(filePath: String, config: PostgresConfig) extends Logging {
  private val reader = new BufferedReader(new FileReader(filePath))
  private val postgresInsertion = new PostgresInsertion(config)
  
  /**
   * Lit le fichier ligne par ligne
   */
  def readFile(): Unit = {
    try {
      var line: String = null
      while ({ line = reader.readLine(); line != null }) {
        try {
          val message = SocialMediaMessage.fromJson(line)
          message.providerType match {
            case "twitter" =>
              postgresInsertion.insertLine(line)
            case "bluesky" =>
              postgresInsertion.insertBlueskyMessage(message)
            case _ =>
              logger.warn(s"Provider type ${message.providerType} not supported")
          }
        } catch {
          case e: Exception =>
            logger.error(s"Error processing line: $line", e)
        }
      }
    } finally {
      reader.close()
      postgresInsertion.close()
    }
  }
} 