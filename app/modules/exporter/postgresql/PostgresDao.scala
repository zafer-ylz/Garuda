package modules.exporter.postgresql

import java.sql._
import java.util.Properties

import models.SocialMediaMessage
import models.tweet.{Annotation, Cashtag, Hashtag, Media, Place, Tweet, Url, User, UserMention}
import play.api.Logging
import providers.ProviderType

class PostgresDao(val config: PostgresConfiguration) extends Logging {
	
	case class MultiInsertions(stmt: Statement)
	
	Class.forName("org.postgresql.Driver")
	private val url: String = s"jdbc:postgresql://${config.host}:${config.port}/${config.base}"
	private val props: Properties = new Properties()
	props.setProperty("user", config.user)
	props.setProperty("password", config.password)
	private val conn: Connection = DriverManager.getConnection(url, props)
	private val schema: String = config.schema
	
	private val TWEET_TABLE: String = "tweet"
	private val USER_TABLE: String = "user"
	private val WITHHELD_IN_COUNTRY_TABLE: String = "withheld_in_country"
	private val PLACE_TABLE: String = "place"
	private val BLUESKY_POST_TABLE: String = "bluesky_post"
	private val MESSAGE_TABLE: String = "social_message"
	
	private val REPLY_TABLE: String = "reply"
	private val QUOTE_TABLE: String = "quote"
	private val RETWEET_TABLE: String = "retweet"
	
	private val TWEET_HASHTAG_TABLE: String = "tweet_hashtag"
	private val TWEET_URL_TABLE: String = "tweet_url"
	private val TWEET_CASHTAG_TABLE: String = "tweet_cashtag"
	private val TWEET_EMOJI_TABLE: String = "tweet_emoji"
	private val TWEET_MEDIA_TABLE: String = "tweet_media"
	private val TWEET_USER_MENTION_TABLE: String = "tweet_user_mention"
	private val TWEET_PLACE_TABLE: String = "tweet_place"
	private val TWEET_ANNOTATION_TABLE: String = "tweet_annotation"
	
	private val TWEET_TAG_TABLE: String = "tweet_tag"
	
