package twitter

import java.io.{File, InputStream, PrintWriter}
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}
import java.util.{Arrays => JArrays, HashSet => JHashSet, Set => JSet}

import com.twitter.clientlib.{ApiException, JSON}
import com.twitter.clientlib.api.TwitterApi
import com.twitter.clientlib.model.{ConnectionExceptionProblem, OperationalDisconnectProblem, StreamingTweetResponse}
import models.Collect
import org.joda.time.DateTime
import play.api.Logging

class TweetStreamingHandler(val twitterApi: TwitterApi, val collect: Collect) extends StreamingHandler[StreamingTweetResponse] with Logging {
	
	new JSON()
	
	private val directory = if (collect.directory.endsWith(File.separator)) collect.directory else collect.directory + File.separator
	private val file = collect.observableFile
	file.setFile(new File(directory + getFileName))
	private var errorFile = new File(directory + "Errors" + File.separator + getFileName)
	file.getFile.getParentFile.mkdirs()
	errorFile.getParentFile.mkdirs()
	private var printWriter = new PrintWriter(file.getFile)
	private var errorPrintWriter = new PrintWriter(errorFile)
	var continueWriting: Boolean = true
	private val lockFile = new AnyRef
	
	@throws(classOf[ApiException])
	override def actionOnStreamingObject(tweetString: String, streamingTweet: StreamingTweetResponse): Unit = {
		if(streamingTweet != null) {
			if (streamingTweet.getData != null) {
				lockFile.synchronized {
					printWriter.write(tweetString+"\n")
				}
			} else {
				lockFile.synchronized {
					errorPrintWriter.write(tweetString + "\n")
				}
			}
		}
	}
	
	override def getStreamingObject(tweetString: String): StreamingTweetResponse = {
		StreamingTweetResponse.fromJson(tweetString)
	}
	
	/**
	 * Program a task to change file every 24h.
	 */
	private val ex = new ScheduledThreadPoolExecutor(1)
	private val task = new Runnable {
		override def run(): Unit = {
			try {
				lockFile.synchronized {
					printWriter.close()
					file.setFile(new File(directory + getFileName))
					printWriter = new PrintWriter(file.getFile)
					logger.info(s"New file ${file.getFile.getAbsolutePath} created.")
					errorPrintWriter.close()
					if (errorFile.length() == 0) {
						errorFile.delete()
					}
					errorFile = new File(directory + "Errors" + File.separator + getFileName)
					errorPrintWriter = new PrintWriter(errorFile)
				}
			} catch {
				case e: Exception => e.printStackTrace()
			}
			
		}
	}
	private val scheduler = ex.scheduleAtFixedRate(task, 24, 24, TimeUnit.HOURS)
	scheduler.cancel(false)
	
	/**
	 * Check if Twitter send an error that implies a reconnection to the API.
	 *
	 * @param streamingTweet the object read from the stream.
	 * @return true if there is a reconnection error, false otherwise.
	 */
	override def hasReconnectErrors(streamingTweet: StreamingTweetResponse): Boolean = {
		import scala.jdk.CollectionConverters._
		
		var needToReconnect: Boolean = false
		
		if (streamingTweet.getErrors != null) {
			for (problem <- streamingTweet.getErrors.asScala if !needToReconnect) {
				if (problem.isInstanceOf[OperationalDisconnectProblem]
					|| problem.isInstanceOf[ConnectionExceptionProblem]) {
					logger.warn(s"Re-connecting to the stream due to: $problem")
					needToReconnect = true
				}
			}
		}
		needToReconnect
	}
	
	/**
	 * Method to get a stream of tweets from the Twitter API.
	 *
	 * @throws com.twitter.clientlib.ApiException when a problem occurs with the Twitter API.
	 * @return the InputStream connected to the Twitter API.
	 */
	@throws(classOf[ApiException])
	override def connectStream(): InputStream = {
		val tweetFields: JSet[String] = new JHashSet[String](JArrays.asList(
			"attachments",
			"author_id",
			"context_annotations",
			"conversation_id",
			"created_at",
			"edit_controls",
			"edit_history_tweet_ids",
			"entities",
			"geo",
			"id",
			"in_reply_to_user_id",
			"lang",
			"non_public_metrics",
			"organic_metrics",
			"possibly_sensitive",
			"promoted_metrics",
			"public_metrics",
			"referenced_tweets",
			"reply_settings",
			"source",
			"text",
			"withheld"
		)) // Set<String> | A comma separated list of Tweet fields to display.
		val expansions: JSet[String] = new JHashSet[String](JArrays.asList(
			"attachments.media_keys",
			"attachments.poll_ids",
			"author_id",
			"edit_history_tweet_ids",
			"entities.mentions.username",
			"geo.place_id",
			"in_reply_to_user_id",
			"referenced_tweets.id",
			"referenced_tweets.id.author_id"
		)) // Set<String> | A comma separated list of fields to expand.
		val mediaFields: JSet[String] = new JHashSet[String](JArrays.asList(
			"alt_text",
			"duration_ms",
			"height",
			"media_key",
			"non_public_metrics",
			"organic_metrics",
			"preview_image_url",
			"promoted_metrics",
			"public_metrics",
			"type",
			"url",
			"variants",
			"width"
		)) // Set<String> | A comma separated list of Media fields to display.
		val pollFields: JSet[String] = new JHashSet[String](JArrays.asList(
			"duration_minutes",
			"end_datetime",
			"id",
			"options",
			"voting_status"
		)) // Set<String> | A comma separated list of Poll fields to display.
		val userFields: JSet[String] = new JHashSet[String](JArrays.asList(
			"created_at",
			"description",
			"entities",
			"id",
			"location",
			"name",
			"pinned_tweet_id",
			"profile_image_url",
			"protected",
			"public_metrics",
			"url",
			"username",
			"verified",
			"withheld"
		)) // Set<String> | A comma separated list of User fields to display.
		val placeFields: JSet[String] = new JHashSet[String](JArrays.asList(
			"contained_within",
			"country",
			"country_code",
			"full_name",
			"geo",
			"id",
			"name",
			"place_type"
		)) // Set<String> | A comma separated list of Place fields to display.
		
		twitterApi.tweets().searchStream()
			.backfillMinutes(0)
			.tweetFields(tweetFields)
			.expansions(expansions)
			.mediaFields(mediaFields)
			.pollFields(pollFields)
			.userFields(userFields)
			.placeFields(placeFields)
			.execute()
	}
	
	private def getFileName: String = {
		val createTime = DateTime.now()
		val directoryName = s"${createTime.getYear}-${createTime.getMonthOfYear}"
		val fileName = s"$directoryName-${createTime.getDayOfMonth}_${createTime.getHourOfDay}-${createTime.getMinuteOfHour}-${createTime.getSecondOfMinute}-${createTime.getMillisOfSecond}"
		s"$directoryName${File.separator}$fileName"
	}
}
