package models.tweet

import com.twitter.clientlib.model.{User => TUser}
import org.joda.time.DateTime
import java.time.ZoneId

/**
 * Représentation d'un utilisateur Twitter à partir du modèle fourni par l'API
 */
class User(private val user: TUser) {
	import scala.jdk.CollectionConverters._
	
	// Identifiants
	lazy val id: Option[String] = Option(user.getId)
	lazy val name: Option[String] = Option(user.getName)
	lazy val screen_name: Option[String] = Option(user.getUsername)
	
	// Informations de profil
	lazy val location: Option[String] = Option(user.getLocation)
	lazy val description: Option[String] = Option(user.getDescription)
	lazy val url: Option[String] = Option(user.getUrl)
	lazy val protected_account: Option[Boolean] = Option(user.getProtected)
	lazy val verified: Option[Boolean] = Option(user.getVerified)
	lazy val profile_image_url: Option[String] = Option(user.getProfileImageUrl)
	lazy val profile_banner_url: Option[String] = Option(user.getProfileBannerUrl)
	
	// Métriques
	lazy val followers_count: Option[Int] = if (user.getPublicMetrics != null) Option(user.getPublicMetrics.getFollowersCount) else None
	lazy val following_count: Option[Int] = if (user.getPublicMetrics != null) Option(user.getPublicMetrics.getFollowingCount) else None
	lazy val tweet_count: Option[Int] = if (user.getPublicMetrics != null) Option(user.getPublicMetrics.getTweetCount) else None
	lazy val listed_count: Option[Int] = if (user.getPublicMetrics != null) Option(user.getPublicMetrics.getListedCount) else None
	
	// Dates
	lazy val created_at_string: Option[String] = Option(user.getCreatedAt).map(_.toString)
	lazy val created_at: Option[DateTime] = Option(user.getCreatedAt).map(date => {
		val instant = date.atZone(ZoneId.systemDefault()).toInstant()
		new DateTime(instant.toEpochMilli())
	})
	
	// Entités
	lazy val entities: Map[String, List[Object]] = {
		if (user.getEntities != null) {
			val result = scala.collection.mutable.Map[String, List[Object]]()
			
			// URLs
			if (user.getEntities.getUrl != null && user.getEntities.getUrl.getUrls != null) {
				result += "urls" -> user.getEntities.getUrl.getUrls.asScala.toList
			}
			
			// Description URLs
			if (user.getEntities.getDescription != null && user.getEntities.getDescription.getUrls != null) {
				result += "description_urls" -> user.getEntities.getDescription.getUrls.asScala.toList
			}
			
			result.toMap
		} else {
			Map.empty
		}
	}
}