	/**
	 * Create the schema and the tables if it is not already done.
	 */
	def setupCollect(): Unit = {
		val st: Statement = conn.createStatement()
		st.execute(s"CREATE SCHEMA IF NOT EXISTS $schema")
		
		val tweetTable: String = s"""CREATE TABLE IF NOT EXISTS $schema.$TWEET_TABLE(
				id TEXT PRIMARY KEY,
				created_at TEXT,
				published_time BIGINT,
				user_id TEXT,
				user_name TEXT,
				user_screen_name TEXT,
				text TEXT,
				source TEXT,
				language TEXT,
				coordinates_longitude TEXT,
				coordinates_latitude TEXT,
				possibly_sensitive BOOLEAN
			)"""
		st.execute(tweetTable)
		
		val userTable: String = s"""CREATE TABLE IF NOT EXISTS $schema.$USER_TABLE(
				id TEXT PRIMARY KEY,
				screen_name TEXT,
				name TEXT,
				created_at TIMESTAMP,
				verified BOOLEAN,
				protected BOOLEAN
			)"""
		st.execute(userTable)
		
		val withheldInCountryTable: String = s"""CREATE TABLE IF NOT EXISTS $schema.$WITHHELD_IN_COUNTRY_TABLE(
				user_id TEXT,
				country TEXT,
				PRIMARY KEY(user_id, country)
			)"""
		st.execute(withheldInCountryTable)
		
		val placeTable: String = s"""CREATE TABLE IF NOT EXISTS $schema.$PLACE_TABLE(
				id TEXT PRIMARY KEY,
				name TEXT,
				full_name TEXT,
				country_code TEXT,
				country TEXT,
				place_type TEXT,
				bounding_box TEXT,
				type_bounding_box TEXT
			)"""
		st.execute(placeTable)
		
		val replyTable: String = s"""CREATE TABLE IF NOT EXISTS $schema.$REPLY_TABLE(
				tweet_id TEXT,
				in_reply_to_tweet_id TEXT,
				in_reply_to_user_id TEXT,
				in_reply_to_screen_name TEXT,
				PRIMARY KEY(tweet_id, in_reply_to_tweet_id)
			)"""
		st.execute(replyTable)
		
		val quoteTable: String = s"""CREATE TABLE IF NOT EXISTS $schema.$QUOTE_TABLE(
				tweet_id TEXT,
				quoted_tweet_id TEXT,
				PRIMARY KEY(tweet_id, quoted_tweet_id)
			)"""
		st.execute(quoteTable)
		
		val retweetTable: String = s"""CREATE TABLE IF NOT EXISTS $schema.$RETWEET_TABLE(
				tweet_id TEXT,
				retweeted_tweet_id TEXT,
				PRIMARY KEY(tweet_id, retweeted_tweet_id)
			)"""
		st.execute(retweetTable)
		
		val tweetHashtagTable: String = s"""CREATE TABLE IF NOT EXISTS $schema.$TWEET_HASHTAG_TABLE(
				tweet_id TEXT,
				rank INTEGER,
				hashtag TEXT,
				start_indice INTEGER,
				end_indice INTEGER,
				PRIMARY KEY(tweet_id, rank)
			)"""
		st.execute(tweetHashtagTable)
		
		val tweetUrlTable: String = s"""CREATE TABLE IF NOT EXISTS $schema.$TWEET_URL_TABLE(
				tweet_id TEXT,
				rank INTEGER,
				url TEXT,
				expanded_url TEXT,
				display_url TEXT,
				status INTEGER,
				title TEXT,
				description TEXT,
				start_indice INTEGER,
				end_indice INTEGER,
				PRIMARY KEY(tweet_id, rank)
			)"""
		st.execute(tweetUrlTable)
		
		val tweetCashtagTable: String = s"""CREATE TABLE IF NOT EXISTS $schema.$TWEET_CASHTAG_TABLE(
				tweet_id TEXT,
				rank INTEGER,
				cashtag TEXT,
				start_indice INTEGER,
				end_indice INTEGER,
				PRIMARY KEY(tweet_id, rank)
			)"""
		st.execute(tweetCashtagTable)
		
		val tweetEmojiTable: String = s"""CREATE TABLE IF NOT EXISTS $schema.$TWEET_EMOJI_TABLE(
				tweet_id TEXT,
				rank INTEGER,
				emoji TEXT,
				start_indice INTEGER,
				end_indice INTEGER,
				PRIMARY KEY(tweet_id, rank)
			)"""
		st.execute(tweetEmojiTable)
		
		val tweetMediaTable: String = s"""CREATE TABLE IF NOT EXISTS $schema.$TWEET_MEDIA_TABLE(
				tweet_id TEXT,
				rank INTEGER,
				key TEXT,
				type TEXT,
				media_url TEXT,
				duration_ms INTEGER,
				height INTEGER,
				width INTEGER,
				preview_image_url TEXT,
				view_count INTEGER,
				alternative_text TEXT,
				PRIMARY KEY(tweet_id, rank)
			)"""
		st.execute(tweetMediaTable)
		
		val tweetUserMentionTable: String = s"""CREATE TABLE IF NOT EXISTS $schema.$TWEET_USER_MENTION_TABLE(
				tweet_id TEXT,
				rank INTEGER,
				user_id TEXT,
				start_indice INTEGER,
				end_indice INTEGER,
				PRIMARY KEY(tweet_id, rank)
			)"""
		st.execute(tweetUserMentionTable)
		
		val tweetPlaceTable: String = s"""CREATE TABLE IF NOT EXISTS $schema.$TWEET_PLACE_TABLE(
				tweet_id TEXT,
				place_id TEXT,
				PRIMARY KEY(tweet_id, place_id)
			)"""
		st.execute(tweetPlaceTable)
		
		val tweetAnnotationTable: String = s"""CREATE TABLE IF NOT EXISTS $schema.$TWEET_ANNOTATION_TABLE(
				tweet_id TEXT,
				rank INTEGER,
				annotation_type TEXT,
				normalized_text TEXT,
				start_indice INTEGER,
				end_indice INTEGER,
				PRIMARY KEY(tweet_id, rank)
			)"""
		st.execute(tweetAnnotationTable)
		
		val tweetTagTable: String = s"""CREATE TABLE IF NOT EXISTS $schema.$TWEET_TAG_TABLE(
				tweet_id TEXT,
				tag TEXT,
				PRIMARY KEY(tweet_id, tag)
			)"""
		st.execute(tweetTagTable)
		
		// Tables pour Bluesky
		val blueskyPostTable: String = s"""CREATE TABLE IF NOT EXISTS $schema.$BLUESKY_POST_TABLE(
				id TEXT PRIMARY KEY,
				uri TEXT,
				cid TEXT,
				author TEXT,
				text TEXT,
				reply_count INTEGER,
				repost_count INTEGER,
				like_count INTEGER,
				created_at TIMESTAMP,
				indexed_at TIMESTAMP,
				FOREIGN KEY (id) REFERENCES $schema.$MESSAGE_TABLE(id)
			)"""
		st.execute(blueskyPostTable)
		
		val socialMessageTable: String = s"""CREATE TABLE IF NOT EXISTS $schema.$MESSAGE_TABLE(
				id TEXT PRIMARY KEY,
				provider_type TEXT NOT NULL,
				content TEXT,
				author_id TEXT,
				created_at TIMESTAMP,
				metadata JSONB
			)"""
		st.execute(socialMessageTable)
		
		st.close()
	}
	
