package test.unit.providers

import models.SocialAccount
import models.SocialMediaMessage
import org.scalatestplus.play.PlaySpec
import providers.ProviderType
import test.mocks.BlueskyApiMock
import org.joda.time.DateTime

class BlueskyProviderTest extends PlaySpec {
  "BlueskyProvider" should {
    "create a valid Bluesky account" in {
      val provider = new BlueskyProvider()
      val account = SocialAccount(
        name = "test_account",
        identifier = "test_user.bsky.social",
        password = "test_password",
        providerType = ProviderType.Bluesky,
        createdAt = new DateTime()
      )
      
      val connection = provider.createConnection(account)
      connection mustBe a[BlueskyApiMock]
      connection.validateCredentials() mustBe true
    }
    
    "validate credentials correctly" in {
      val provider = new BlueskyProvider()
      val account = SocialAccount(
        name = "test_account",
        identifier = "test_user.bsky.social",
        password = "test_password",
        providerType = ProviderType.Bluesky,
        createdAt = new DateTime()
      )
      
      val connection = provider.createConnection(account)
      provider.validateCredentials(account) mustBe true
    }
    
    "handle post processing" in {
      val provider = new BlueskyProvider()
      val account = SocialAccount(
        name = "test_account",
        identifier = "test_user.bsky.social",
        password = "test_password",
        providerType = ProviderType.Bluesky,
        createdAt = new DateTime()
      )
      
      val connection = provider.createConnection(account)
      val post = SocialMediaMessage(
        id = "123456",
        providerType = ProviderType.Bluesky,
        content = "Test post",
        authorId = "test_user",
        createdAt = new DateTime(),
        metadata = Map(
          "uri" -> "at://test_user.bsky.social/app.bsky.feed.post/123456",
          "cid" -> "test_cid",
          "replyCount" -> 0,
          "repostCount" -> 0,
          "likeCount" -> 0
        )
      )
      
      connection.asInstanceOf[BlueskyApiMock].simulatePostReceived(post)
      connection.asInstanceOf[BlueskyApiMock].getTestPosts must contain(post)
    }
  }
} 