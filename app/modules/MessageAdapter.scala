package modules

import models.SocialMediaMessage
import providers.ProviderType
import play.api.Logging
import models.tweet.Tweet
import models.tweet.User
import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json}

/**
 * Adaptateur pour convertir différents formats de messages en format standardisé
 */
object MessageAdapter extends Logging {
  
  /**
   * Convertit un message brut en SocialMediaMessage normalisé
   */
  def convertMessage(rawMessage: String, providerType: ProviderType): Option[SocialMediaMessage] = {
    try {
      providerType match {
        case ProviderType.Twitter => convertTwitterMessage(rawMessage)
        case ProviderType.Bluesky => convertBlueskyMessage(rawMessage)
        case _ => 
          logger.warn(s"Unsupported provider type: $providerType")
          None
      }
    } catch {
      case e: Exception =>
        logger.error(s"Error converting message: ${e.getMessage}", e)
        None
    }
  }
  
  /**
   * Convertit un message Twitter en format standardisé
   */
  private def convertTwitterMessage(rawMessage: String): Option[SocialMediaMessage] = {
    try {
      val tweet = new Tweet(rawMessage)
      
      if (tweet.id.isEmpty || tweet.text.isEmpty) {
        return None
      }
      
      // Extracting metadata
      val metadata = Map[String, Any](
        "source" -> tweet.source.getOrElse(""),
        "lang" -> tweet.lang.getOrElse(""),
        "replyCount" -> tweet.replyCount.getOrElse(0),
        "retweetCount" -> tweet.retweetCount.getOrElse(0),
        "likeCount" -> tweet.likeCount.getOrElse(0),
        "quoteCount" -> tweet.quoteCount.getOrElse(0),
        "isRetweet" -> tweet.isRetweet,
        "isQuote" -> tweet.isQuote,
        "hashtags" -> tweet.hashtags.map(_.tag.getOrElse("")).mkString(","),
        "urls" -> tweet.urls.map(_.url.getOrElse("")).mkString(",")
      )
      
      // Extracting user
      val author = if (tweet.user.isDefined) {
        tweet.user.get.username.getOrElse(tweet.userId.getOrElse("unknown"))
      } else {
        tweet.userId.getOrElse("unknown")
      }
      
      Some(SocialMediaMessage(
        id = tweet.id.get,
        providerType = ProviderType.Twitter,
        content = tweet.text.get,
        authorId = author,
        createdAt = new DateTime(tweet.publishedTimeMs.getOrElse(System.currentTimeMillis())),
        metadata = metadata
      ))
    } catch {
      case e: Exception =>
        logger.error(s"Error converting Twitter message: ${e.getMessage}", e)
        None
    }
  }
  
  /**
   * Convertit un message Bluesky en format standardisé
   */
  private def convertBlueskyMessage(rawMessage: String): Option[SocialMediaMessage] = {
    try {
      val json = Json.parse(rawMessage)
      
      val id = (json \ "uri").asOpt[String].getOrElse(
        (json \ "cid").asOpt[String].getOrElse("")
      )
      
      if (id.isEmpty) {
        return None
      }
      
      val content = (json \ "record" \ "text").asOpt[String].getOrElse("")
      val authorId = (json \ "author").asOpt[String].getOrElse("")
      val createdAtStr = (json \ "indexedAt").asOpt[String].getOrElse(DateTime.now().toString())
      
      // Extracting metadata
      val metadata = Map[String, Any](
        "replyCount" -> (json \ "replyCount").asOpt[Int].getOrElse(0),
        "repostCount" -> (json \ "repostCount").asOpt[Int].getOrElse(0),
        "likeCount" -> (json \ "likeCount").asOpt[Int].getOrElse(0),
        "isRepost" -> (json \ "viewer" \ "reposted").asOpt[Boolean].getOrElse(false)
      )
      
      Some(SocialMediaMessage(
        id = id,
        providerType = ProviderType.Bluesky,
        content = content,
        authorId = authorId,
        createdAt = new DateTime(createdAtStr),
        metadata = metadata
      ))
    } catch {
      case e: Exception =>
        logger.error(s"Error converting Bluesky message: ${e.getMessage}", e)
        None
    }
  }
} 