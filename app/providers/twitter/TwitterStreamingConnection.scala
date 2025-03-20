package providers.twitter

import providers._
import models.{SocialMediaAccount, SocialMediaCollect, SocialMediaMessage}
import com.twitter.clientlib.api.TwitterApi
import java.io.FileWriter
import scala.collection.mutable.ArrayBuffer

class TwitterStreamingConnection(
  private val apiInstance: TwitterApi,
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
      // Configure le streaming avec les champs nécessaires
      val tweetFields = Set(
        "attachments",
        "author_id",
        "context_annotations",
        "conversation_id",
        "created_at",
        "entities",
        "geo",
        "id",
        "in_reply_to_user_id",
        "lang",
        "public_metrics",
        "referenced_tweets",
        "reply_settings",
        "source",
        "text",
        "withheld"
      )
      
      val expansions = Set(
        "attachments.media_keys",
        "attachments.poll_ids",
        "author_id",
        "entities.mentions.username",
        "geo.place_id",
        "in_reply_to_user_id",
        "referenced_tweets.id",
        "referenced_tweets.id.author_id"
      )
      
      val mediaFields = Set(
        "alt_text",
        "duration_ms",
        "height",
        "media_key",
        "preview_image_url",
        "type",
        "url",
        "variants",
        "width"
      )
      
      val pollFields = Set(
        "duration_minutes",
        "end_datetime",
        "id",
        "options",
        "voting_status"
      )
      
      val userFields = Set(
        "created_at",
        "description",
        "entities",
        "id",
        "location",
        "name",
        "pinned_tweet_id",
        "profile_image_url",
        "protected",
        "public_metrics",
        "url",
        "username",
        "verified",
        "withheld"
      )
      
      val placeFields = Set(
        "contained_within",
        "country",
        "country_code",
        "full_name",
        "geo",
        "id",
        "name",
        "place_type"
      )
      
      // Configure le streaming avec les règles actives
      val stream = apiInstance.tweets().searchStream()
        .tweetFields(tweetFields)
        .expansions(expansions)
        .mediaFields(mediaFields)
        .pollFields(pollFields)
        .userFields(userFields)
        .placeFields(placeFields)
        .execute()
      
      // Démarrer le traitement du stream dans un thread séparé
      new Thread(() => {
        try {
          val reader = new java.io.BufferedReader(
            new java.io.InputStreamReader(stream)
          )
          
          var line: String = null
          while (isStreaming && { line = reader.readLine(); line != null }) {
            try {
              val message = TwitterProvider.normalizeMessage(line)
              notifyListeners(message)
              writeToFile(message)
            } catch {
              case e: Exception =>
                notifyError(e)
            }
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