package models

import providers.ProviderType
import org.joda.time.DateTime
import play.api.libs.json._

case class SocialMediaMessage(
  id: String,
  providerType: ProviderType,
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
  implicit val dateTimeFormat: Format[DateTime] = Format(
    JodaReads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss.SSSZ"),
    JodaWrites.jodaDateWrites("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  )
  
  // Format personnalisé pour ProviderType
  implicit val providerTypeFormat: Format[ProviderType.Value] = new Format[ProviderType.Value] {
    def reads(json: JsValue): JsResult[ProviderType.Value] = 
      json.validate[String].map(s => ProviderType.withName(s))
    
    def writes(providerType: ProviderType.Value): JsValue = 
      JsString(providerType.toString)
  }
  
  // Format pour Map[String, String]
  implicit val mapFormat: Format[Map[String, String]] = Format(
    Reads.mapReads[String],
    Writes.mapWrites[String]
  )
  
  implicit val messageFormat: Format[SocialMediaMessage] = Json.format[SocialMediaMessage]
  
  def fromJson(json: String): SocialMediaMessage = {
    Json.parse(json).as[SocialMediaMessage]
  }
} 