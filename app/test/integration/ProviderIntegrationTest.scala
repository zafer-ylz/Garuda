package test.integration

import models.SocialAccount
import models.SocialMediaMessage
import org.scalatestplus.play.PlaySpec
import providers.{ProviderManager, ProviderType}
import services.AccountService
import test.mocks.{TwitterApiMock, BlueskyApiMock}
import org.joda.time.DateTime

class ProviderIntegrationTest extends PlaySpec {
  "Provider Integration" should {
    "handle multiple providers simultaneously" in {
      val providerManager = new ProviderManager()
      val accountService = new AccountService(providerManager)
      
      // Créer un compte Twitter
      val twitterAccount = SocialAccount(
        name = "twitter_test",
        identifier = "test_user",
        password = "test_password",
        providerType = ProviderType.Twitter,
        createdAt = new DateTime()
      )
      
      // Créer un compte Bluesky
      val blueskyAccount = SocialAccount(
        name = "bluesky_test",
        identifier = "test_user.bsky.social",
        password = "test_password",
        providerType = ProviderType.Bluesky,
        createdAt = new DateTime()
      )
      
      // Ajouter les comptes
      accountService.createTwitterAccount(TwitterAccountForm.fromAccount(twitterAccount))
      accountService.createBlueskyAccount(BlueskyAccountForm.fromAccount(blueskyAccount))
      
      // Vérifier que les comptes sont bien créés
      val accounts = accountService.getAllAccounts
      accounts must contain(twitterAccount)
      accounts must contain(blueskyAccount)
      
      // Simuler des messages des deux providers
      val tweet = SocialMediaMessage(
        id = "123456",
        providerType = ProviderType.Twitter,
        content = "Test tweet",
        authorId = "test_user",
        createdAt = new DateTime(),
        metadata = Map(
          "source" -> "Twitter for iPhone",
          "lang" -> "fr"
        )
      )
      
      val post = SocialMediaMessage(
        id = "789012",
        providerType = ProviderType.Bluesky,
        content = "Test post",
        authorId = "test_user",
        createdAt = new DateTime(),
        metadata = Map(
          "uri" -> "at://test_user.bsky.social/app.bsky.feed.post/789012",
          "cid" -> "test_cid",
          "replyCount" -> 0,
          "repostCount" -> 0,
          "likeCount" -> 0
        )
      )
      
      // Vérifier que les messages sont correctement traités
      val twitterConnection = providerManager.getConnection(twitterAccount)
      val blueskyConnection = providerManager.getConnection(blueskyAccount)
      
      twitterConnection.asInstanceOf[TwitterApiMock].simulateTweetReceived(tweet)
      blueskyConnection.asInstanceOf[BlueskyApiMock].simulatePostReceived(post)
      
      twitterConnection.asInstanceOf[TwitterApiMock].getTestTweets must contain(tweet)
      blueskyConnection.asInstanceOf[BlueskyApiMock].getTestPosts must contain(post)
    }
    
    "maintain backward compatibility with Twitter API" in {
      val providerManager = new ProviderManager()
      val accountService = new AccountService(providerManager)
      
      // Créer un compte Twitter avec l'ancien format
      val oldTwitterAccount = SocialAccount(
        name = "old_twitter_test",
        identifier = "old_test_user",
        password = "test_password",
        providerType = ProviderType.Twitter,
        createdAt = new DateTime()
      )
      
      // Vérifier que l'ancien format est toujours supporté
      val connection = providerManager.createConnection(oldTwitterAccount)
      connection mustBe a[TwitterApiMock]
      connection.validateCredentials() mustBe true
      
      // Vérifier que les anciens tweets sont toujours correctement traités
      val oldTweet = SocialMediaMessage(
        id = "old_123456",
        providerType = ProviderType.Twitter,
        content = "Old test tweet",
        authorId = "old_test_user",
        createdAt = new DateTime(),
        metadata = Map(
          "source" -> "Twitter for iPhone",
          "lang" -> "fr"
        )
      )
      
      connection.asInstanceOf[TwitterApiMock].simulateTweetReceived(oldTweet)
      connection.asInstanceOf[TwitterApiMock].getTestTweets must contain(oldTweet)
    }
  }
} 