package twitter

import com.twitter.clientlib.ApiException
import com.twitter.clientlib.model.{AddOrDeleteRulesRequest, AddRulesRequest, DeleteRulesRequest, DeleteRulesRequestDelete, RuleNoId}
import com.twitter.clientlib.TwitterCredentialsBearer
import com.twitter.clientlib.api.TwitterApi
import models.{Account, Collect, Rule, TemporaryRule}

import scala.jdk.CollectionConverters._

class TwitterConnection(val account: Account) {
	val apiInstance: TwitterApi = new TwitterApi(new TwitterCredentialsBearer(account.bearerToken))
	private var tweetStreamListener: Option[TweetStreamListener] = None
	
	def isCollectRunning: Boolean = tweetStreamListener.isDefined

	/**
	 * Start the collect with the already defined rules.
	 *
	 * @param collect the collect
	 * @return None if a collect is already running, Some(TweetStreamListener) otherwise.
	 */
	def startCollect(collect: Collect): Either[String, TweetStreamListener] = {
		if (this.tweetStreamListener.isEmpty) {
			try {
				val listener = new TweetStreamListener(new TweetStreamingHandler(apiInstance, collect), collect)
				listener.stream().executeListeners()
				Right(listener)
			} catch {
				case e: ApiException => {
					catchApiError(e, "TweetsApi#searchStream")
					Left(catchApiErrorToString(e, "TweetsApi#searchStream"))
				}
			}
		} else {
			Left("Another collect is already running.")
		}
	}
	
	/**
	 * Stop the collect.
	 */
	def stopCollect(): Unit = {
		if (this.tweetStreamListener.isDefined) {
			this.tweetStreamListener.get.shutdown()
			this.tweetStreamListener = None
		}
	}

/*/**
 * Start the collect with the already defined rules.
 *
 * @param collect the collect
 * @return None if a collect is already running, Some(TweetWriter) otherwise.
 */
def startCollect(collect: Collect): Either[String, TweetWriter] = {
	if (this.tweetWriter.isEmpty) {
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
		try {
			val inputStream = apiInstance.tweets().searchStream()
				.tweetFields(tweetFields)
				.expansions(expansions)
				.mediaFields(mediaFields)
				.pollFields(pollFields)
				.userFields(userFields)
				.placeFields(placeFields)
				.execute()
			val tweetWriter = new TweetWriter(inputStream, collect)
			tweetWriter.start()
			this.tweetWriter = Some(tweetWriter)
			Right(tweetWriter)
		} catch {
			case e: ApiException => {
				catchApiError(e, "TweetsApi#searchStream")
				Left(catchApiErrorToString(e, "TweetsApi#searchStream"))
			}
		}
	} else {
		Left("Another collect is already running.")
	}
}

/**
 * Stop the collect.
 */
def stopCollect(): Unit = {
	if (this.tweetWriter.isDefined) {
		this.tweetWriter.get.close()
		this.tweetWriter = None
	}
}*/

/**
 * Add a set of rules to the Twitter instance. The method does not delete the existing rules.
 *
 * @param temporaryRules the temporary rules to add
 * @param rules the rules to add
 * @param collectName the collect concerned by the rules
 * @return the set of rules added to the Twitter instance (right), or a String with the error
 *         returned by the Twitter API (left)
 */
def addRules(collectName: String, temporaryRules: Seq[TemporaryRule], rules: Seq[Rule]): Either[String, Seq[Rule]] = {
	// Prepare the request
	val addOrDeleteRulesRequest = new AddOrDeleteRulesRequest
	val dryRun = true // Check if the rule is corrected without using the rule to collect tweets
	
	// Create Twitter rules
	val addRuleRequest: AddRulesRequest = new AddRulesRequest
	for (rule <- temporaryRules) {
		val newRule: RuleNoId = new RuleNoId
		newRule.value(rule.content)
		newRule.tag(rule.tag)
		addRuleRequest.addAddItem(newRule)
	}
	for (rule <- rules) {
		val newRule: RuleNoId = new RuleNoId
		newRule.value(rule.content)
		newRule.tag(rule.tag)
		addRuleRequest.addAddItem(newRule)
	}
	
	// Add the rules to the request
	addOrDeleteRulesRequest.setActualInstance(addRuleRequest)
	
	try {
		// Send the request
		val resultDryRun = apiInstance.tweets.addOrDeleteRules(addOrDeleteRulesRequest).dryRun(dryRun).execute
		System.out.println(resultDryRun)
		val result = apiInstance.tweets.addOrDeleteRules(addOrDeleteRulesRequest).execute
		System.out.println(result)
		val newRules = result.getData.asScala.map(rule =>
			models.Rule(rule.getId.toLong, rule.getTag, rule.getValue, collectName)).toSeq
		newRules.foreach(_.setActive(true))
		Right(newRules)
	} catch {
		case e: ApiException => {
			catchApiError(e, "TweetsApi#addOrDeleteRules")
			Left(catchApiErrorToString(e, "TweetsApi#addOrDeleteRules"))
		}
	}
}

/**
 * Retrieve all the rules currently linked to the account.
 *
 * @return the rules (right), or a String containing the error returned by the Twitter API (left).
 */
def getAllRules(collectName: String): Either[String, Seq[Rule]] = {
	var rules = Seq[Rule]()
	try {
		val result = apiInstance.tweets().getRules.execute()
		println(result)
		if (result.getData != null) {
			rules = result.getData.asScala.map(rule =>
				models.Rule(rule.getId.toLong, rule.getTag, rule.getValue, collectName)).toSeq
		}
		Right(rules)
	} catch {
		case e: ApiException => {
			catchApiError(e, "TweetsApi#getRules")
			Left(catchApiErrorToString(e, "TweetsApi#getRules"))
		}
	}
}

/**
 * Removes a set of rules from the Twitter instance.
 * @return the result of the operation
 */
def removeRules(rules: Seq[Rule]): Either[String, Seq[Rule]] = {
	try {
		val addOrDeleteRulesRequest = new AddOrDeleteRulesRequest
		val deleteRulesRequest: DeleteRulesRequest = new DeleteRulesRequest
		val deleteRules = new DeleteRulesRequestDelete
		
		deleteRules.ids((for (rule <- rules) yield rule.id.toString).asJava)
		deleteRulesRequest.delete(deleteRules)
		
		addOrDeleteRulesRequest.setActualInstance(deleteRulesRequest)
		val result = apiInstance.tweets().addOrDeleteRules(addOrDeleteRulesRequest).execute()
		println(result)
		
		Right(Seq.empty[Rule]) // Retourne une séquence vide puisque les règles ont été supprimées
	} catch {
		case e: ApiException => 
			catchApiError(e, "TweetsApi#deleteRules")
			Left(catchApiErrorToString(e, "TweetsApi#deleteRules"))
	}
}

private def catchApiErrorToString(e: ApiException, name: String): String = {
	s"""Exception when calling $name
	Status code: ${e.getCode}
	Reason: ${e.getResponseBody}
	Response headers: ${e.getResponseHeaders}"""
}

private def catchApiError(e: ApiException, name: String): Unit = {
	System.err.println(s"Exception when calling $name")
	System.err.println("Status code: " + e.getCode)
	System.err.println("Reason: " + e.getResponseBody)
	System.err.println("Response headers: " + e.getResponseHeaders)
	e.printStackTrace()
}
}
