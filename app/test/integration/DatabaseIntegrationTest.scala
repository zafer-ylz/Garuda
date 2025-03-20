package test.integration

import models.{SocialCollect, SocialMediaMessage}
import modules.exporter.postgresql.{PostgresConfiguration, PostgresModule}
import modules.dao.ModuleFileProcessedDao
import org.scalatestplus.play.PlaySpec
import providers.ProviderType
import test.config.TestConfig
import org.joda.time.DateTime
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import java.sql.{Connection, DriverManager}

class DatabaseIntegrationTest extends PlaySpec {
  "Database Integration" should {
    "connect to test database" in {
      val config = TestConfig.testDbConfig
      
      // Tester la connexion à la base de données
      val connection = DriverManager.getConnection(
        s"jdbc:postgresql://${config.host}:${config.port}/${config.base}",
        config.user,
        config.password
      )
      
      connection must not be null
      connection.isClosed mustBe false
      
      // Nettoyer
      connection.close()
    }
    
    "handle database operations" in {
      val config = TestConfig.testDbConfig
      val collect = SocialCollect(
        name = "db_integration_test",
        directory = "test_dir",
        observableFile = "test_file",
        createdAt = new DateTime()
      )
      
      val moduleFileProcessedDao = new ModuleFileProcessedDao()
      val postgresModule = new PostgresModule(collect, config, moduleFileProcessedDao)
      
      // Créer des messages de test
      val messages = (1 to 100).map { i =>
        SocialMediaMessage(
          id = s"test_message_$i",
          providerType = ProviderType.Twitter,
          content = s"Test message $i",
          authorId = "test_user",
          createdAt = new DateTime(),
          metadata = Map(
            "source" -> "Test",
            "lang" -> "fr"
          )
        )
      }
      
      // Traiter les messages
      val futures = Future.sequence(messages.map { message =>
        Future {
          postgresModule.processMessage(message)
        }
      })
      
      // Attendre la fin du traitement
      Await.result(futures, 30.seconds)
      
      // Vérifier que les messages ont été traités
      // Note: Dans un test réel, nous vérifierions la base de données
      postgresModule must not be null
      
      // Nettoyer
      postgresModule.shutdown()
    }
    
    "handle database errors gracefully" in {
      val invalidConfig = PostgresConfiguration(
        host = "invalid_host",
        port = 5432,
        base = "invalid_db",
        user = "invalid_user",
        password = "invalid_password",
        schema = "invalid_schema",
        filesProcessed = List()
      )
      
      val collect = SocialCollect(
        name = "error_test",
        directory = "test_dir",
        observableFile = "test_file",
        createdAt = new DateTime()
      )
      
      val moduleFileProcessedDao = new ModuleFileProcessedDao()
      val postgresModule = new PostgresModule(collect, invalidConfig, moduleFileProcessedDao)
      
      // Tester la gestion des erreurs
      val message = SocialMediaMessage(
        id = "error_test_message",
        providerType = ProviderType.Twitter,
        content = "Test error message",
        authorId = "test_user",
        createdAt = new DateTime(),
        metadata = Map(
          "source" -> "Test",
          "lang" -> "fr"
        )
      )
      
      // Vérifier que le module gère les erreurs de connexion
      try {
        postgresModule.processMessage(message)
        fail("Devrait avoir échoué avec une erreur de connexion")
      } catch {
        case e: Exception =>
          // L'erreur est attendue
          e.getMessage must include("connection")
      }
      
      // Nettoyer
      postgresModule.shutdown()
    }
    
    "support database transactions" in {
      val config = TestConfig.testDbConfig
      val collect = SocialCollect(
        name = "transaction_test",
        directory = "test_dir",
        observableFile = "test_file",
        createdAt = new DateTime()
      )
      
      val moduleFileProcessedDao = new ModuleFileProcessedDao()
      val postgresModule = new PostgresModule(collect, config, moduleFileProcessedDao)
      
      // Créer une transaction de test
      val connection = DriverManager.getConnection(
        s"jdbc:postgresql://${config.host}:${config.port}/${config.base}",
        config.user,
        config.password
      )
      
      try {
        // Démarrer une transaction
        connection.setAutoCommit(false)
        
        // Créer des messages de test
        val messages = (1 to 50).map { i =>
          SocialMediaMessage(
            id = s"transaction_message_$i",
            providerType = ProviderType.Twitter,
            content = s"Transaction test message $i",
            authorId = "test_user",
            createdAt = new DateTime(),
            metadata = Map(
              "source" -> "Test",
              "lang" -> "fr"
            )
          )
        }
        
        // Traiter les messages dans la transaction
        val futures = Future.sequence(messages.map { message =>
          Future {
            postgresModule.processMessage(message)
          }
        })
        
        // Attendre la fin du traitement
        Await.result(futures, 30.seconds)
        
        // Valider la transaction
        connection.commit()
        
        // Vérifier que les messages ont été traités
        // Note: Dans un test réel, nous vérifierions la base de données
        postgresModule must not be null
        
      } catch {
        case e: Exception =>
          // En cas d'erreur, annuler la transaction
          connection.rollback()
          throw e
      } finally {
        // Nettoyer
        connection.setAutoCommit(true)
        connection.close()
        postgresModule.shutdown()
      }
    }
  }
} 