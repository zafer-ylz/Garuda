package test.performance

import models.{SocialCollect, SocialMediaMessage}
import modules.{Module, MessageAdapter}
import modules.exporter.postgresql.{PostgresConfiguration, PostgresModule}
import modules.dao.ModuleFileProcessedDao
import org.scalatestplus.play.PlaySpec
import providers.ProviderType
import org.joda.time.DateTime
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class ModulePerformanceTest extends PlaySpec {
  "Module Performance" should {
    "handle high message throughput" in {
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
        name = "perf_test_collect",
        directory = "test_dir",
        observableFile = "test_file",
        createdAt = new DateTime()
      )
      
      val moduleFileProcessedDao = new ModuleFileProcessedDao()
      val postgresModule = new PostgresModule(collect, config, moduleFileProcessedDao)
      
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
          
          postgresModule.processMessage(tweet)
        }
      })
      
      // Attendre la fin du traitement
      Await.result(futures, 30.seconds)
      
      val endTime = System.currentTimeMillis()
      val duration = endTime - startTime
      val messagesPerSecond = messageCount * 1000 / duration
      
      println(s"Module performance: $messagesPerSecond messages/sec")
      println(s"Total duration: ${duration}ms for $messageCount messages")
      
      // Vérifier que la performance est acceptable
      messagesPerSecond must be > 0
      
      // Arrêter le module
      postgresModule.shutdown()
    }
    
    "perform well with mixed message types" in {
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
        name = "mixed_perf_test",
        directory = "test_dir",
        observableFile = "test_file",
        createdAt = new DateTime()
      )
      
      val moduleFileProcessedDao = new ModuleFileProcessedDao()
      val postgresModule = new PostgresModule(collect, config, moduleFileProcessedDao)
      
      // Générer un nombre égal de messages Twitter et Bluesky
      val messageCount = 5000
      val startTime = System.currentTimeMillis()
      
      // Créer une liste de futurs pour les messages Twitter
      val twitterFutures = (1 to messageCount).map { i =>
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
          
          postgresModule.processMessage(tweet)
        }
      }
      
      // Créer une liste de futurs pour les messages Bluesky
      val blueskyFutures = (1 to messageCount).map { i =>
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
          
          postgresModule.processMessage(post)
        }
      }
      
      // Combiner les futurs et attendre la fin
      val allFutures = twitterFutures ++ blueskyFutures
      Await.result(Future.sequence(allFutures), 30.seconds)
      
      val endTime = System.currentTimeMillis()
      val duration = endTime - startTime
      val messagesPerSecond = messageCount * 2 * 1000 / duration
      
      println(s"Mixed message performance: $messagesPerSecond messages/sec")
      println(s"Total duration: ${duration}ms for ${messageCount * 2} messages")
      
      // Vérifier que la performance est acceptable
      messagesPerSecond must be > 0
      
      // Arrêter le module
      postgresModule.shutdown()
    }
    
    "maintain performance under concurrent module processing" in {
      // Créer plusieurs instances de modules avec des configurations différentes
      val moduleCount = 5
      val modules = (1 to moduleCount).map { i =>
        val config = PostgresConfiguration(
          host = "localhost",
          port = 5432,
          base = s"test_db_$i",
          user = "test_user",
          password = "test_password",
          schema = s"test_schema_$i",
          filesProcessed = List()
        )
        
        val collect = SocialCollect(
          name = s"concurrent_test_$i",
          directory = "test_dir",
          observableFile = s"test_file_$i",
          createdAt = new DateTime()
        )
        
        val moduleFileProcessedDao = new ModuleFileProcessedDao()
        new PostgresModule(collect, config, moduleFileProcessedDao)
      }
      
      // Générer des messages pour chaque module
      val messageCount = 2000
      val startTime = System.currentTimeMillis()
      
      // Pour chaque module, créer une liste de futures
      val moduleFutures = modules.map { module =>
        Future.sequence((1 to messageCount).map { i =>
          Future {
            val random = new Random()
            val providerType = if (random.nextBoolean()) ProviderType.Twitter else ProviderType.Bluesky
            
            val message = if (providerType == ProviderType.Twitter) {
              SocialMediaMessage(
                id = s"tweet_${module.name}_$i",
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
                id = s"post_${module.name}_$i",
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
            
            module.processMessage(message)
          }
        })
      }
      
      // Attendre la fin de tous les traitements
      Await.result(Future.sequence(moduleFutures), 60.seconds)
      
      val endTime = System.currentTimeMillis()
      val duration = endTime - startTime
      val totalMessages = messageCount * moduleCount
      val messagesPerSecond = totalMessages * 1000 / duration
      
      println(s"Concurrent module performance: $messagesPerSecond messages/sec with $moduleCount modules")
      println(s"Total duration: ${duration}ms for $totalMessages messages")
      
      // Vérifier que la performance est acceptable
      messagesPerSecond must be > 0
      
      // Arrêter tous les modules
      modules.foreach(_.shutdown())
    }
  }
} 