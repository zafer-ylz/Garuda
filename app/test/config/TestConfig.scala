package test.config

import modules.exporter.postgresql.PostgresConfiguration
import play.api.Configuration
import play.api.Environment
import play.api.Mode

object TestConfig {
  // Configuration de base de données pour l'environnement de test
  val testDbConfig = PostgresConfiguration(
    host = "localhost",
    port = 5432,
    base = "test_db",
    user = "test_user",
    password = "test_password",
    schema = "test_schema",
    filesProcessed = List()
  )
  
  // Configuration de base de données pour l'environnement de staging
  val stagingDbConfig = PostgresConfiguration(
    host = "staging-db",
    port = 5432,
    base = "staging_db",
    user = "staging_user",
    password = "staging_password",
    schema = "staging_schema",
    filesProcessed = List()
  )
  
  // Configuration de base de données pour l'environnement de production
  val prodDbConfig = PostgresConfiguration(
    host = "prod-db",
    port = 5432,
    base = "prod_db",
    user = "prod_user",
    password = "prod_password",
    schema = "prod_schema",
    filesProcessed = List()
  )
  
  // Configuration de l'application pour l'environnement de test
  val testAppConfig = Configuration.from(Map(
    "application.name" -> "Garuda Test",
    "application.environment" -> "test",
    "application.secret" -> "test_secret",
    "application.langs" -> List("fr"),
    "application.timezone" -> "Europe/Paris",
    
    "database.default.host" -> "localhost",
    "database.default.port" -> 5432,
    "database.default.name" -> "test_db",
    "database.default.user" -> "test_user",
    "database.default.password" -> "test_password",
    
    "providers.twitter.enabled" -> true,
    "providers.twitter.api.key" -> "test_twitter_key",
    "providers.twitter.api.secret" -> "test_twitter_secret",
    
    "providers.bluesky.enabled" -> true,
    "providers.bluesky.api.url" -> "https://bsky.social",
    
    "modules.postgres.enabled" -> true,
    "modules.postgres.batch.size" -> 100,
    "modules.postgres.flush.interval" -> "5s"
  ))
  
  // Configuration de l'application pour l'environnement de staging
  val stagingAppConfig = Configuration.from(Map(
    "application.name" -> "Garuda Staging",
    "application.environment" -> "staging",
    "application.secret" -> "staging_secret",
    "application.langs" -> List("fr"),
    "application.timezone" -> "Europe/Paris",
    
    "database.default.host" -> "staging-db",
    "database.default.port" -> 5432,
    "database.default.name" -> "staging_db",
    "database.default.user" -> "staging_user",
    "database.default.password" -> "staging_password",
    
    "providers.twitter.enabled" -> true,
    "providers.twitter.api.key" -> "staging_twitter_key",
    "providers.twitter.api.secret" -> "staging_twitter_secret",
    
    "providers.bluesky.enabled" -> true,
    "providers.bluesky.api.url" -> "https://bsky.social",
    
    "modules.postgres.enabled" -> true,
    "modules.postgres.batch.size" -> 100,
    "modules.postgres.flush.interval" -> "5s"
  ))
  
  // Configuration de l'application pour l'environnement de production
  val prodAppConfig = Configuration.from(Map(
    "application.name" -> "Garuda",
    "application.environment" -> "prod",
    "application.secret" -> "prod_secret",
    "application.langs" -> List("fr"),
    "application.timezone" -> "Europe/Paris",
    
    "database.default.host" -> "prod-db",
    "database.default.port" -> 5432,
    "database.default.name" -> "prod_db",
    "database.default.user" -> "prod_user",
    "database.default.password" -> "prod_password",
    
    "providers.twitter.enabled" -> true,
    "providers.twitter.api.key" -> "prod_twitter_key",
    "providers.twitter.api.secret" -> "prod_twitter_secret",
    
    "providers.bluesky.enabled" -> true,
    "providers.bluesky.api.url" -> "https://bsky.social",
    
    "modules.postgres.enabled" -> true,
    "modules.postgres.batch.size" -> 100,
    "modules.postgres.flush.interval" -> "5s"
  ))
  
  // Environnement de test
  val testEnv = Environment(new java.io.File("."), getClass.getClassLoader, Mode.Test)
  
  // Environnement de staging
  val stagingEnv = Environment(new java.io.File("."), getClass.getClassLoader, Mode.Dev)
  
  // Environnement de production
  val prodEnv = Environment(new java.io.File("."), getClass.getClassLoader, Mode.Prod)
  
  // Fonction utilitaire pour obtenir la configuration en fonction de l'environnement
  def getConfig(env: String): Configuration = env match {
    case "test" => testAppConfig
    case "staging" => stagingAppConfig
    case "prod" => prodAppConfig
    case _ => throw new IllegalArgumentException(s"Environnement non supporté: $env")
  }
  
  // Fonction utilitaire pour obtenir la configuration de base de données en fonction de l'environnement
  def getDbConfig(env: String): PostgresConfiguration = env match {
    case "test" => testDbConfig
    case "staging" => stagingDbConfig
    case "prod" => prodDbConfig
    case _ => throw new IllegalArgumentException(s"Environnement non supporté: $env")
  }
} 