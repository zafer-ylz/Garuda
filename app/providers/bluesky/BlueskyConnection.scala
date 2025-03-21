package providers.bluesky

import models.SocialAccount
import providers.{ProviderType, SocialMediaRule}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Left, Right}
import com.atproto.{Client, ClientBuilder}
import com.atproto.server.CreateSessionRequest
import com.atproto.server.CreateSessionResponse
import com.atproto.server.RefreshSessionResponse
import com.atproto.server.Session
import com.atproto.server.SessionManager
import play.api.cache.AsyncCacheApi
import play.api.Logging
import javax.inject.Inject
import scala.concurrent.duration._
import scala.util.Try
import org.joda.time.DateTime

class BlueskyConnection @Inject()(account: SocialAccount, cache: AsyncCacheApi) extends Logging {
  private var client: Option[Client] = None
  private var session: Option[Session] = None
  private val CACHE_PREFIX = "bluesky_token_"
  private val TOKEN_VALIDITY = 1.day
  
  /**
   * Valide les identifiants en tentant de créer une session
   */
  def validateCredentials(): Future[Either[String, Unit]] = {
    implicit val ec: ExecutionContext = ExecutionContext.global
    
    Future {
      try {
        // Vérifier si une session est déjà stockée en cache
        val cacheKey = s"$CACHE_PREFIX${account.getIdentifier}"
        val cachedSession = cache.get[Session](cacheKey)
        
        if (cachedSession.isCompleted && cachedSession.value.exists(_.isSuccess) && cachedSession.value.get.get.isDefined) {
          // Utiliser la session mise en cache
          val sessionFromCache = cachedSession.value.get.get.get
          this.session = Some(sessionFromCache)
          
          // Créer un client avec les paramètres de la session
          val clientInstance = ClientBuilder.create()
            .setEndpoint("https://bsky.social")
            .build()
          
          this.client = Some(clientInstance)
          
          logger.info(s"Session Bluesky récupérée du cache pour ${account.getIdentifier}")
          Right(())
        } else {
          // Créer une nouvelle session
          val clientInstance = ClientBuilder.create()
            .setEndpoint("https://bsky.social")
            .build()
            
          val newSession = clientInstance.createSession(
            CreateSessionRequest(
              identifier = account.getIdentifier,
              password = account.getPassword
            )
          )
          
          // Stocker en cache
          cache.set(cacheKey, newSession, TOKEN_VALIDITY)
          
          this.client = Some(clientInstance)
          this.session = Some(newSession)
          
          logger.info(s"Nouvelle session Bluesky créée pour ${account.getIdentifier}")
          Right(())
        }
      } catch {
        case e: Exception => 
          logger.error(s"Échec de validation des identifiants Bluesky: ${e.getMessage}", e)
          Left(s"Failed to validate Bluesky credentials: ${e.getMessage}")
      }
    }
  }
  
  /**
   * Rafraîchit le token de session
   */
  def refreshSession(): Future[Either[String, Unit]] = {
    implicit val ec: ExecutionContext = ExecutionContext.global
    
    Future {
      try {
        if (client.isDefined && session.isDefined) {
          val sessionManager = new SessionManager(client.get)
          val refreshedSession = sessionManager.refreshSession(session.get)
          
          // Mettre à jour le cache
          val cacheKey = s"$CACHE_PREFIX${account.getIdentifier}"
          cache.set(cacheKey, refreshedSession, TOKEN_VALIDITY)
          
          this.session = Some(refreshedSession)
          logger.info(s"Session Bluesky rafraîchie pour ${account.getIdentifier}")
          Right(())
        } else {
          // Si pas de session, tenter d'en créer une nouvelle
          validateCredentials().flatMap {
            case Right(_) => Future.successful(Right(()))
            case Left(error) => Future.successful(Left(error))
          }
        }
      } catch {
        case e: Exception => 
          logger.error(s"Échec du rafraîchissement de la session Bluesky: ${e.getMessage}", e)
          Left(s"Failed to refresh Bluesky session: ${e.getMessage}")
      }
    }
  }
  
  /**
   * Vérification de la validité de la session
   */
  def isSessionValid(): Boolean = {
    session.exists { currentSession =>
      Try {
        val expiresAt = currentSession.getRefreshJwt.getOrElse("").split("\\.")(1)
        val decoded = java.util.Base64.getDecoder.decode(expiresAt)
        val json = new String(decoded)
        val expiryTime = json.split("\"exp\":")(1).split(",")(0).toLong * 1000
        expiryTime > System.currentTimeMillis()
      }.getOrElse(false)
    }
  }
  
  /**
   * Ajoute des règles de collecte
   */
  def addRules(collectName: String, rules: Seq[SocialMediaRule]): Future[Either[String, Seq[SocialMediaRule]]] = {
    implicit val ec: ExecutionContext = ExecutionContext.global
    
    Future {
      try {
        if (!isSessionValid()) {
          refreshSession()
        }
        // TODO: Implémenter l'ajout de règles via l'API Bluesky
        Right(rules)
      } catch {
        case e: Exception => Left(s"Failed to add Bluesky rules: ${e.getMessage}")
      }
    }
  }
  
  /**
   * Récupère les règles actives
   */
  def getActiveRules(collectName: String): Future[Either[String, Seq[SocialMediaRule]]] = {
    implicit val ec: ExecutionContext = ExecutionContext.global
    
    Future {
      try {
        if (!isSessionValid()) {
          refreshSession()
        }
        // TODO: Implémenter la récupération des règles via l'API Bluesky
        Right(Seq.empty)
      } catch {
        case e: Exception => Left(s"Failed to get Bluesky rules: ${e.getMessage}")
      }
    }
  }
  
  /**
   * Démarrer un stream de données
   */
  def stream(): Future[Either[String, BlueskyStreamListener]] = {
    implicit val ec: ExecutionContext = ExecutionContext.global
    
    Future {
      try {
        if (!isSessionValid()) {
          refreshSession()
        }
        
        val listener = new BlueskyStreamListener(client.get, session.get)
        listener.start()
        Right(listener)
      } catch {
        case e: Exception => Left(s"Failed to start Bluesky stream: ${e.getMessage}")
      }
    }
  }
  
  /**
   * Obtention du client Bluesky
   */
  def getClient: Option[Client] = client
  
  /**
   * Obtention de la session active
   */
  def getSession: Option[Session] = session
} 