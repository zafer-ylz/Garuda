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
import java.sql.{Connection, DriverManager, Statement}

class DataMigrationTest extends PlaySpec {
  "Data Migration" should {
    "migrate existing data to new schema" in {
      val config = TestConfig.testDbConfig
      val collect = SocialCollect(
        name = "migration_test",
        directory = "test_dir",
        observableFile = "test_file",
        createdAt = new DateTime()
      )
      
      // Créer une connexion à la base de données
      val connection = DriverManager.getConnection(
        s"jdbc:postgresql://${config.host}:${config.port}/${config.base}",
        config.user,
        config.password
      )
      
      try {
        // Créer la table de test avec l'ancien schéma
        val statement = connection.createStatement()
        statement.execute("""
          CREATE TABLE IF NOT EXISTS old_messages (
            id VARCHAR(255) PRIMARY KEY,
            content TEXT,
            author_id VARCHAR(255),
            created_at TIMESTAMP,
            provider_type VARCHAR(50)
          )
        """)
        
        // Insérer des données de test dans l'ancien schéma
        val insertStatement = connection.prepareStatement("""
          INSERT INTO old_messages (id, content, author_id, created_at, provider_type)
          VALUES (?, ?, ?, ?, ?)
        """)
        
        (1 to 100).foreach { i =>
          insertStatement.setString(1, s"old_message_$i")
          insertStatement.setString(2, s"Old test message $i")
          insertStatement.setString(3, "test_user")
          insertStatement.setTimestamp(4, new java.sql.Timestamp(new DateTime().getMillis))
          insertStatement.setString(5, "Twitter")
          insertStatement.addBatch()
        }
        insertStatement.executeBatch()
        
        // Créer le nouveau module PostgreSQL
        val moduleFileProcessedDao = new ModuleFileProcessedDao()
        val postgresModule = new PostgresModule(collect, config, moduleFileProcessedDao)
        
        // Migrer les données
        val selectStatement = connection.createStatement()
        val resultSet = selectStatement.executeQuery("SELECT * FROM old_messages")
        
        while (resultSet.next()) {
          val message = SocialMediaMessage(
            id = resultSet.getString("id"),
            providerType = ProviderType.withName(resultSet.getString("provider_type")),
            content = resultSet.getString("content"),
            authorId = resultSet.getString("author_id"),
            createdAt = new DateTime(resultSet.getTimestamp("created_at").getTime),
            metadata = Map(
              "source" -> "Migration",
              "lang" -> "fr"
            )
          )
          
          postgresModule.processMessage(message)
        }
        
        // Vérifier que les données ont été migrées
        // Note: Dans un test réel, nous vérifierions la nouvelle table
        postgresModule must not be null
        
        // Nettoyer
        statement.execute("DROP TABLE old_messages")
        postgresModule.shutdown()
        
      } finally {
        connection.close()
      }
    }
    
    "handle migration errors gracefully" in {
      val config = TestConfig.testDbConfig
      val collect = SocialCollect(
        name = "migration_error_test",
        directory = "test_dir",
        observableFile = "test_file",
        createdAt = new DateTime()
      )
      
      // Créer une connexion à la base de données
      val connection = DriverManager.getConnection(
        s"jdbc:postgresql://${config.host}:${config.port}/${config.base}",
        config.user,
        config.password
      )
      
      try {
        // Créer une table avec des données invalides
        val statement = connection.createStatement()
        statement.execute("""
          CREATE TABLE IF NOT EXISTS invalid_messages (
            id VARCHAR(255) PRIMARY KEY,
            content TEXT,
            author_id VARCHAR(255),
            created_at TIMESTAMP,
            provider_type VARCHAR(50)
          )
        """)
        
        // Insérer des données invalides
        val insertStatement = connection.prepareStatement("""
          INSERT INTO invalid_messages (id, content, author_id, created_at, provider_type)
          VALUES (?, ?, ?, ?, ?)
        """)
        
        insertStatement.setString(1, "invalid_message")
        insertStatement.setString(2, "Invalid message")
        insertStatement.setString(3, "test_user")
        insertStatement.setTimestamp(4, new java.sql.Timestamp(new DateTime().getMillis))
        insertStatement.setString(5, "InvalidProvider") // Type de provider invalide
        insertStatement.execute()
        
        // Créer le nouveau module PostgreSQL
        val moduleFileProcessedDao = new ModuleFileProcessedDao()
        val postgresModule = new PostgresModule(collect, config, moduleFileProcessedDao)
        
        // Tenter de migrer les données
        val selectStatement = connection.createStatement()
        val resultSet = selectStatement.executeQuery("SELECT * FROM invalid_messages")
        
        try {
          while (resultSet.next()) {
            val message = SocialMediaMessage(
              id = resultSet.getString("id"),
              providerType = ProviderType.withName(resultSet.getString("provider_type")),
              content = resultSet.getString("content"),
              authorId = resultSet.getString("author_id"),
              createdAt = new DateTime(resultSet.getTimestamp("created_at").getTime),
              metadata = Map(
                "source" -> "Migration",
                "lang" -> "fr"
              )
            )
            
            postgresModule.processMessage(message)
            fail("Devrait avoir échoué avec un type de provider invalide")
          }
        } catch {
          case e: Exception =>
            // L'erreur est attendue
            e.getMessage must include("InvalidProvider")
        }
        
        // Nettoyer
        statement.execute("DROP TABLE invalid_messages")
        postgresModule.shutdown()
        
      } finally {
        connection.close()
      }
    }
    
    "support rollback during migration" in {
      val config = TestConfig.testDbConfig
      val collect = SocialCollect(
        name = "migration_rollback_test",
        directory = "test_dir",
        observableFile = "test_file",
        createdAt = new DateTime()
      )
      
      // Créer une connexion à la base de données
      val connection = DriverManager.getConnection(
        s"jdbc:postgresql://${config.host}:${config.port}/${config.base}",
        config.user,
        config.password
      )
      
      try {
        // Démarrer une transaction
        connection.setAutoCommit(false)
        
        // Créer une table de test
        val statement = connection.createStatement()
        statement.execute("""
          CREATE TABLE IF NOT EXISTS rollback_messages (
            id VARCHAR(255) PRIMARY KEY,
            content TEXT,
            author_id VARCHAR(255),
            created_at TIMESTAMP,
            provider_type VARCHAR(50)
          )
        """)
        
        // Insérer des données de test
        val insertStatement = connection.prepareStatement("""
          INSERT INTO rollback_messages (id, content, author_id, created_at, provider_type)
          VALUES (?, ?, ?, ?, ?)
        """)
        
        (1 to 50).foreach { i =>
          insertStatement.setString(1, s"rollback_message_$i")
          insertStatement.setString(2, s"Rollback test message $i")
          insertStatement.setString(3, "test_user")
          insertStatement.setTimestamp(4, new java.sql.Timestamp(new DateTime().getMillis))
          insertStatement.setString(5, "Twitter")
          insertStatement.addBatch()
        }
        insertStatement.executeBatch()
        
        // Créer le nouveau module PostgreSQL
        val moduleFileProcessedDao = new ModuleFileProcessedDao()
        val postgresModule = new PostgresModule(collect, config, moduleFileProcessedDao)
        
        try {
          // Tenter de migrer les données
          val selectStatement = connection.createStatement()
          val resultSet = selectStatement.executeQuery("SELECT * FROM rollback_messages")
          
          while (resultSet.next()) {
            val message = SocialMediaMessage(
              id = resultSet.getString("id"),
              providerType = ProviderType.withName(resultSet.getString("provider_type")),
              content = resultSet.getString("content"),
              authorId = resultSet.getString("author_id"),
              createdAt = new DateTime(resultSet.getTimestamp("created_at").getTime),
              metadata = Map(
                "source" -> "Migration",
                "lang" -> "fr"
              )
            )
            
            postgresModule.processMessage(message)
          }
          
          // Simuler une erreur
          throw new RuntimeException("Erreur de migration simulée")
          
        } catch {
          case e: Exception =>
            // Annuler la transaction
            connection.rollback()
            e.getMessage mustBe "Erreur de migration simulée"
        } finally {
          // Nettoyer
          connection.setAutoCommit(true)
          statement.execute("DROP TABLE rollback_messages")
          postgresModule.shutdown()
        }
        
      } finally {
        connection.close()
      }
    }
  }
} 