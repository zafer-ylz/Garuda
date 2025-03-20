package modules.exporter.postgresql

import java.sql.SQLException
import java.sql.Statement

import models.tweet.{Place, Tweet, User}
import models.SocialMediaMessage
import providers.ProviderType
import play.api.Logging

/**
 * Gestionnaire d'insertion dans PostgreSQL
 */
class PostgresInsertion(val config: PostgresConfiguration) extends Logging {
	private val postgresDao = new PostgresDao(config)
	private val MAX_BATCH_SIZE = 1000
	private var batchCount = 0
	
	/**
	 * Insère une ligne de données brute (compatible avec Twitter)
	 */
	def insertLine(line: String): Unit = {
		try {
			val tweet = new Tweet(line)
			if (tweet.id.isDefined) {
				postgresDao.insertTweet(tweet)
				batchCount += 1
				if (batchCount >= MAX_BATCH_SIZE) {
					insertBatch()
				}
			}
		} catch {
			case e: Exception => logger.error("Failed to insert tweet line", e)
		}
	}
	
	/**
	 * Insère un message standardisé de Bluesky
	 */
	def insertBlueskyMessage(message: SocialMediaMessage): Unit = {
		try {
			if (message.providerType == ProviderType.Bluesky) {
				postgresDao.insertBlueskyMessage(message)
				batchCount += 1
				if (batchCount >= MAX_BATCH_SIZE) {
					insertBatch()
				}
			} else {
				logger.warn(s"Expected Bluesky message but got ${message.providerType}")
			}
		} catch {
			case e: Exception => logger.error("Failed to insert Bluesky message", e)
		}
	}
	
	/**
	 * Insère un lot de données
	 */
	def insertBatch(): Unit = {
		try {
			postgresDao.executeBatch()
			batchCount = 0
		} catch {
			case e: Exception => logger.error("Failed to execute batch", e)
		}
	}
	
	/**
	 * Ferme les connexions
	 */
	def close(): Unit = {
		postgresDao.close()
	}
}


