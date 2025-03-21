package models

import providers.ProviderType
import org.joda.time.DateTime
import play.api.libs.json._
// Import direct de Format sans utiliser JodaReads/JodaWrites
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsArray, JsBoolean, JsNull, JsNumber, JsObject, JsString, JsValue}

/**
 * Représente un message normalisé depuis les différentes plateformes de médias sociaux
 */
case class SocialMediaMessage(
  id: String,
  providerType: ProviderType.ProviderType,
  content: String,
  authorId: String,
  createdAt: DateTime,
  metadata: Map[String, String] = Map.empty
) {
  def toJson: String = {
    Json.toJson(this).toString()
  }
}

object SocialMediaMessage {
  // Lecture de DateTime pour Joda Time
  implicit val jodaDateReads: Reads[DateTime] = new Reads[DateTime] {
    def reads(json: JsValue): JsResult[DateTime] = json match {
      case JsString(s) => 
        try {
          JsSuccess(new DateTime(s))
        } catch {
          case e: Exception => 
            try {
              // Essayer en format millisecondes
              JsSuccess(new DateTime(s.toLong))
            } catch {
              case _: Exception => JsError(s"Invalid DateTime format: $s")
            }
        }
      case JsNumber(n) => JsSuccess(new DateTime(n.toLong))
      case _ => JsError("DateTime value expected")
    }
  }

  // Ecriture de DateTime pour Joda Time
  implicit val jodaDateWrites: Writes[DateTime] = new Writes[DateTime] {
    def writes(d: DateTime): JsValue = JsString(d.toString)
  }

  // Format pour ProviderType
  implicit val providerTypeReads: Reads[ProviderType.ProviderType] = new Reads[ProviderType.ProviderType] {
    def reads(json: JsValue): JsResult[ProviderType.ProviderType] = json match {
      case JsString(s) => 
        try {
          val providerType = s.toLowerCase match {
            case "twitter" => ProviderType.Twitter
            case "bluesky" => ProviderType.Bluesky
            case _ => throw new IllegalArgumentException(s"Unknown provider: $s")
          }
          JsSuccess(providerType)
        } catch {
          case e: Exception => JsError(s"Error parsing provider type: ${e.getMessage}")
        }
      case _ => JsError("String value expected for provider type")
    }
  }

  implicit val providerTypeWrites: Writes[ProviderType.ProviderType] = new Writes[ProviderType.ProviderType] {
    def writes(providerType: ProviderType.ProviderType): JsValue = JsString(providerType.toString)
  }
  
  // Format correct pour Map[String, String]
  implicit val mapStringFormat: Format[Map[String, String]] = Format(
    Reads.mapReads[String],
    new Writes[Map[String, String]] {
      def writes(map: Map[String, String]): JsValue = {
        val fields = map.map { case (key, value) => (key, JsString(value)) }
        JsObject(fields)
      }
    }
  )

  // Format pour SocialMediaMessage
  implicit val socialMediaMessageFormat: Format[SocialMediaMessage] = (
    (__ \ "id").format[String] and
    (__ \ "providerType").format[ProviderType.ProviderType] and
    (__ \ "content").format[String] and
    (__ \ "authorId").format[String] and
    (__ \ "createdAt").format[DateTime](Format(jodaDateReads, jodaDateWrites)) and
    (__ \ "metadata").formatWithDefault[Map[String, String]](Map.empty)
  )(SocialMediaMessage.apply, unlift(SocialMediaMessage.unapply))
  
