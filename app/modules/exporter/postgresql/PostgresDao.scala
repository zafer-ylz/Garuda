package modules.exporter.postgresql

import java.sql._
import java.util.Properties

import models.SocialMediaMessage
import models.tweet.{Annotation, Cashtag, Hashtag, Media, Place, Tweet, Url, User, UserMention}
import play.api.Logging
import providers.ProviderType

class PostgresDao(val config: PostgresConfig) extends Logging {
	
	case class MultiInsertions(stmt: Statement)
	
	Class.forName("org.postgresql.Driver")
	private val conn: Connection = DriverManager.getConnection(config.getUrl, config.getProperties)
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
		
		try {
			// Création du schéma
			st.execute(PostgresQueries.createSchema(schema))
			
			// Création des tables principales
			st.execute(PostgresQueries.createTweetTable(schema))
			st.execute(PostgresQueries.createUserTable(schema))
			st.execute(PostgresQueries.createWithheldInCountryTable(schema))
			st.execute(PostgresQueries.createPlaceTable(schema))
			st.execute(PostgresQueries.createSocialMessageTable(schema))
			st.execute(PostgresQueries.createBlueskyPostTable(schema))
			
			// Création des tables de relations
			st.execute(PostgresQueries.createReplyTable(schema))
			st.execute(PostgresQueries.createQuoteTable(schema))
			st.execute(PostgresQueries.createRetweetTable(schema))
			
			// Création des tables d'entités liées aux tweets
			st.execute(PostgresQueries.createTweetHashtagTable(schema))
			st.execute(PostgresQueries.createTweetUrlTable(schema))
			st.execute(PostgresQueries.createTweetCashtagTable(schema))
			st.execute(PostgresQueries.createTweetEmojiTable(schema))
			st.execute(PostgresQueries.createTweetMediaTable(schema))
			st.execute(PostgresQueries.createTweetUserMentionTable(schema))
			st.execute(PostgresQueries.createTweetPlaceTable(schema))
			st.execute(PostgresQueries.createTweetAnnotationTable(schema))
			st.execute(PostgresQueries.createTweetTagTable(schema))
		} finally {
			st.close()
		}
	}
	
	def addBatchTweet(multiInsertions: MultiInsertions, tweet: Tweet): Unit = {
		assert(tweet.id.isDefined)
		assert(tweet.userId.isDefined)
		
		val sql = s"""INSERT INTO $schema.$TWEET_TABLE(
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
			'${tweet.createdAt}',
			${tweet.publishedTime},
			'${tweet.userId.get}',
			'${escapeSQL(tweet.userName)}',
			'${escapeSQL(tweet.userScreenName)}',
			'${escapeSQL(tweet.text)}',
			'${escapeSQL(tweet.source)}',
			'${escapeSQL(tweet.language)}',
			'${tweet.coordinates.map(_.longitude).getOrElse("")}',
			'${tweet.coordinates.map(_.latitude).getOrElse("")}',
			${tweet.possiblySensitive})
		ON CONFLICT (id) DO NOTHING"""
		
		multiInsertions.stmt.addBatch(sql)
	}
	
	def addBatchUser(multiInsertions: MultiInsertions, user: User): Unit = {
		assert(user.id.isDefined)
		
		val sql = s"""INSERT INTO $schema.$USER_TABLE(
			id,
			screen_name,
			name,
			created_at,
			verified,
			protected)
		VALUES(
			'${user.id.get}',
			'${user.screenName.map(escapeSQL).getOrElse("")}',
			'${user.name.map(escapeSQL).getOrElse("")}',
			'${user.createdAt.getOrElse("")}',
			${user.verified.getOrElse(false)},
			${user.`protected`.getOrElse(false)})
		ON CONFLICT (id) DO NOTHING"""
		
		multiInsertions.stmt.addBatch(sql)
	}
	
	def addBatchUserWithheldInCountry(multiInsertions: MultiInsertions, userId: String, country: String): Unit = {
		val sql = s"""INSERT INTO $schema.$WITHHELD_IN_COUNTRY_TABLE(
		    	user_id,
		    	country)
	    	VALUES(
		    	'$userId',
		    	'${escapeSQL(country)}')
            ON CONFLICT (user_id, country) DO NOTHING"""
		
		multiInsertions.stmt.addBatch(sql)
	}
	
	def addBatchPlace(multiInsertions: MultiInsertions, place: Place): Unit = {
		assert(place.id.isDefined)
		
		val sql = s"""INSERT INTO $schema.$PLACE_TABLE(
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
			'${escapeSQL(place.name)}',
			'${escapeSQL(place.fullName)}',
			'${escapeSQL(place.countryCode)}',
			'${escapeSQL(place.country)}',
			'${escapeSQL(place.placeType)}',
			'${escapeSQL(place.boundingBox)}',
			'${escapeSQL(place.typeBoundingBox)}')
        ON CONFLICT (id) DO NOTHING"""
		
		multiInsertions.stmt.addBatch(sql)
	}
	
	def addBatchReply(multiInsertions: MultiInsertions, tweetId: String, inReplyToTweetId: String, inReplyToUserId: String, inReplyToScreenName: String): Unit = {
		val sql = s"""INSERT INTO $schema.$REPLY_TABLE(
			tweet_id,
			in_reply_to_tweet_id,
			in_reply_to_user_id,
			in_reply_to_screen_name)
		VALUES(
			'$tweetId',
			'$inReplyToTweetId',
			'$inReplyToUserId',
			'${escapeSQL(inReplyToScreenName)}')
		ON CONFLICT (tweet_id, in_reply_to_tweet_id) DO NOTHING"""
		
		multiInsertions.stmt.addBatch(sql)
	}
	
	def addBatchQuote(multiInsertions: MultiInsertions, tweetId: String, quotedTweetId: String): Unit = {
		val sql = s"""INSERT INTO $schema.$QUOTE_TABLE(
			tweet_id,
			quoted_tweet_id)
		VALUES(
			'$tweetId',
			'$quotedTweetId')
		ON CONFLICT (tweet_id, quoted_tweet_id) DO NOTHING"""
		
		multiInsertions.stmt.addBatch(sql)
	}
	
	def addBatchRetweet(multiInsertions: MultiInsertions, tweetId: String, retweetedTweetId: String): Unit = {
		val sql = s"""INSERT INTO $schema.$RETWEET_TABLE(
			tweet_id,
			retweeted_tweet_id)
		VALUES(
			'$tweetId',
			'$retweetedTweetId')
		ON CONFLICT (tweet_id, retweeted_tweet_id) DO NOTHING"""
		
		multiInsertions.stmt.addBatch(sql)
	}
	
	def addBatchTweetHashtags(multiInsertions: MultiInsertions, tweetId: String, hashtags: scala.Array[Hashtag]): Unit = {
		for (i <- hashtags.indices) {
			val sql = s"""INSERT INTO $schema.$TWEET_HASHTAG_TABLE(
				tweet_id,
				rank,
				hashtag,
				start_indice,
				end_indice)
			VALUES(
				'$tweetId',
				${i + 1},
				'${escapeSQL(hashtags(i).text)}',
				${hashtags(i).startIndice},
				${hashtags(i).endIndice})
			ON CONFLICT (tweet_id, rank) DO NOTHING"""
			
			multiInsertions.stmt.addBatch(sql)
		}
	}
	
	def addBatchTweetUrls(multiInsertions: MultiInsertions, tweetId: String, urls: scala.Array[Url]): Unit = {
		for (i <- urls.indices) {
			val sql = s"""INSERT INTO $schema.$TWEET_URL_TABLE(
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
				'${escapeSQL(urls(i).url)}',
				'${escapeSQL(urls(i).expandedUrl)}',
				'${escapeSQL(urls(i).displayUrl)}',
				${urls(i).status},
				'${escapeSQL(urls(i).title.map(_.replace("'", "''")).getOrElse(""))}',
				'${escapeSQL(urls(i).description.map(_.replace("'", "''")).getOrElse(""))}',
				${urls(i).startIndice},
				${urls(i).endIndice})
            ON CONFLICT (tweet_id, rank) DO NOTHING"""
			
			multiInsertions.stmt.addBatch(sql)
		}
	}
	
	def addBatchTweetCashtags(multiInsertions: MultiInsertions, tweetId: String, cashtags: scala.Array[Cashtag]): Unit = {
		for (i <- cashtags.indices) {
			val sql = s"""INSERT INTO $schema.$TWEET_CASHTAG_TABLE(
				tweet_id,
				rank,
				cashtag,
				start_indice,
				end_indice)
			VALUES(
				'$tweetId',
				${i + 1},
				'${escapeSQL(cashtags(i).text)}',
				${cashtags(i).startIndice},
				${cashtags(i).endIndice})
			ON CONFLICT (tweet_id, rank) DO NOTHING"""
			
			multiInsertions.stmt.addBatch(sql)
		}
	}
	
	def addBatchTweetMedias(multiInsertions: MultiInsertions, tweetId: String, medias: scala.Array[Media]): Unit = {
		for (i <- medias.indices) {
			val media: Media = medias(i)
			val sql = s"""INSERT INTO $schema.$TWEET_MEDIA_TABLE(
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
				'${escapeSQL(media.key)}',
				'${escapeSQL(media.mediaType)}',
				'${escapeSQL(media.mediaUrl)}',
				${media.durationMs},
				${media.height},
				${media.width},
 				'${escapeSQL(media.previewImageUrl)}',
	 			${media.viewCount},
  				'${escapeSQL(media.alternativeText.map(_.replace("'", "''")).getOrElse(""))}')
			ON CONFLICT (tweet_id, rank) DO NOTHING"""
			
			multiInsertions.stmt.addBatch(sql)
		}
	}
	
	def addBatchTweetUserMentions(multiInsertions: MultiInsertions, tweetId: String, mentions: scala.Array[UserMention]): Unit = {
		for (i <- mentions.indices) {
			if (mentions(i).userId != null) {
				val sql = s"""INSERT INTO $schema.$TWEET_USER_MENTION_TABLE(
					tweet_id,
					rank,
					user_id,
					start_indice,
					end_indice)
				VALUES(
					'$tweetId',
					${i + 1},
					'${escapeSQL(mentions(i).userId)}',
					${mentions(i).startIndice},
					${mentions(i).endIndice})
				ON CONFLICT (tweet_id, rank) DO NOTHING"""
				
				multiInsertions.stmt.addBatch(sql)
			}
		}
	}
	
	def addBatchTweetPlace(multiInsertions: MultiInsertions, tweetId: String, placeId: String): Unit = {
		val sql = s"""INSERT INTO $schema.$TWEET_PLACE_TABLE(
	    	  tweet_id,
	    	  place_id)
    	  VALUES(
	    	  '$tweetId',
	    	  '$placeId')
          ON CONFLICT (tweet_id, place_id) DO NOTHING"""
		
		multiInsertions.stmt.addBatch(sql)
	}
	
	def addBatchAnnotation(multiInsertions: MultiInsertions, tweetId: String, annotations: scala.Array[Annotation]): Unit = {
		for (i <- annotations.indices) {
			val sql = s"""INSERT INTO $schema.$TWEET_ANNOTATION_TABLE(
				tweet_id,
				rank,
				annotation_type,
				normalized_text,
				start_indice,
				end_indice)
			VALUES(
				'$tweetId',
				${i + 1},
				'${escapeSQL(annotations(i).annotationType)}',
				'${escapeSQL(annotations(i).normalizedText)}',
				${annotations(i).startIndice},
				${annotations(i).endIndice})
			ON CONFLICT (tweet_id, rank) DO NOTHING"""
			
			multiInsertions.stmt.addBatch(sql)
		}
	}
	
	def addBatchTag(multiInsertions: MultiInsertions, tweetId: String, tag: String): Unit = {
		val sql = s"""INSERT INTO $schema.$TWEET_TAG_TABLE(
				tweet_id,
				tag)
			VALUES(
				'$tweetId',
				'${escapeSQL(tag)}')
			ON CONFLICT (tweet_id, tag) DO NOTHING"""
		
		multiInsertions.stmt.addBatch(sql)
	}
	
	def addBatchBlueskyPost(multiInsertions: MultiInsertions, message: SocialMediaMessage): Unit = {
		val sql = s"""INSERT INTO $schema.$BLUESKY_POST_TABLE(
				id,
				uri,
				cid,
				author,
				text,
				reply_count,
				repost_count,
				like_count,
				created_at,
				indexed_at)
			VALUES(
				'${message.id}',
				'${escapeSQL(message.metadata.getOrElse("uri", "").toString)}',
				'${escapeSQL(message.metadata.getOrElse("cid", "").toString)}',
				'${escapeSQL(message.metadata.getOrElse("author", "").toString)}',
				'${escapeSQL(message.content)}',
				${message.metadata.getOrElse("reply_count", 0).toString.toInt},
				${message.metadata.getOrElse("repost_count", 0).toString.toInt},
				${message.metadata.getOrElse("like_count", 0).toString.toInt},
				'${message.createdAt}',
				'${message.metadata.getOrElse("indexed_at", message.createdAt).toString}')
			ON CONFLICT (id) DO NOTHING"""
		
		multiInsertions.stmt.addBatch(sql)
	}
	
	def addBatchSocialMessage(multiInsertions: MultiInsertions, message: SocialMediaMessage): Unit = {
		val sql = s"""INSERT INTO $schema.$MESSAGE_TABLE(
				id,
				provider_type,
				content,
				author_id,
				created_at,
				metadata)
			VALUES(
				'${message.id}',
				'${message.providerType}',
				'${escapeSQL(message.content)}',
				'${message.authorId}',
				'${message.createdAt}',
				'${message.metadata.map(_.toString).getOrElse("{}")}')
			ON CONFLICT (id) DO NOTHING"""
		
		multiInsertions.stmt.addBatch(sql)
	}
	
	def getMultiInsertions: MultiInsertions = {
		MultiInsertions(conn.createStatement())
	}
	
	def executeBatch(multiInsertions: MultiInsertions): Unit = {
		try {
			multiInsertions.stmt.executeBatch()
		} finally {
			multiInsertions.stmt.close()
		}
	}
	
	private def escapeSQL(str: String): String = {
		str.replace("'", "''")
	}
	
	def close(): Unit = {
		if (!conn.isClosed) {
			logger.info("Shutdown PostgresDao connection.")
			conn.close()
		}
	}
}
