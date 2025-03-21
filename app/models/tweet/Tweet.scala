package models.tweet

import net.liftweb.json._
import java.time.OffsetDateTime
import utils.GetJson

import com.twitter.clientlib.JSON
import com.twitter.clientlib.model.{Expansions, StreamingTweetResponse, TweetReferencedTweets, Tweet => TTweet}

/**
 * Representation of a tweet from the JSON obtained from Twitter.
 *
 * @param basicTweet the tweet given by Twitter
 */
class Tweet(val basicTweet: TTweet) {
	
	private var extendedTweet: Option[Expansions] = None
	
	def this(rawData: String) = {
		this({
			new JSON()
			StreamingTweetResponse.fromJson(rawData).getData
		})
		val tweetResponse = StreamingTweetResponse.fromJson(rawData)
		extendedTweet = Option(tweetResponse.getIncludes)
	}
	
	import scala.jdk.CollectionConverters._
	
	
	// Users concerned by the tweet
	private lazy val users: Option[Map[String, User]] = {
		if (extendedTweet.isDefined) {
			Some(extendedTweet.get.getUsers.asScala.map(user => {
				user.getId -> new User(user)
			}).toMap)
		} else {
			None
		}
	}
	
	// General fields
	lazy val id: Option[String] = Option(basicTweet.getId)
	lazy val text: Option[String] = Option(basicTweet.getText)
	lazy val source: Option[String] = Option(basicTweet.getSource)
	lazy val possiblySensitive: Option[Boolean] = Option(basicTweet.getPossiblySensitive)
	lazy val lang: Option[String] = Option(basicTweet.getLang)
	lazy val createdAt: Option[String] = Option(basicTweet.getCreatedAt.toString)
	private lazy val createdAtOffset: Option[OffsetDateTime] = Option(basicTweet.getCreatedAt)
	lazy val publishedTimeMs: Option[Long] = if (createdAtOffset.isDefined) Some(createdAtOffset.get.toEpochSecond * 1000) else None
	lazy val userId: Option[String] = Option(basicTweet.getAuthorId)
	lazy val user: Option[User] = {
		if (users.isDefined) {
			users.get.get(basicTweet.getAuthorId)
		} else {
			None
		}
	}
	lazy val (longitude: Option[String], latitude: Option[String]) = {
		try {
			val coord = for (l <- basicTweet.getGeo.getCoordinates.getCoordinates.asScala) yield Option(l.toString)
			(coord.head, coord(1))
		} catch {
			case _: Throwable => (None, None)
		}
	}
	lazy val places: Array[Place] = {
		if (extendedTweet.isDefined && extendedTweet.get.getPlaces != null) {
			extendedTweet.get.getPlaces.asScala.map(new Place(_)).toArray
		} else {
			Array[Place]()
		}
	}
	
	// Entities
	lazy val hashtags: Array[Hashtag] = {
		if (basicTweet.getEntities != null && basicTweet.getEntities.getHashtags != null) {
			basicTweet.getEntities.getHashtags.asScala.map(hashtag =>
				Hashtag(Option(hashtag.getStart), Option(hashtag.getEnd), Option(hashtag.getTag))
			).toArray
		} else {
			Array[Hashtag]()
		}
	}
	lazy val cashtags: Array[Cashtag] = {
		if (basicTweet.getEntities != null && basicTweet.getEntities.getCashtags != null) {
			basicTweet.getEntities.getCashtags.asScala.map(cashtag =>
				Cashtag(Option(cashtag.getStart), Option(cashtag.getEnd), Option(cashtag.getTag))
			).toArray
		} else {
			Array[Cashtag]()
		}
	}
	lazy val urls: Array[Url] = {
		if (basicTweet.getEntities != null && basicTweet.getEntities.getUrls != null) {
			basicTweet.getEntities.getUrls.asScala.map(url =>
				Url(Option(url.getStart), Option(url.getEnd), Option(url.getUrl.getPath),
					Option(url.getExpandedUrl.getPath), Option(url.getDisplayUrl),
					Option(url.getStatus), Option(url.getTitle), Option(url.getDescription))
			).toArray
		} else {
			Array[Url]()
		}
	}
	lazy val userMentions: Array[UserMention] = {
		if (basicTweet.getEntities != null && basicTweet.getEntities.getMentions != null) {
			basicTweet.getEntities.getMentions.asScala.map(u =>
				UserMention(Option(u.getStart), Option(u.getEnd), Option(u.getId), Option(u.getUsername))
			).toArray
		} else {
			Array[UserMention]()
		}
	}
	lazy val annotations: Array[Annotation] = {
		if (basicTweet.getEntities != null && basicTweet.getEntities.getAnnotations != null) {
			basicTweet.getEntities.getAnnotations.asScala.map(annotation =>
				Annotation(Option(annotation.getStart), Option(annotation.getEnd),
					Option(annotation.getProbability), Option(annotation.getType), Option(annotation.getNormalizedText))
			).toArray
		} else {
			Array[Annotation]()
		}
	}
	lazy val medias: Array[Media] = {
		if (extendedTweet.isDefined && extendedTweet.get.getMedia != null) {
			try {
				extendedTweet.get.getMedia.asScala.map(media => {
					val mediaJson: JValue = parse(media.toJson)
					val key: Option[String] = GetJson.optionString(mediaJson \ "media_key")
					val mediaType: Option[String] = GetJson.optionString(mediaJson \ "type")
					val url: Option[String] = GetJson.optionString(mediaJson \ "url")
					val durationMs: Option[Int] = GetJson.optionInt(mediaJson \ "duration_ms")
					val height: Option[Int] = GetJson.optionInt(mediaJson \ "height")
					val width: Option[Int] = GetJson.optionInt(mediaJson \ "width")
					val previewUrl: Option[String] = GetJson.optionString(mediaJson \ "preview_image_url")
					val viewCount: Option[Int] = GetJson.optionInt(mediaJson \ "public_metrics" \ "view_count")
					val altText: Option[String] = GetJson.optionString(mediaJson \ "alt_text")
					
					Media(key, mediaType, url, durationMs, height, width, previewUrl, viewCount, altText)
				}).toArray
			} catch {
				case _: Throwable => Array[Media]()
			}
		} else {
			Array[Media]()
		}
		
	}
	lazy val polls: Array[Poll] = {
		if (extendedTweet.isDefined && extendedTweet.get.getPolls != null) {
			extendedTweet.get.getPolls.asScala.map(p => {
				Poll(
					Option(p.getId),
					p.getOptions.asScala.map(o => OptionPoll(Option(o.getPosition), Option(o.getLabel), Option(o.getVotes))).toList,
					Option(p.getDurationMinutes),
					if (p.getEndDatetime != null) Some(p.getEndDatetime.toString) else None,
					Option(p.getVotingStatus.getValue)
				)
			}).toArray
		} else {
			Array[Poll]()
		}
	}
	
