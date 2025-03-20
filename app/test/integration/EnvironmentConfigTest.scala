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
import java.io.{File, FileWriter}

class EnvironmentConfigTest extends PlaySpec {
  "Environment Configuration" should {
    "load test environment configuration" in {
      val config = TestConfig.getConfig("test")
      
      config.name mustBe "Garuda Test"
      config.environment mustBe "test"
      config.secret mustBe "test_secret"
      config.languages must contain("fr")
      config.timezone mustBe "Europe/Paris"
      
      val dbConfig = config.database
      dbConfig.host mustBe "localhost"
      dbConfig.port mustBe 5432
      dbConfig.base mustBe "test_db"
      dbConfig.user mustBe "test_user"
      dbConfig.password mustBe "test_password"
      dbConfig.schema mustBe "test_schema"
    }
    
    "load staging environment configuration" in {
      val config = TestConfig.getConfig("staging")
      
      config.name mustBe "Garuda Staging"
      config.environment mustBe "staging"
      config.secret mustBe "staging_secret"
      config.languages must contain("fr")
      config.timezone mustBe "Europe/Paris"
      
      val dbConfig = config.database
      dbConfig.host mustBe "staging-db"
      dbConfig.port mustBe 5432
      dbConfig.base mustBe "staging_db"
      dbConfig.user mustBe "staging_user"
      dbConfig.password mustBe "staging_password"
      dbConfig.schema mustBe "staging_schema"
    }
    
    "load production environment configuration" in {
      val config = TestConfig.getConfig("prod")
      
      config.name mustBe "Garuda"
      config.environment mustBe "prod"
      config.secret mustBe "prod_secret"
      config.languages must contain("fr")
      config.timezone mustBe "Europe/Paris"
      
      val dbConfig = config.database
      dbConfig.host mustBe "prod-db"
      dbConfig.port mustBe 5432
      dbConfig.base mustBe "prod_db"
      dbConfig.user mustBe "prod_user"
      dbConfig.password mustBe "prod_password"
      dbConfig.schema mustBe "prod_schema"
    }
    
    "handle configuration file changes" in {
      // Créer un fichier de configuration temporaire
      val tempConfigFile = File.createTempFile("test_config", ".conf")
      val writer = new FileWriter(tempConfigFile)
      
      try {
        // Écrire une configuration de test
        writer.write("""
          application {
            name = "Garuda Test"
            environment = "test"
            secret = "test_secret"
            languages = ["fr"]
            timezone = "Europe/Paris"
          }
          
          database {
            host = "localhost"
            port = 5432
            base = "test_db"
            user = "test_user"
            password = "test_password"
            schema = "test_schema"
          }
        """)
        
        writer.close()
        
        // Vérifier que le fichier existe et n'est pas vide
        tempConfigFile.exists() mustBe true
        tempConfigFile.length() must be > 0
        
        // Créer une instance de collect avec la configuration
        val collect = SocialCollect(
          name = "config_test",
          directory = "test_dir",
          observableFile = "test_file",
          createdAt = new DateTime()
        )
        
        val moduleFileProcessedDao = new ModuleFileProcessedDao()
        val postgresModule = new PostgresModule(collect, TestConfig.testDbConfig, moduleFileProcessedDao)
        
        // Vérifier que le module a été créé avec succès
        postgresModule must not be null
        
        // Nettoyer
        postgresModule.shutdown()
        
      } finally {
        // Supprimer le fichier temporaire
        tempConfigFile.delete()
      }
    }
    
    "handle invalid environment configuration" in {
      // Tenter de charger une configuration pour un environnement invalide
      try {
        TestConfig.getConfig("invalid_env")
        fail("Devrait avoir échoué avec un environnement invalide")
      } catch {
        case e: Exception =>
          e.getMessage must include("invalid_env")
      }
    }
    
    "handle database configuration changes" in {
      val collect = SocialCollect(
        name = "db_config_test",
        directory = "test_dir",
        observableFile = "test_file",
        createdAt = new DateTime()
      )
      
      // Tester avec la configuration de test
      val testConfig = TestConfig.testDbConfig
      val moduleFileProcessedDao = new ModuleFileProcessedDao()
      val testModule = new PostgresModule(collect, testConfig, moduleFileProcessedDao)
      
      testModule must not be null
      testModule.shutdown()
      
      // Tester avec la configuration de staging
      val stagingConfig = TestConfig.stagingDbConfig
      val stagingModule = new PostgresModule(collect, stagingConfig, moduleFileProcessedDao)
      
      stagingModule must not be null
      stagingModule.shutdown()
      
      // Tester avec la configuration de production
      val prodConfig = TestConfig.prodDbConfig
      val prodModule = new PostgresModule(collect, prodConfig, moduleFileProcessedDao)
      
      prodModule must not be null
      prodModule.shutdown()
    }
  }
} 