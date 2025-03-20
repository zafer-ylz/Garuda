package modules.exporter.postgresql

/**
 * Constantes pour les tables PostgreSQL
 */
object PostgresConstants {
  // Tables principales
  val TWEET_TABLE = "tweet"
  val USER_TABLE = "user"
  val WITHHELD_IN_COUNTRY_TABLE = "withheld_in_country"
  val PLACE_TABLE = "place"
  val BLUESKY_POST_TABLE = "bluesky_post"
  val MESSAGE_TABLE = "social_message"
  
  // Tables de relations
  val REPLY_TABLE = "reply"
  val QUOTE_TABLE = "quote"
  val RETWEET_TABLE = "retweet"
  
  // Tables d'entités liées aux tweets
  val TWEET_HASHTAG_TABLE = "tweet_hashtag"
  val TWEET_URL_TABLE = "tweet_url"
  val TWEET_CASHTAG_TABLE = "tweet_cashtag"
  val TWEET_EMOJI_TABLE = "tweet_emoji"
  val TWEET_MEDIA_TABLE = "tweet_media"
  val TWEET_USER_MENTION_TABLE = "tweet_user_mention"
  val TWEET_PLACE_TABLE = "tweet_place"
  val TWEET_ANNOTATION_TABLE = "tweet_annotation"
  val TWEET_TAG_TABLE = "tweet_tag"
} 