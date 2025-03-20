package modules.exporter.postgresql

import java.util.Properties

/**
 * Configuration pour la connexion PostgreSQL
 */
case class PostgresConfig(
  host: String,
  port: Int,
  database: String,
  user: String,
  password: String,
  schema: String
) {
  def getUrl: String = s"jdbc:postgresql://$host:$port/$database"
  
  def getProperties: Properties = {
    val props = new Properties()
    props.setProperty("user", user)
    props.setProperty("password", password)
    props
  }
} 