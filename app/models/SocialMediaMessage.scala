package models

import providers.ProviderType
import java.time.DateTime
import play.api.libs.json._

case class SocialMediaMessage(
  id: String,
  providerType: ProviderType,
  content: String,
  authorId: String,
  createdAt: DateTime,
  metadata: Map[String, Any] = Map.empty
) {
  def toJson: String = {
    Json.toJson(this).toString()
  }
}

object SocialMediaMessage {
  implicit val dateTimeFormat: Format[DateTime] = Format(
    Reads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss.SSSZ"),
    Writes.jodaDateWrites
  )
  
  implicit val providerTypeFormat: Format[ProviderType] = Format(
    Reads.enumNameReads(ProviderType),
    Writes.enumNameWrites
  )
  
  implicit val messageFormat: Format[SocialMediaMessage] = Json.format[SocialMediaMessage]
  
  def fromJson(json: String): SocialMediaMessage = {
    Json.parse(json).as[SocialMediaMessage]
  }
} 