	def addBatchTweet(multiInsertions: MultiInsertions, tweet: Tweet): Unit = {
		assert(tweet.id.isDefined)
		assert(tweet.userId.isDefined)
		multiInsertions.stmt.addBatch(
			s"""INSERT INTO $schema.$TWEET_TABLE(
				id,
				created_at,
				published_time,
				user_id,
				user_name,
				user_screen_name,
				text,
				source,
				language,
				coordinates_longitude,
				coordinates_latitude,
				possibly_sensitive)
    	VALUES(
			'${tweet.id.get}',
			${getStringOrNull(tweet.createdAt)},
			${getValueOrNull(tweet.publishedTimeMs)},
			${getStringOrNull(tweet.userId)},
			${if (tweet.user.isDefined) getStringOrNull(tweet.user.get.name) else "NULL"},
			${if (tweet.user.isDefined) getStringOrNull(tweet.user.get.screenName) else "NULL"},
			${getStringOrNull(tweet.text)},
			${getStringOrNull(tweet.source)},
			${getStringOrNull(tweet.lang)},
			${getStringOrNull(tweet.longitude)},
			${getStringOrNull(tweet.latitude)},
			${getValueOrNull(tweet.possiblySensitive)})
        ON CONFLICT (id) DO NOTHING""")
	}
	
	def addBatchUser(multiInsertions: MultiInsertions, user: User): Unit = {
		assert(user.id.isDefined)
		multiInsertions.stmt.addBatch(s"""INSERT INTO $schema.$USER_TABLE(
			id,
			screen_name,
			name,
			created_at,
			verified,
			protected)
    	VALUES(
			'${user.id.get}',
			${getStringOrNull(user.screenName)},
			${getStringOrNull(user.name)},
			${getStringOrNull(user.createdAt)},
			${getValueOrNull(user.verified)},
			${getValueOrNull(user.`protected`)})
        ON CONFLICT (id) DO NOTHING""")
	}
	
	def addBatchUserWithheldInCountry(multiInsertions: MultiInsertions, userId: String, countries: scala.Array[String]): Unit = {
		for (country <- countries) {
			multiInsertions.stmt.addBatch(s"""INSERT INTO $schema.$WITHHELD_IN_COUNTRY_TABLE(
		    	user_id,
		    	country)
	    	VALUES(
		    	'$userId',
		    	'${country.replace("'", "''").replace("\u0000", "")}')
            ON CONFLICT (user_id, country) DO NOTHING""")
		}
	}
	
	def addBatchPlace(multiInsertions: MultiInsertions, place: Place): Unit = {
		assert(place.id.isDefined)
		multiInsertions.stmt.addBatch(s"""INSERT INTO $schema.$PLACE_TABLE(
			id,
			name,
			full_name,
			country_code,
			country,
			place_type,
			bounding_box,
			type_bounding_box)
    	VALUES(
			'${place.id.get}',
			${getStringOrNull(place.name)},
			${getStringOrNull(place.fullName)},
			${getStringOrNull(place.countryCode)},
			${getStringOrNull(place.country)},
			${getStringOrNull(place.placeType)},
			${if (place.boundingBox.isEmpty) {
				"NULL"
			} else {
				s"'${place.boundingBox.mkString("\n")}'"
			}},
			${getStringOrNull(place.boundingBoxType)})
        ON CONFLICT (id) DO NOTHING""")
	}
	
	def addBatchReply(multiInsertions: MultiInsertions, tweetId: String, inReplyToTweetId: String, inReplyToUserId: Option[String], inReplyToUserScreenName: Option[String]): Unit = {
		multiInsertions.stmt.addBatch(s"""INSERT INTO $schema.$REPLY_TABLE(
			tweet_id,
			in_reply_to_tweet_id,
			in_reply_to_user_id,
			in_reply_to_screen_name)
		VALUES(
			'$tweetId',
			'$inReplyToTweetId',
			${getStringOrNull(inReplyToUserId)},
			${getStringOrNull(inReplyToUserScreenName)})
		ON CONFLICT (tweet_id, in_reply_to_tweet_id) DO NOTHING""")
	}
	
