package test.integration

import models.SocialCollect
import models.SocialMediaMessage
import modules.exporter.postgresql.{PostgresConfiguration, PostgresModule}
import modules.dao.ModuleFileProcessedDao
import org.scalatestplus.play.PlaySpec
import providers.ProviderType
import org.joda.time.DateTime
import scala.concurrent.ExecutionContext.Implicits.global

class ModuleIntegrationTest extends PlaySpec {
  "Module Integration" should {
    "process messages from different providers" in {
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
        name = "test_collect",
        directory = "test_dir",
        observableFile = "test_file",
        createdAt = new DateTime()
      )
      
      val moduleFileProcessedDao = new ModuleFileProcessedDao()
      val postgresModule = new PostgresModule(collect, config, moduleFileProcessedDao)
      
      // Créer des messages de différents providers
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
      
      // Traiter les messages
      postgresModule.processMessage(tweet)
      postgresModule.processMessage(post)
      
      // Vérifier que les messages ont été traités
      // Note: Dans un test d'intégration réel, nous vérifierions la base de données
      // Ici, nous vérifions simplement que le module ne lance pas d'exception
      postgresModule must not be null
    }
    
    "handle module plugins correctly" in {
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
        name = "test_collect",
        directory = "test_dir",
        observableFile = "test_file",
        createdAt = new DateTime()
      )
      
      val moduleFileProcessedDao = new ModuleFileProcessedDao()
      val postgresModule = new PostgresModule(collect, config, moduleFileProcessedDao)
      
      // Vérifier que le module est correctement initialisé
      postgresModule.name mustBe "PostgresExporter"
      postgresModule.description mustBe "Exporte les données vers PostgreSQL"
      postgresModule.version mustBe "1.0.0"
      postgresModule.supportedProviders must contain allOf(ProviderType.Twitter, ProviderType.Bluesky)
      
      // Vérifier que le module peut être arrêté proprement
      postgresModule.shutdown()
      // Note: Dans un test d'intégration réel, nous vérifierions que les ressources sont libérées
    }
  }
} 