package modules.exporter.postgresql

import java.sql._
import models.SocialMediaMessage
import models.tweet.Tweet
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import org.joda.time.DateTime
import providers.ProviderType

/**
 * Classe pour insérer des messages dans PostgreSQL
 */
class PostgresInsertion(config: PostgresConfig) extends Module with Logging {
	private var connection: Connection = _
	private var preparedStatement: PreparedStatement = _
	
	// Initialiser la connexion
	try {
		Class.forName("org.postgresql.Driver")
		connection = DriverManager.getConnection(
			config.getUrl,
			config.user,
			config.password
		)
		
		// Créer le schéma si nécessaire
		val schemaStatement = connection.createStatement()
		schemaStatement.execute(s"CREATE SCHEMA IF NOT EXISTS ${config.schema}")
		schemaStatement.close()
		
		// Créer les tables si nécessaires
		val createTableStatement = connection.createStatement()
		createTableStatement.execute(
			s"""
				|CREATE TABLE IF NOT EXISTS ${config.schema}.messages (
				|  id TEXT PRIMARY KEY,
				|  provider_type TEXT NOT NULL,
				|  content TEXT NOT NULL,
				|  author_id TEXT NOT NULL,
				|  created_at TIMESTAMP NOT NULL,
				|  metadata JSONB,
				|  raw_data JSONB
				|)
				|""".stripMargin)
		createTableStatement.close()
		
		// Préparer la requête d'insertion
		preparedStatement = connection.prepareStatement(
			s"""
				|INSERT INTO ${config.schema}.messages 
				|(id, provider_type, content, author_id, created_at, metadata, raw_data)
				|VALUES (?, ?, ?, ?, ?, ?::jsonb, ?::jsonb)
				|ON CONFLICT (id) DO NOTHING
				|""".stripMargin)
	} catch {
		case e: Exception => 
			logger.error("Error initializing PostgreSQL connection", e)
			throw e
	}
	
	/**
	 * Insère une ligne brute JSON
	 */
	def insertLine(line: String): Unit = {
		try {
			val message = SocialMediaMessage.fromJson(line)
			processMessage(message, Some(line))
		} catch {
			case e: Exception =>
				logger.error(s"Error processing line: $line", e)
		}
	}
	
	/**
	 * Traite un message pour insertion
	 */
	override def processMessage(message: SocialMediaMessage): Unit = {
		processMessage(message, None)
	}
	
	/**
	 * Insère un message Bluesky
	 */
	def insertBlueskyMessage(message: SocialMediaMessage): Unit = {
		processMessage(message, None)
	}
	
	/**
	 * Implémentation interne du traitement des messages
	 */
	private def processMessage(message: SocialMediaMessage, rawData: Option[String]): Unit = {
		try {
			preparedStatement.setString(1, message.id)
			preparedStatement.setString(2, message.providerType.toString)
			preparedStatement.setString(3, message.content)
			preparedStatement.setString(4, message.authorId)
			preparedStatement.setTimestamp(5, new Timestamp(message.createdAt.getMillis))
			
			// Conversion de la map en JSON
			val metadataJson = Json.toJson(message.metadata).toString()
			preparedStatement.setString(6, metadataJson)
			
			// Données brutes
			val rawJson = rawData.getOrElse(Json.toJson(message).toString())
			preparedStatement.setString(7, rawJson)
			
			preparedStatement.executeUpdate()
		} catch {
			case e: Exception =>
				logger.error(s"Error inserting message: ${message.id}", e)
		}
	}
	
	/**
	 * Ferme les ressources
	 */
	override def close(): Unit = {
		if (preparedStatement != null) {
			try {
				preparedStatement.close()
			} catch {
				case _: Exception => // Ignorer
			}
		}
		
		if (connection != null) {
			try {
				connection.close()
			} catch {
				case _: Exception => // Ignorer
			}
		}
	}
}


