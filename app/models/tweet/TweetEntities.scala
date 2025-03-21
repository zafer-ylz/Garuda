package models.tweet

// Hashtag représente un hashtag dans un tweet
case class Hashtag(
  start: Option[Int], 
  end: Option[Int], 
  text: Option[String]
)

// Cashtag représente un cashtag (ex: $BTC) dans un tweet
case class Cashtag(
  start: Option[Int], 
  end: Option[Int], 
  text: Option[String]
)

// Url représente une URL partagée dans un tweet
case class Url(
  start: Option[Int], 
  end: Option[Int], 
  url: Option[String], 
  expandedUrl: Option[String], 
  displayUrl: Option[String],
  status: Option[String],
  title: Option[String],
  description: Option[String]
)

// UserMention représente la mention d'un utilisateur dans un tweet
case class UserMention(
  start: Option[Int], 
  end: Option[Int], 
  id: Option[String], 
  username: Option[String]
)

// Annotation représente une annotation dans un tweet (entité reconnue par Twitter)
case class Annotation(
  start: Option[Int], 
  end: Option[Int], 
  probability: Option[Double], 
  annotationType: Option[String], 
  normalizedText: Option[String]
)

// Media représente un média (image, vidéo) attaché à un tweet
case class Media(
  key: Option[String],
  mediaType: Option[String],
  url: Option[String],
  durationMs: Option[Int],
  height: Option[Int],
  width: Option[Int],
  previewUrl: Option[String],
  viewCount: Option[Int],
  altText: Option[String]
)

// Poll représente un sondage dans un tweet
case class Poll(
  id: Option[String],
  options: List[OptionPoll],
  durationMinutes: Option[Int],
  endDateTime: Option[String],
  votingStatus: Option[String]
)

// OptionPoll représente une option de sondage
case class OptionPoll(
  position: Option[Int],
  label: Option[String],
  votes: Option[Int]
)

// Place représente un lieu géographique mentionné dans un tweet
case class Place(
  place: com.twitter.clientlib.model.Place
) {
  import scala.jdk.CollectionConverters._

  lazy val id: Option[String] = Option(place.getId)
  lazy val name: Option[String] = Option(place.getFullName)
  lazy val country: Option[String] = Option(place.getCountry)
  lazy val countryCode: Option[String] = Option(place.getCountryCode)
  lazy val placeType: Option[String] = Option(place.getPlaceType)
  lazy val fullName: Option[String] = Option(place.getFullName)
  lazy val typeBoundingBox: Option[String] = if (place.getGeo != null && place.getGeo.getType != null) {
    Option(place.getGeo.getType.getValue)
  } else {
    None
  }
  lazy val boundingBox: Array[Place.Coordinates] = if (place.getGeo != null && place.getGeo.getBbox != null) {
    place.getGeo.getBbox.asScala.grouped(2).map(point => 
      Place.Coordinates(point(0).toString, point(1).toString)
    ).toArray
  } else {
    Array.empty[Place.Coordinates]
  }
}

// Objet companion pour Place
object Place {
  // Coordonnées géographiques (longitude, latitude)
  case class Coordinates(longitude: String, latitude: String)
} 