package providers.bluesky

import com.atproto.{Client, Session}
import com.atproto.api.{TimelineResponse, TimelineRequest}
import com.atproto.api.models.{Post, FeedViewPost}
import models.SocialMediaMessage
import providers.ProviderType
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Left, Right}
import java.time.DateTime

class BlueskyStreamListener(client: Client, session: Session) {
  private var isRunning: Boolean = false
  private var lastCursor: Option[String] = None
  
  /**
   * Démarre l'écoute du flux de données
   */
  def start(): Future[Either[String, Unit]] = {
    implicit val ec: ExecutionContext = ExecutionContext.global
    
    Future {
      try {
        isRunning = true
        pollTimeline()
        Right(())
      } catch {
        case e: Exception => Left(s"Failed to start Bluesky stream: ${e.getMessage}")
      }
    }
  }
  
  /**
   * Arrête l'écoute du flux de données
   */
  def stop(): Future[Either[String, Unit]] = {
    implicit val ec: ExecutionContext = ExecutionContext.global
    
    Future {
      try {
        isRunning = false
        Right(())
      } catch {
        case e: Exception => Left(s"Failed to stop Bluesky stream: ${e.getMessage}")
      }
    }
  }
  
  /**
   * Récupère les posts du timeline
   */
  private def pollTimeline(): Unit = {
    implicit val ec: ExecutionContext = ExecutionContext.global
    
    while (isRunning) {
      try {
        val request = TimelineRequest(
          limit = 50,
          cursor = lastCursor
        )
        
        val response = client.getTimeline(request)
        processTimelineResponse(response)
        
        // Mise à jour du curseur pour la prochaine requête
        lastCursor = response.cursor
        
        // Attente avant la prochaine requête
        Thread.sleep(5000) // 5 secondes entre chaque requête
      } catch {
        case e: Exception =>
          println(s"Error polling Bluesky timeline: ${e.getMessage}")
          Thread.sleep(30000) // 30 secondes en cas d'erreur
      }
    }
  }
  
  /**
   * Traite la réponse du timeline
   */
  private def processTimelineResponse(response: TimelineResponse): Unit = {
    response.feed.foreach { post =>
      val message = convertToMessage(post)
      // TODO: Envoyer le message au système de traitement
      println(s"Received Bluesky post: ${message.content}")
    }
  }
  
  /**
   * Convertit un post Bluesky en message standard
   */
  private def convertToMessage(post: FeedViewPost): SocialMediaMessage = {
    SocialMediaMessage(
      id = post.post.uri,
      providerType = ProviderType.Bluesky,
      content = post.post.record.text,
      authorId = post.post.author.did,
      createdAt = DateTime.parse(post.post.indexedAt)
    )
  }
} 