  /**
   * Crée un SocialMediaMessage à partir d'une chaîne JSON
   */
  def fromJson(json: String): SocialMediaMessage = {
    try {
      // Essayer d'abord de parser comme un SocialMediaMessage directement
      Json.parse(json).validate[SocialMediaMessage] match {
        case JsSuccess(message, _) => message
        case JsError(_) => 
          // Si ça ne fonctionne pas, essayer de déterminer le format et adapter
          val jsValue = Json.parse(json)
          
          // Déterminer le providerType en fonction du JSON
          val providerType = if ((jsValue \ "text").isDefined || (jsValue \ "tweet").isDefined) {
            ProviderType.Twitter
          } else if ((jsValue \ "uri").isDefined || (jsValue \ "record" \ "text").isDefined) {
            ProviderType.Bluesky
          } else {
            throw new IllegalArgumentException("Unknown JSON format")
          }
          
          // Construire un nouveau SocialMediaMessage approprié
          providerType match {
            case ProviderType.Twitter =>
              val id = (jsValue \ "id").asOpt[String].orElse(
                (jsValue \ "id_str").asOpt[String]
              ).getOrElse(throw new IllegalArgumentException("Missing Twitter ID"))
              
              val content = (jsValue \ "text").asOpt[String].orElse(
                (jsValue \ "full_text").asOpt[String]
              ).getOrElse(throw new IllegalArgumentException("Missing Twitter text"))
              
              val authorId = (jsValue \ "user" \ "screen_name").asOpt[String].orElse(
                (jsValue \ "user" \ "id_str").asOpt[String]
              ).getOrElse(throw new IllegalArgumentException("Missing Twitter author"))
              
              val createdAtStr = (jsValue \ "created_at").asOpt[String].getOrElse(
                DateTime.now().toString()
              )
              val createdAt = try {
                new DateTime(createdAtStr)
              } catch {
                case _: Exception => DateTime.now()
              }
              
              // Extraire des métadonnées pertinentes
              val metadata = Map[String, String](
                "source" -> (jsValue \ "source").asOpt[String].getOrElse(""),
                "lang" -> (jsValue \ "lang").asOpt[String].getOrElse(""),
                "replyCount" -> (jsValue \ "reply_count").asOpt[Int].getOrElse(0).toString,
                "retweetCount" -> (jsValue \ "retweet_count").asOpt[Int].getOrElse(0).toString,
                "likeCount" -> (jsValue \ "favorite_count").asOpt[Int].getOrElse(0).toString
              )
              
              SocialMediaMessage(id, providerType, content, authorId, createdAt, metadata)
              
            case ProviderType.Bluesky =>
              val id = (jsValue \ "uri").asOpt[String].getOrElse(
                (jsValue \ "cid").asOpt[String].getOrElse(
                  throw new IllegalArgumentException("Missing Bluesky ID")
                )
              )
              
              val content = (jsValue \ "record" \ "text").asOpt[String].getOrElse(
                throw new IllegalArgumentException("Missing Bluesky text")
              )
              
              val authorId = (jsValue \ "author").asOpt[String].getOrElse(
                throw new IllegalArgumentException("Missing Bluesky author")
              )
              
              val createdAtStr = (jsValue \ "indexedAt").asOpt[String].getOrElse(
                DateTime.now().toString()
              )
              val createdAt = try {
                new DateTime(createdAtStr)
              } catch {
                case _: Exception => DateTime.now()
              }
              
              // Extraire des métadonnées pertinentes
              val metadata = Map[String, String](
                "uri" -> (jsValue \ "uri").asOpt[String].getOrElse(""),
                "cid" -> (jsValue \ "cid").asOpt[String].getOrElse(""),
                "replyCount" -> (jsValue \ "replyCount").asOpt[Int].getOrElse(0).toString,
                "repostCount" -> (jsValue \ "repostCount").asOpt[Int].getOrElse(0).toString,
                "likeCount" -> (jsValue \ "likeCount").asOpt[Int].getOrElse(0).toString
              )
              
              SocialMediaMessage(id, providerType, content, authorId, createdAt, metadata)
          }
      }
    } catch {
      case e: Exception =>
        throw new IllegalArgumentException(s"Failed to parse JSON: ${e.getMessage}", e)
    }
  }
} 