	def addBatchQuote(multiInsertions: MultiInsertions, tweetId: String, quotedTweetId: String): Unit = {
		multiInsertions.stmt.addBatch(s"""INSERT INTO $schema.$QUOTE_TABLE(
			tweet_id,
			quoted_tweet_id)
		VALUES(
			'$tweetId',
			'$quotedTweetId')
		ON CONFLICT (tweet_id, quoted_tweet_id) DO NOTHING""")
	}
	
	def addBatchRetweet(multiInsertions: MultiInsertions, tweetId: String, retweetedTweetId: String): Unit = {
		multiInsertions.stmt.addBatch(s"""INSERT INTO $schema.$RETWEET_TABLE(
			tweet_id,
			retweeted_tweet_id)
		VALUES(
			'$tweetId',
			'$retweetedTweetId')
		ON CONFLICT (tweet_id, retweeted_tweet_id) DO NOTHING""")
	}
	
	def addBatchTweetHashtags(multiInsertions: MultiInsertions, tweetId: String, hashtags: scala.Array[Hashtag]): Unit = {
		for (i <- hashtags.indices) {
			multiInsertions.stmt.addBatch(s"""INSERT INTO $schema.$TWEET_HASHTAG_TABLE(
				tweet_id,
				rank,
				hashtag,
				start_indice,
				end_indice)
			VALUES(
				'$tweetId',
				${i + 1},
				${getStringOrNull(hashtags(i).content)},
				${getValueOrNull(hashtags(i).start)},
				${getValueOrNull(hashtags(i).`end`)})
			ON CONFLICT (tweet_id, rank) DO NOTHING""")
		}
	}
	
	def addBatchTweetUrls(multiInsertions: MultiInsertions, tweetId: String, urls: scala.Array[Url]): Unit = {
		for (i <- urls.indices) {
			multiInsertions.stmt.addBatch(s"""INSERT INTO $schema.$TWEET_URL_TABLE(
		    	tweet_id,
				rank,
				url,
				expanded_url,
				display_url,
				status,
				title,
				description,
				start_indice,
				end_indice)
	    	VALUES(
				'$tweetId',
				${i + 1},
				${getStringOrNull(urls(i).url)},
				${getStringOrNull(urls(i).expandedUrl)},
				${getStringOrNull(urls(i).displayUrl)},
				${getValueOrNull(urls(i).status)},
				${getStringOrNull(urls(i).title)},
				${getStringOrNull(urls(i).description)},
				${getValueOrNull(urls(i).start)},
				${getValueOrNull(urls(i).`end`)})
            ON CONFLICT (tweet_id, rank) DO NOTHING""")
		}
	}
	
	def addBatchTweetCashtags(multiInsertions: MultiInsertions, tweetId: String, cashtags: scala.Array[Cashtag]): Unit = {
		for (i <- cashtags.indices) {
			multiInsertions.stmt.addBatch(s"""INSERT INTO $schema.$TWEET_CASHTAG_TABLE(
				tweet_id,
				rank,
				cashtag,
				start_indice,
				end_indice)
			VALUES(
				'$tweetId',
				${i + 1},
				${getStringOrNull(cashtags(i).content)},
				${getValueOrNull(cashtags(i).start)},
				${getValueOrNull(cashtags(i).`end`)})
			ON CONFLICT (tweet_id, rank) DO NOTHING""")
		}
	}
	
	def addBatchTweetMedias(multiInsertions: MultiInsertions, tweetId: String, medias: scala.Array[Media]): Unit = {
		for (i <- medias.indices) {
			val media: Media = medias(i)
			multiInsertions.stmt.addBatch(s"""INSERT INTO $schema.$TWEET_MEDIA_TABLE(
				tweet_id,
				rank,
				key,
				type,
				media_url,
				duration_ms,
				height,
				width,
				preview_image_url,
				view_count,
				alternative_text)
			VALUES(
				'$tweetId',
				${i + 1},
				${getStringOrNull(media.key)},
				${getStringOrNull(media.mediaType)},
				${getStringOrNull(media.url)},
				${getValueOrNull(media.durationMs)},
				${getValueOrNull(media.height)},
				${getValueOrNull(media.width)},
 				${getStringOrNull(media.previewImageUrl)},
	 			${getValueOrNull(media.viewCount)},
  				${getStringOrNull(media.altText)})
			ON CONFLICT (tweet_id, rank) DO NOTHING""")
		}
	}
	
