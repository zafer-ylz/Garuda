package test.unit.providers

import models.SocialAccount
import models.tweet.Tweet
import org.scalatestplus.play.PlaySpec
import providers.ProviderType
import test.mocks.TwitterApiMock

class TwitterProviderTest extends PlaySpec {
  "TwitterProvider" should {
    "create a valid Twitter account" in {
      val provider = new TwitterProvider()
      val account = SocialAccount(
        name = "test_account",
        identifier = "test_user",
        password = "test_password",
        providerType = ProviderType.Twitter,
        createdAt = new org.joda.time.DateTime()
      )
      
      val connection = provider.createConnection(account)
      connection mustBe a[TwitterApiMock]
      connection.validateCredentials() mustBe true
    }
    
    "validate credentials correctly" in {
      val provider = new TwitterProvider()
      val account = SocialAccount(
        name = "test_account",
        identifier = "test_user",
        password = "test_password",
        providerType = ProviderType.Twitter,
        createdAt = new org.joda.time.DateTime()
      )
      
      val connection = provider.createConnection(account)
      provider.validateCredentials(account) mustBe true
    }
    
    "handle tweet processing" in {
      val provider = new TwitterProvider()
      val account = SocialAccount(
        name = "test_account",
        identifier = "test_user",
        password = "test_password",
        providerType = ProviderType.Twitter,
        createdAt = new org.joda.time.DateTime()
      )
      
      val connection = provider.createConnection(account)
      val tweet = Tweet(
        id = Some("123456"),
        text = Some("Test tweet"),
        userId = Some("test_user"),
        publishedTimeMs = Some(System.currentTimeMillis()),
        source = Some("Twitter for iPhone"),
        lang = Some("fr")
      )
      
      connection.asInstanceOf[TwitterApiMock].simulateTweetReceived(tweet)
      connection.asInstanceOf[TwitterApiMock].getTestTweets must contain(tweet)
    }
  }
} 