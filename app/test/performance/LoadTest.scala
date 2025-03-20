package test.performance

import models.{SocialAccount, SocialCollect, SocialMediaMessage}
import modules.exporter.postgresql.{PostgresConfiguration, PostgresModule}
import modules.dao.ModuleFileProcessedDao
import org.scalatestplus.play.PlaySpec
import providers.{ProviderManager, ProviderType}
import test.mocks.{TwitterApiMock, BlueskyApiMock}
import org.joda.time.DateTime
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class LoadTest extends PlaySpec {
  "System Load" should {
    "handle sustained high load" in {
      val providerManager = new ProviderManager()
      val moduleFileProcessedDao = new ModuleFileProcessedDao()
      
      // Configuration du module PostgreSQL
      val config = PostgresConfiguration(
        host = "localhost",
        port = 5432,
        base = "test_db",
        user = "test_user",
        password = "test_password",
        schema = "test_schema",
        filesProcessed = List()
      )
      
      val collect = SocialCollect(
        name = "load_test_collect",
        directory = "test_dir",
        observableFile = "test_file",
        createdAt = new DateTime()
      )
      
      val postgresModule = new PostgresModule(collect, config, moduleFileProcessedDao)
      
      // Créer des comptes pour différents providers
      val twitterAccount = SocialAccount(
        name = "twitter_load_test",
        identifier = "test_user",
        password = "test_password",
        providerType = ProviderType.Twitter,
        createdAt = new DateTime()
      )
      
      val blueskyAccount = SocialAccount(
        name = "bluesky_load_test",
        identifier = "test_user.bsky.social",
        password = "test_password",
        providerType = ProviderType.Bluesky,
        createdAt = new DateTime()
      )
      
      val twitterConnection = providerManager.createConnection(twitterAccount)
      val blueskyConnection = providerManager.createConnection(blueskyAccount)
      
      // Test de charge soutenu sur 5 minutes
      val testDuration = 5.minutes
      val startTime = System.currentTimeMillis()
      val endTime = startTime + testDuration.toMillis
      
      var totalMessages = 0
      var lastReportTime = startTime
      
      while (System.currentTimeMillis() < endTime) {
        val currentTime = System.currentTimeMillis()
        
        // Générer et traiter des messages
        val messageBatch = 100
        val futures = Future.sequence((1 to messageBatch).map { i =>
          Future {
            val random = new Random()
            val providerType = if (random.nextBoolean()) ProviderType.Twitter else ProviderType.Bluesky
            
            val message = if (providerType == ProviderType.Twitter) {
              SocialMediaMessage(
                id = s"tweet_${currentTime}_$i",
                providerType = providerType,
                content = s"Test tweet $i",
                authorId = "test_user",
                createdAt = new DateTime(),
                metadata = Map(
                  "source" -> "Twitter for iPhone",
                  "lang" -> "fr"
                )
              )
            } else {
              SocialMediaMessage(
                id = s"post_${currentTime}_$i",
                providerType = providerType,
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
            }
            
            // Simuler la réception du message
            if (providerType == ProviderType.Twitter) {
              twitterConnection.asInstanceOf[TwitterApiMock].simulateTweetReceived(message)
            } else {
              blueskyConnection.asInstanceOf[BlueskyApiMock].simulatePostReceived(message)
            }
            
            // Traiter le message via le module
            postgresModule.processMessage(message)
          }
        })
        
        // Attendre la fin du traitement du batch
        Await.result(futures, 10.seconds)
        
        totalMessages += messageBatch
        
        // Afficher un rapport toutes les 30 secondes
        if (currentTime - lastReportTime >= 30000) {
          val elapsedSeconds = (currentTime - startTime) / 1000
          val messagesPerSecond = totalMessages / elapsedSeconds
          println(s"Load test progress: $messagesPerSecond messages/sec after $elapsedSeconds seconds")
          lastReportTime = currentTime
        }
      }
      
      val finalDuration = (System.currentTimeMillis() - startTime) / 1000
      val averageMessagesPerSecond = totalMessages / finalDuration
      
      println(s"Load test completed:")
      println(s"Total messages: $totalMessages")
      println(s"Total duration: $finalDuration seconds")
      println(s"Average throughput: $averageMessagesPerSecond messages/sec")
      
      // Vérifier que la performance est acceptable
      averageMessagesPerSecond must be > 0
      
      // Nettoyer
      postgresModule.shutdown()
    }
    
    "handle burst load" in {
      val providerManager = new ProviderManager()
      val moduleFileProcessedDao = new ModuleFileProcessedDao()
      
      // Configuration du module PostgreSQL
      val config = PostgresConfiguration(
        host = "localhost",
        port = 5432,
        base = "test_db",
        user = "test_user",
        password = "test_password",
        schema = "test_schema",
        filesProcessed = List()
      )
      
      val collect = SocialCollect(
        name = "burst_test_collect",
        directory = "test_dir",
        observableFile = "test_file",
        createdAt = new DateTime()
      )
      
      val postgresModule = new PostgresModule(collect, config, moduleFileProcessedDao)
      
      // Créer un compte Twitter
      val twitterAccount = SocialAccount(
        name = "twitter_burst_test",
        identifier = "test_user",
        password = "test_password",
        providerType = ProviderType.Twitter,
        createdAt = new DateTime()
      )
      
      val connection = providerManager.createConnection(twitterAccount)
      
      // Simuler une charge soudaine
      val burstSize = 50000
      val startTime = System.currentTimeMillis()
      
      // Générer et traiter un grand nombre de messages en une seule fois
      val futures = Future.sequence((1 to burstSize).map { i =>
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
          postgresModule.processMessage(tweet)
        }
      })
      
      // Attendre la fin du traitement
      Await.result(futures, 60.seconds)
      
      val duration = (System.currentTimeMillis() - startTime) / 1000
      val messagesPerSecond = burstSize / duration
      
      println(s"Burst test completed:")
      println(s"Total messages: $burstSize")
      println(s"Duration: $duration seconds")
      println(s"Throughput: $messagesPerSecond messages/sec")
      
      // Vérifier que la performance est acceptable
      messagesPerSecond must be > 0
      
      // Nettoyer
      postgresModule.shutdown()
    }
  }
} 