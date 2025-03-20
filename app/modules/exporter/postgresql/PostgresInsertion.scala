package modules.exporter.postgresql

import java.sql._
import models.SocialMediaMessage
import play.api.Logging

class PostgresInsertion(val config: PostgresConfig) extends Logging {
	private val conn: Connection = DriverManager.getConnection(config.getUrl, config.getProperties)
	private val schema: String = config.schema
	
	/**
	 * Insère une ligne dans la base de données
	 */
	def insertLine(json: String): Unit = {
		val stmt = conn.createStatement()
		try {
			stmt.execute(s"""INSERT INTO $schema.${PostgresConstants.MESSAGE_TABLE} 
				(id, provider_type, content, author_id, created_at, metadata) 
				VALUES ('${json}', 'twitter', '${json}', '${json}', NOW(), '${json}'::jsonb)
				ON CONFLICT (id) DO NOTHING""")
		} finally {
			stmt.close()
		}
	}
	
	/**
	 * Insère un message Bluesky dans la base de données
	 */
	def insertBlueskyMessage(message: SocialMediaMessage): Unit = {
		val stmt = conn.createStatement()
		try {
			// Insertion dans la table commune des messages
			val metadataJson = message.metadata.map {
				case (k, v: String) => s""""$k":"${escapeSQL(v)}""""
				case (k, v) => s""""$k":$v"""
			}.mkString("{", ",", "}")
			
			stmt.execute(s"""INSERT INTO $schema.${PostgresConstants.MESSAGE_TABLE} 
				(id, provider_type, content, author_id, created_at, metadata) 
				VALUES ('${message.id}', '${message.providerType}', '${escapeSQL(message.content)}', 
					'${message.authorId}', '${message.createdAt}', '$metadataJson'::jsonb)
				ON CONFLICT (id) DO NOTHING""")
			
			// Insertion dans la table spécifique Bluesky
			val replyCount = message.metadata.getOrElse("reply_count", 0).toString.toInt
			val repostCount = message.metadata.getOrElse("repost_count", 0).toString.toInt
			val likeCount = message.metadata.getOrElse("like_count", 0).toString.toInt
			
			stmt.execute(s"""INSERT INTO $schema.${PostgresConstants.BLUESKY_POST_TABLE} 
				(id, uri, cid, author, text, reply_count, repost_count, like_count, created_at, indexed_at) 
				VALUES ('${message.id}', 
					'${message.metadata.getOrElse("uri", "").toString.replace("'", "''")}',
					'${message.metadata.getOrElse("cid", "").toString.replace("'", "''")}',
					'${message.authorId}',
					'${escapeSQL(message.content)}',
					$replyCount,
					$repostCount,
					$likeCount,
					'${message.createdAt}',
					'${message.createdAt}')
				ON CONFLICT (id) DO NOTHING""")
		} finally {
			stmt.close()
		}
	}
	
	private def escapeSQL(str: String): String = {
		str.replace("'", "''")
	}
	
	def close(): Unit = {
		conn.close()
	}
}


