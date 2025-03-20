package providers.bluesky

import providers._
import models.{SocialMediaAccount, SocialMediaCollect, SocialMediaMessage}
import java.io.FileWriter
import scala.collection.mutable.ArrayBuffer

class BlueskyStreamingConnection(
  private val accessToken: String,
  val account: SocialMediaAccount,
  private var fileWriter: Option[FileWriter] = None
) extends StreamingConnection {
  private var listeners: ArrayBuffer[MessageListener] = ArrayBuffer.empty
  private var isStreaming: Boolean = false
  
  override def collect: SocialMediaCollect = {
    account.getActiveCollect.getOrElse(
      throw new IllegalStateException("No active collect for this connection")
    )
  }
  
  override def isActive: Boolean = isStreaming
  
  override def stream(): StreamingConnection = {
    if (!isStreaming) {
      isStreaming = true
      // TODO: Implement Bluesky streaming using AT Protocol
      // This is a placeholder implementation
      new Thread(() => {
        try {
          while (isStreaming) {
            // Simuler la réception de messages
            Thread.sleep(1000)
            val message = BlueskyProvider.normalizeMessage("Placeholder Bluesky message")
            notifyListeners(message)
            writeToFile(message)
          }
        } catch {
          case e: Exception =>
            notifyError(e)
        } finally {
          notifyComplete()
        }
      }).start()
    }
    this
  }
  
  override def executeListeners(): Unit = {
    // Les listeners sont déjà exécutés dans le thread de streaming
  }
  
  override def shutdown(): Unit = {
    isStreaming = false
    fileWriter.foreach(_.close())
    fileWriter = None
    notifyComplete()
  }
  
  override def addMessageListener(listener: MessageListener): Unit = {
    listeners += listener
  }
  
  override def setFileWriter(writer: FileWriter): Unit = {
    fileWriter = Some(writer)
  }
  
  private def notifyListeners(message: SocialMediaMessage): Unit = {
    listeners.foreach(_.onMessage(message))
  }
  
  private def notifyError(error: Throwable): Unit = {
    listeners.foreach(_.onError(error))
  }
  
  private def notifyComplete(): Unit = {
    listeners.foreach(_.onComplete())
  }
  
  private def writeToFile(message: SocialMediaMessage): Unit = {
    fileWriter.foreach { writer =>
      writer.write(message.toJson + "\n")
      writer.flush()
    }
  }
} 