	// Public metrics
	lazy val quoteCount: Option[Int] = Option(basicTweet.getPublicMetrics.getQuoteCount)
	lazy val replyCount: Option[Int] = Option(basicTweet.getPublicMetrics.getReplyCount)
	lazy val retweetCount: Option[Int] = Option(basicTweet.getPublicMetrics.getRetweetCount)
	lazy val likeCount: Option[Int] = Option(basicTweet.getPublicMetrics.getLikeCount)
	
	// If the tweet is a reply
	lazy val inReplyToTweet: Option[Tweet] = {
		try {
			if (extendedTweet.isDefined && basicTweet.getReferencedTweets != null) {
				val replies = basicTweet.getReferencedTweets.asScala.filter(t => t.getType == TweetReferencedTweets.TypeEnum.REPLIED_TO)
				if (replies.isEmpty) {
					None
				} else {
					Some(new Tweet(extendedTweet.get.getTweets.asScala.filter(_.getId == replies.head.getId).head))
				}
			} else {
				None
			}
		} catch {
			case _: Throwable => None
		}
	}
	lazy val inReplyToStatusId: Option[String] = {
		if (extendedTweet.isDefined && basicTweet.getReferencedTweets != null) {
			val replies = basicTweet.getReferencedTweets.asScala.filter(t => t.getType == TweetReferencedTweets.TypeEnum.REPLIED_TO)
			if (replies.isEmpty) {
				None
			} else {
				Some(replies.head.getId)
			}
		} else {
			None
		}
	}
	lazy val inReplyToUserId: Option[String] = Option(basicTweet.getInReplyToUserId)
	lazy val inReplyToUserScreenName: Option[String] = {
		if (inReplyToTweet.isDefined) {
			val u = inReplyToTweet.get.user
			if (u.isDefined) {
				u.get.id
			} else {
				None
			}
		} else {
			None
		}
	}
	
	// If the tweet is a retweet
	lazy val retweet: Option[Tweet] = {
		try {
			if (extendedTweet.isDefined && basicTweet.getReferencedTweets != null) {
				val retweets = basicTweet.getReferencedTweets.asScala.filter(t => t.getType == TweetReferencedTweets.TypeEnum.RETWEETED)
				if (retweets.isEmpty) {
					None
				} else {
					Some(new Tweet(extendedTweet.get.getTweets.asScala.filter(_.getId == retweets.head.getId).head))
				}
			} else {
				None
			}
		} catch {
			case _: Throwable => None
		}
	}
	lazy val isRetweet: Boolean = retweetedTweetId.isDefined
	lazy val retweetedTweetId: Option[String] = {
		if (extendedTweet.isDefined && basicTweet.getReferencedTweets != null) {
			val retweets = basicTweet.getReferencedTweets.asScala.filter(t => t.getType == TweetReferencedTweets.TypeEnum.RETWEETED)
			if (retweets.isEmpty) {
				None
			} else {
				Some(retweets.head.getId)
			}
		} else {
			None
		}
	}
	
	// If the tweet is a quote
	lazy val quote: Option[Tweet] = {
		try {
			if (extendedTweet.isDefined && basicTweet.getReferencedTweets != null) {
				val quotes = basicTweet.getReferencedTweets.asScala.filter(t => t.getType == TweetReferencedTweets.TypeEnum.QUOTED)
				if (quotes.isEmpty) {
					None
				} else {
					Some(new Tweet(extendedTweet.get.getTweets.asScala.filter(_.getId == quotes.head.getId).head))
				}
			} else {
				None
			}
		} catch {
			case _: Throwable => None
		}
	}
	lazy val isQuote: Boolean = quotedTweetId.isDefined
	lazy val quotedTweetId: Option[String] = {
		if (extendedTweet.isDefined && basicTweet.getReferencedTweets != null) {
			val quotes = basicTweet.getReferencedTweets.asScala.filter(t => t.getType == TweetReferencedTweets.TypeEnum.QUOTED)
			if (quotes.isEmpty) {
				None
			} else {
				Some(quotes.head.getId)
			}
		} else {
			None
		}
	}
}
