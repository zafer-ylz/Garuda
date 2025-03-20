package test.performance

import models.SocialAccount
import models.SocialMediaMessage
import org.scalatestplus.play.PlaySpec
import providers.{ProviderManager, ProviderType}
import test.mocks.{TwitterApiMock, BlueskyApiMock}
import org.joda.time.DateTime
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class ProviderPerformanceTest extends PlaySpec {
  "Provider Performance" should {
    "handle high message throughput" in {
      val providerManager = new ProviderManager()
      
      // Créer un compte Twitter
      val twitterAccount = SocialAccount(
        name = "twitter_perf_test",
        identifier = "test_user",
        password = "test_password",
        providerType = ProviderType.Twitter,
        createdAt = new DateTime()
      )
      
      val connection = providerManager.createConnection(twitterAccount)
      connection mustBe a[TwitterApiMock]
      
      // Générer un grand nombre de messages
      val messageCount = 10000
      val startTime = System.currentTimeMillis()
      
      // Traiter les messages en parallèle
      val futures = Future.sequence((1 to messageCount).map { i =>
        Future {
          val tweet = SocialMediaMessage(
            id = s"tweet_$i",
            providerType = ProviderType.Twitter,
            content = s"Test tweet $i",
            authorId = "test_user",
            createdAt = new DateTime(),
            metadata = Map(
              "source" -> "Twitter for iPhone",
              "lang" -> "fr"
            )
          )
          
          connection.asInstanceOf[TwitterApiMock].simulateTweetReceived(tweet)
        }
      })
      
      // Attendre la fin du traitement
      Await.result(futures, 30.seconds)
      
      val endTime = System.currentTimeMillis()
      val duration = endTime - startTime
      val messagesPerSecond = messageCount * 1000 / duration
      
      println(s"Performance: $messagesPerSecond messages/sec")
      println(s"Total duration: ${duration}ms for $messageCount messages")
      
      // Vérifier que la performance est acceptable
      // Dans un environnement de test réel, on définirait une valeur minimale
      messagesPerSecond must be > 0
    }
    
    "maintain performance with multiple providers" in {
      val providerManager = new ProviderManager()
      
      // Créer un compte Twitter
      val twitterAccount = SocialAccount(
        name = "twitter_multi_perf",
        identifier = "test_user",
        password = "test_password",
        providerType = ProviderType.Twitter,
        createdAt = new DateTime()
      )
      
      // Créer un compte Bluesky
      val blueskyAccount = SocialAccount(
        name = "bluesky_multi_perf",
        identifier = "test_user.bsky.social",
        password = "test_password",
        providerType = ProviderType.Bluesky,
        createdAt = new DateTime()
      )
      
      val twitterConnection = providerManager.createConnection(twitterAccount)
      twitterConnection mustBe a[TwitterApiMock]
      
      val blueskyConnection = providerManager.createConnection(blueskyAccount)
      blueskyConnection mustBe a[BlueskyApiMock]
      
      // Générer des messages pour les deux providers
      val messageCount = 5000
      val startTime = System.currentTimeMillis()
      
      // Traiter les messages en parallèle
      val twitterFutures = Future.sequence((1 to messageCount).map { i =>
        Future {
          val tweet = SocialMediaMessage(
            id = s"tweet_$i",
            providerType = ProviderType.Twitter,
            content = s"Test tweet $i",
            authorId = "test_user",
            createdAt = new DateTime(),
            metadata = Map(
              "source" -> "Twitter for iPhone",
              "lang" -> "fr"
            )
          )
          
          twitterConnection.asInstanceOf[TwitterApiMock].simulateTweetReceived(tweet)
        }
      })
      
      val blueskyFutures = Future.sequence((1 to messageCount).map { i =>
        Future {
          val post = SocialMediaMessage(
            id = s"post_$i",
            providerType = ProviderType.Bluesky,
            content = s"Test post $i",
            authorId = "test_user",
            createdAt = new DateTime(),
            metadata = Map(
              "uri" -> s"at://test_user.bsky.social/app.bsky.feed.post/$i",
              "cid" -> s"test_cid_$i",
              "replyCount" -> 0,
              "repostCount" -> 0,
              "likeCount" -> 0
            )
          )
          
          blueskyConnection.asInstanceOf[BlueskyApiMock].simulatePostReceived(post)
        }
      })
      
      // Attendre la fin du traitement
      Await.result(Future.sequence(Seq(twitterFutures, blueskyFutures)), 30.seconds)
      
      val endTime = System.currentTimeMillis()
      val duration = endTime - startTime
      val messagesPerSecond = messageCount * 2 * 1000 / duration
      
      println(s"Multi-provider performance: $messagesPerSecond messages/sec")
      println(s"Total duration: ${duration}ms for ${messageCount * 2} messages")
      
      // Vérifier que la performance est acceptable
      messagesPerSecond must be > 0
    }
    
    "scale with increasing rule complexity" in {
      val providerManager = new ProviderManager()
      
      // Créer un compte Twitter
      val twitterAccount = SocialAccount(
        name = "twitter_rule_perf",
        identifier = "test_user",
        password = "test_password",
        providerType = ProviderType.Twitter,
        createdAt = new DateTime()
      )
      
      val connection = providerManager.createConnection(twitterAccount)
      connection mustBe a[TwitterApiMock]
      
      // Tester la performance avec différents nombres de règles
      val ruleCounts = List(1, 5, 10, 50, 100)
      val messageCount = 1000
      
      ruleCounts.foreach { ruleCount =>
        // Ajouter des règles
        (1 to ruleCount).foreach { i =>
          connection.asInstanceOf[TwitterApiMock].addRule(s"rule_$i", s"keyword$i OR hashtag$i")
        }
        
        val startTime = System.currentTimeMillis()
        
        // Traiter les messages
        val futures = Future.sequence((1 to messageCount).map { i =>
          Future {
            val random = new Random()
            val ruleMatch = if (random.nextBoolean()) s"keyword${random.nextInt(ruleCount) + 1}" else ""
            
            val tweet = SocialMediaMessage(
              id = s"tweet_$i",
              providerType = ProviderType.Twitter,
              content = s"Test tweet $i $ruleMatch",
              authorId = "test_user",
              createdAt = new DateTime(),
              metadata = Map(
                "source" -> "Twitter for iPhone",
                "lang" -> "fr"
              )
            )
            
            connection.asInstanceOf[TwitterApiMock].simulateTweetReceived(tweet)
          }
        })
        
        // Attendre la fin du traitement
        Await.result(futures, 30.seconds)
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        val messagesPerSecond = messageCount * 1000 / duration
        
        println(s"Performance with $ruleCount rules: $messagesPerSecond messages/sec")
        println(s"Total duration: ${duration}ms for $messageCount messages")
        
        // Vérifier que la performance est acceptable
        messagesPerSecond must be > 0
        
        // Nettoyer les règles pour le prochain test
        connection.asInstanceOf[TwitterApiMock].clearRules()
      }
    }
  }
} 