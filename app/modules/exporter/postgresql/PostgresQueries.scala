package modules.exporter.postgresql

/**
 * Requêtes SQL pour PostgreSQL
 */
object PostgresQueries {
  def createSchema(schema: String): String = s"CREATE SCHEMA IF NOT EXISTS $schema"
  
  def createTweetTable(schema: String): String = s"""
    CREATE TABLE IF NOT EXISTS $schema.${PostgresConstants.TWEET_TABLE}(
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
    
  def createUserTable(schema: String): String = s"""
    CREATE TABLE IF NOT EXISTS $schema.${PostgresConstants.USER_TABLE}(
      id TEXT PRIMARY KEY,
      screen_name TEXT,
      name TEXT,
      created_at TIMESTAMP,
      verified BOOLEAN,
      protected BOOLEAN
    )"""
    
  def createWithheldInCountryTable(schema: String): String = s"""
    CREATE TABLE IF NOT EXISTS $schema.${PostgresConstants.WITHHELD_IN_COUNTRY_TABLE}(
      user_id TEXT,
      country TEXT,
      PRIMARY KEY(user_id, country)
    )"""
    
  def createPlaceTable(schema: String): String = s"""
    CREATE TABLE IF NOT EXISTS $schema.${PostgresConstants.PLACE_TABLE}(
      id TEXT PRIMARY KEY,
      name TEXT,
      full_name TEXT,
      country_code TEXT,
      country TEXT,
      place_type TEXT,
      bounding_box TEXT,
      type_bounding_box TEXT
    )"""
    
  def createReplyTable(schema: String): String = s"""
    CREATE TABLE IF NOT EXISTS $schema.${PostgresConstants.REPLY_TABLE}(
      tweet_id TEXT,
      in_reply_to_tweet_id TEXT,
      in_reply_to_user_id TEXT,
      in_reply_to_screen_name TEXT,
      PRIMARY KEY(tweet_id, in_reply_to_tweet_id)
    )"""
    
  def createQuoteTable(schema: String): String = s"""
    CREATE TABLE IF NOT EXISTS $schema.${PostgresConstants.QUOTE_TABLE}(
      tweet_id TEXT,
      quoted_tweet_id TEXT,
      PRIMARY KEY(tweet_id, quoted_tweet_id)
    )"""
    
  def createRetweetTable(schema: String): String = s"""
    CREATE TABLE IF NOT EXISTS $schema.${PostgresConstants.RETWEET_TABLE}(
      tweet_id TEXT,
      retweeted_tweet_id TEXT,
      PRIMARY KEY(tweet_id, retweeted_tweet_id)
    )"""
    
  def createTweetHashtagTable(schema: String): String = s"""
    CREATE TABLE IF NOT EXISTS $schema.${PostgresConstants.TWEET_HASHTAG_TABLE}(
      tweet_id TEXT,
      rank INTEGER,
      hashtag TEXT,
      start_indice INTEGER,
      end_indice INTEGER,
      PRIMARY KEY(tweet_id, rank)
    )"""
    
  def createTweetUrlTable(schema: String): String = s"""
    CREATE TABLE IF NOT EXISTS $schema.${PostgresConstants.TWEET_URL_TABLE}(
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
    
  def createTweetCashtagTable(schema: String): String = s"""
    CREATE TABLE IF NOT EXISTS $schema.${PostgresConstants.TWEET_CASHTAG_TABLE}(
      tweet_id TEXT,
      rank INTEGER,
      cashtag TEXT,
      start_indice INTEGER,
      end_indice INTEGER,
      PRIMARY KEY(tweet_id, rank)
    )"""
    
  def createTweetEmojiTable(schema: String): String = s"""
    CREATE TABLE IF NOT EXISTS $schema.${PostgresConstants.TWEET_EMOJI_TABLE}(
      tweet_id TEXT,
      rank INTEGER,
      emoji TEXT,
      start_indice INTEGER,
      end_indice INTEGER,
      PRIMARY KEY(tweet_id, rank)
    )"""
    
  def createTweetMediaTable(schema: String): String = s"""
    CREATE TABLE IF NOT EXISTS $schema.${PostgresConstants.TWEET_MEDIA_TABLE}(
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
    
  def createTweetUserMentionTable(schema: String): String = s"""
    CREATE TABLE IF NOT EXISTS $schema.${PostgresConstants.TWEET_USER_MENTION_TABLE}(
      tweet_id TEXT,
      rank INTEGER,
      user_id TEXT,
      start_indice INTEGER,
      end_indice INTEGER,
      PRIMARY KEY(tweet_id, rank)
    )"""
    
  def createTweetPlaceTable(schema: String): String = s"""
    CREATE TABLE IF NOT EXISTS $schema.${PostgresConstants.TWEET_PLACE_TABLE}(
      tweet_id TEXT,
      place_id TEXT,
      PRIMARY KEY(tweet_id, place_id)
    )"""
    
  def createTweetAnnotationTable(schema: String): String = s"""
    CREATE TABLE IF NOT EXISTS $schema.${PostgresConstants.TWEET_ANNOTATION_TABLE}(
      tweet_id TEXT,
      rank INTEGER,
      annotation_type TEXT,
      normalized_text TEXT,
      start_indice INTEGER,
      end_indice INTEGER,
      PRIMARY KEY(tweet_id, rank)
    )"""
    
  def createTweetTagTable(schema: String): String = s"""
    CREATE TABLE IF NOT EXISTS $schema.${PostgresConstants.TWEET_TAG_TABLE}(
      tweet_id TEXT,
      tag TEXT,
      PRIMARY KEY(tweet_id, tag)
    )"""
    
  def createBlueskyPostTable(schema: String): String = s"""
    CREATE TABLE IF NOT EXISTS $schema.${PostgresConstants.BLUESKY_POST_TABLE}(
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
      FOREIGN KEY (id) REFERENCES $schema.${PostgresConstants.MESSAGE_TABLE}(id)
    )"""
    
  def createSocialMessageTable(schema: String): String = s"""
    CREATE TABLE IF NOT EXISTS $schema.${PostgresConstants.MESSAGE_TABLE}(
      id TEXT PRIMARY KEY,
      provider_type TEXT NOT NULL,
      content TEXT,
      author_id TEXT,
      created_at TIMESTAMP,
      metadata JSONB
    )"""
} 