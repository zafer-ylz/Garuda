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
		st.execute(PostgresQueries.createSchema(schema))
		
		st.execute(PostgresQueries.createTweetTable(schema))
		st.execute(PostgresQueries.createUserTable(schema))
		st.execute(PostgresQueries.createWithheldInCountryTable(schema))
		st.execute(PostgresQueries.createPlaceTable(schema))
		st.execute(PostgresQueries.createSocialMessageTable(schema))
		st.execute(PostgresQueries.createBlueskyPostTable(schema))
		
		st.execute(PostgresQueries.createReplyTable(schema))
		st.execute(PostgresQueries.createQuoteTable(schema))
		st.execute(PostgresQueries.createRetweetTable(schema))
		
		st.execute(PostgresQueries.createTweetHashtagTable(schema))
		st.execute(PostgresQueries.createTweetUrlTable(schema))
		st.execute(PostgresQueries.createTweetCashtagTable(schema))
		st.execute(PostgresQueries.createTweetEmojiTable(schema))
		st.execute(PostgresQueries.createTweetMediaTable(schema))
		st.execute(PostgresQueries.createTweetUserMentionTable(schema))
		st.execute(PostgresQueries.createTweetPlaceTable(schema))
		st.execute(PostgresQueries.createTweetAnnotationTable(schema))
		st.execute(PostgresQueries.createTweetTagTable(schema))
		
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
			'${tweet.createdAt}',
			${tweet.publishedTime},
			'${tweet.userId.get}',
			'${tweet.userName}',
			'${tweet.userScreenName}',
			'${tweet.text.replace("'", "''")}',
			'${tweet.source.replace("'", "''")}',
			'${tweet.language}',
			'${tweet.coordinates.map(_.longitude).getOrElse("")}',
			'${tweet.coordinates.map(_.latitude).getOrElse("")}',
			${tweet.possiblySensitive})
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
			'${user.screenName.replace("'", "''")}',
			'${user.name.replace("'", "''")}',
			'${user.createdAt}',
			${user.verified},
			${user.protected})
        ON CONFLICT (id) DO NOTHING""")
	}
	
	def addBatchUserWithheldInCountry(multiInsertions: MultiInsertions, userId: String, country: String): Unit = {
		multiInsertions.stmt.addBatch(s"""INSERT INTO $schema.$WITHHELD_IN_COUNTRY_TABLE(
		    	user_id,
		    	country)
	    	VALUES(
		    	'$userId',
		    	'$country')
            ON CONFLICT (user_id, country) DO NOTHING""")
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
			'${place.name.replace("'", "''")}',
			'${place.fullName.replace("'", "''")}',
			'${place.countryCode}',
			'${place.country.replace("'", "''")}',
			'${place.placeType}',
			'${place.boundingBox}',
			'${place.typeBoundingBox}')
        ON CONFLICT (id) DO NOTHING""")
	}
	
	def addBatchReply(multiInsertions: MultiInsertions, tweetId: String, inReplyToTweetId: String, inReplyToUserId: String, inReplyToScreenName: String): Unit = {
		multiInsertions.stmt.addBatch(s"""INSERT INTO $schema.$REPLY_TABLE(
			tweet_id,
			in_reply_to_tweet_id,
			in_reply_to_user_id,
			in_reply_to_screen_name)
		VALUES(
			'$tweetId',
			'$inReplyToTweetId',
			'$inReplyToUserId',
			'${inReplyToScreenName.replace("'", "''")}')
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
				'${hashtags(i).text.replace("'", "''")}',
				${hashtags(i).startIndice},
				${hashtags(i).endIndice})
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
				'${urls(i).url.replace("'", "''")}',
				'${urls(i).expandedUrl.replace("'", "''")}',
				'${urls(i).displayUrl.replace("'", "''")}',
				${urls(i).status},
				'${urls(i).title.map(_.replace("'", "''")).getOrElse("")}',
				'${urls(i).description.map(_.replace("'", "''")).getOrElse("")}',
				${urls(i).startIndice},
				${urls(i).endIndice})
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
				'${cashtags(i).text.replace("'", "''")}',
				${cashtags(i).startIndice},
				${cashtags(i).endIndice})
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
				'${media.key.replace("'", "''")}',
				'${media.mediaType}',
				'${media.mediaUrl.replace("'", "''")}',
				${media.durationMs},
				${media.height},
				${media.width},
 				'${media.previewImageUrl.replace("'", "''")}',
	 			${media.viewCount},
  				'${media.alternativeText.map(_.replace("'", "''")).getOrElse("")}')
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
					'${mentions(i).userId}',
					${mentions(i).startIndice},
					${mentions(i).endIndice})
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
				'${annotations(i).annotationType}',
				'${annotations(i).normalizedText.replace("'", "''")}',
				${annotations(i).startIndice},
				${annotations(i).endIndice})
			ON CONFLICT (tweet_id, rank) DO NOTHING""")
		}
	}
	
	def addBatchTag(multiInsertions: MultiInsertions, tweetId: String, tag: String): Unit = {
		multiInsertions.stmt.addBatch(s"""INSERT INTO $schema.$TWEET_TAG_TABLE(
				tweet_id,
				tag)
			VALUES(
				'$tweetId',
				'${tag.replace("'", "''")}')
			ON CONFLICT (tweet_id, tag) DO NOTHING""")
	}
	
	def addBatchBlueskyPost(multiInsertions: MultiInsertions, message: SocialMediaMessage): Unit = {
		multiInsertions.stmt.addBatch(
			s"""INSERT INTO $schema.$BLUESKY_POST_TABLE(
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
				'${message.metadata.getOrElse("uri", "").toString.replace("'", "''")}',
				'${message.metadata.getOrElse("cid", "").toString.replace("'", "''")}',
				'${message.metadata.getOrElse("author", "").toString.replace("'", "''")}',
				'${message.content.replace("'", "''")}',
				${message.metadata.getOrElse("reply_count", 0).toString.toInt},
				${message.metadata.getOrElse("repost_count", 0).toString.toInt},
				${message.metadata.getOrElse("like_count", 0).toString.toInt},
				'${message.createdAt}',
				'${message.metadata.getOrElse("indexed_at", message.createdAt).toString}')
			ON CONFLICT (id) DO NOTHING""")
	}
	
	def addBatchSocialMessage(multiInsertions: MultiInsertions, message: SocialMediaMessage): Unit = {
		multiInsertions.stmt.addBatch(
			s"""INSERT INTO $schema.$MESSAGE_TABLE(
				id,
				provider_type,
				content,
				author_id,
				created_at,
				metadata)
			VALUES(
				'${message.id}',
				'${message.providerType}',
				'${message.content.replace("'", "''")}',
				'${message.authorId}',
				'${message.createdAt}',
				'${message.metadata.map(_.toString).getOrElse("{}")}')
			ON CONFLICT (id) DO NOTHING""")
	}
	
	def getMultiInsertions: MultiInsertions = {
		MultiInsertions(conn.createStatement())
	}
	
	def executeBatch(multiInsertions: MultiInsertions): Unit = {
		multiInsertions.stmt.executeBatch()
		multiInsertions.stmt.close()
	}
	
	def close(): Unit = {
		if (!conn.isClosed) {
			logger.info("Shutdown PostgresDao connection.")
			conn.close()
		}
	}
}