	def addBatchTweetUserMentions(multiInsertions: MultiInsertions, tweetId: String, mentions: scala.Array[UserMention]): Unit = {
		for (i <- mentions.indices) {
			if (mentions(i).userId != null) {
				multiInsertions.stmt.addBatch(s"""INSERT INTO $schema.$TWEET_USER_MENTION_TABLE(
					tweet_id,
					rank,
					user_id,
					start_indice,
					end_indice)
				VALUES(
					'$tweetId',
					${i + 1},
					${getStringOrNull(mentions(i).userId)},
					${getValueOrNull(mentions(i).start)},
					${getValueOrNull(mentions(i).`end`)})
				ON CONFLICT (tweet_id, rank) DO NOTHING""")
			}
		}
	}
	
	def addBatchTweetPlace(multiInsertions: MultiInsertions, tweetId: String, placeId: String): Unit = {
		multiInsertions.stmt.addBatch(s"""INSERT INTO $schema.$TWEET_PLACE_TABLE(
	    	  tweet_id,
	    	  place_id)
    	  VALUES(
	    	  '$tweetId',
	    	  '$placeId')
          ON CONFLICT (tweet_id, place_id) DO NOTHING""")
	}
	
	def addBatchAnnotation(multiInsertions: MultiInsertions, tweetId: String, annotations: scala.Array[Annotation]): Unit = {
		for (i <- annotations.indices) {
			multiInsertions.stmt.addBatch(s"""INSERT INTO $schema.$TWEET_ANNOTATION_TABLE(
				tweet_id,
				rank,
				annotation_type,
				normalized_text,
				start_indice,
				end_indice)
			VALUES(
				'$tweetId',
				${i + 1},
				${getStringOrNull(annotations(i).annotationType)},
				${getStringOrNull(annotations(i).normalizedText)},
				${getValueOrNull(annotations(i).start)},
				${getValueOrNull(annotations(i).`end`)})
			ON CONFLICT (tweet_id, rank) DO NOTHING""")
		}
	}
	
	def getMultiInsertions: MultiInsertions = {
		MultiInsertions(conn.createStatement())
	}
	
	def executeBatch(multiInsertions: MultiInsertions): Unit = {
		multiInsertions.stmt.executeBatch()
		multiInsertions.stmt.close()
	}
	
	private def getStringOrNull(value: Option[String]): String = {
		if (value.isDefined) {
			s"'${value.get.replace("'", "''").replace("\u0000", "")}'"
		} else {
			"NULL"
		}
	}
	
	private def getValueOrNull[T](value: Option[T]): String = {
		if (value.isDefined) {
			s"${value.get}"
		} else {
			"NULL"
		}
	}
	
	def close(): Unit = {
		if (!conn.isClosed) {
			logger.info("Shutdown PostgresDao connection.")
			conn.close()
		}
	}
	
	/**
	 * Insère un message Bluesky dans la base de données
	 */
	def insertBlueskyMessage(message: SocialMediaMessage): Unit = {
		val multiInsertions = getMultiInsertions
		try {
			// Insertion dans la table commune des messages
			val metadataJson = message.metadata.map {
				case (k, v: String) => s""""$k":"${escapeSQL(v)}""""
				case (k, v) => s""""$k":$v"""
			}.mkString("{", ",", "}")
			
			multiInsertions.stmt.addBatch(s"""INSERT INTO $schema.$MESSAGE_TABLE 
				(id, provider_type, content, author_id, created_at, metadata) 
				VALUES ('${message.id}', '${message.providerType}', '${escapeSQL(message.content)}', 
					'${message.authorId}', '${message.createdAt}', '$metadataJson'::jsonb)
				ON CONFLICT (id) DO NOTHING""")
			
			// Insertion dans la table spécifique Bluesky
			val replyCount = message.metadata.getOrElse("replyCount", 0).asInstanceOf[Int]
			val repostCount = message.metadata.getOrElse("repostCount", 0).asInstanceOf[Int]
			val likeCount = message.metadata.getOrElse("likeCount", 0).asInstanceOf[Int]
			
			multiInsertions.stmt.addBatch(s"""INSERT INTO $schema.$BLUESKY_POST_TABLE 
				(id, uri, cid, author, text, reply_count, repost_count, like_count, created_at, indexed_at) 
				VALUES ('${message.id}', 
					${getStringOrNull(message.metadata.get("uri").map(_.toString))},
					${getStringOrNull(message.metadata.get("cid").map(_.toString))},
					'${message.authorId}',
					'${escapeSQL(message.content)}',
					$replyCount,
					$repostCount,
					$likeCount,
					'${message.createdAt}',
					'${message.createdAt}')
				ON CONFLICT (id) DO NOTHING""")
			
			executeBatch(multiInsertions)
		} catch {
			case e: Exception => 
				logger.error("Error inserting Bluesky message", e)
				throw e
		}
	}
}
