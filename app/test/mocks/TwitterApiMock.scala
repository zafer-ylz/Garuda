package test.mocks

import models.SocialMediaMessage
import models.tweet.Tweet
import providers.twitter.TwitterConnection
import providers.ProviderType

class TwitterApiMock extends TwitterConnection {
  private var tweets: List[Tweet] = List()
  private var isConnected: Boolean = false
  
  override def validateCredentials(): Boolean = {
    isConnected = true
    true
  }
  
  override def addRule(rule: String): Boolean = {
    true
  }
  
  override def getRules(): List[String] = {
    List()
  }
  
  override def removeRule(rule: String): Boolean = {
    true
  }
  
  override def startStreaming(): Boolean = {
    true
  }
  
  override def stopStreaming(): Boolean = {
    true
  }
  
  // Méthodes pour les tests
  def addTestTweet(tweet: Tweet): Unit = {
    tweets = tweet :: tweets
  }
  
  def clearTweets(): Unit = {
    tweets = List()
  }
  
  def getTestTweets: List[Tweet] = tweets
  
  def simulateTweetReceived(tweet: Tweet): Unit = {
    val message = SocialMediaMessage(
      id = tweet.id.getOrElse(""),
      providerType = ProviderType.Twitter,
      content = tweet.text.getOrElse(""),
      authorId = tweet.userId.getOrElse(""),
      createdAt = new org.joda.time.DateTime(tweet.publishedTimeMs.getOrElse(System.currentTimeMillis())),
      metadata = Map(
        "source" -> tweet.source.getOrElse(""),
        "lang" -> tweet.lang.getOrElse("")
      )
    )
    onMessageReceived(message)
  }
  
  def isStreamingActive: Boolean = isConnected
} 