package dao

import java.sql.Timestamp

import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.{Inject, Singleton}
import models.TemporaryRule
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
class TemporaryRuleDao @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
	extends HasDatabaseConfigProvider[JdbcProfile] {
	
	import profile.api._
	
	protected class TemporaryRulesTable(tag: Tag) extends Table[TemporaryRule](tag, "temporary_rule") {
		/**
		 * Fields
		 */
		def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
		
		def ruleTag = column[String]("tag")
		
		def content = column[String]("content")
		
		def collect = column[String]("collect")
		
		implicit def jodaTimeMapping: BaseColumnType[DateTime] = MappedColumnType.base[DateTime, Timestamp](
			dateTime => new Timestamp(dateTime.getMillis),
			timeStamp => new DateTime(timeStamp.getTime)
		)
		def createdAt = column[DateTime]("created_at")
		
		// Méthode pour faire la conversion entre les colonnes de la base de données et l'objet TemporaryRule
		def toTemporaryRule(id: Option[Long], tag: String, content: String, collect: String, jodaTime: DateTime): TemporaryRule = {
			TemporaryRule(id, tag, content, collect, jodaTime)
		}
		
		def fromTemporaryRule(rule: TemporaryRule): Option[(Option[Long], String, String, String, DateTime)] = {
			Some((rule.id, rule.tag, rule.content, rule.collectName, rule.createdAt))
		}
		
		override def * = (id.?, ruleTag, content, collect, createdAt) <> (toTemporaryRule, fromTemporaryRule)
	}
	
	private val rules = TableQuery[TemporaryRulesTable]
	
	/** Retrieve all the rules */
	def all(): Future[Seq[TemporaryRule]] = db.run(rules.result)
	
	/** Retrieve a rule from the id */
	def findById(id: Long): Future[Option[TemporaryRule]] =
		db.run(rules.filter(_.id === id).result.headOption)
	
	/** Retrieve rules from the collect name */
	def findByCollectName(name: String): Future[Seq[TemporaryRule]] =
		db.run(rules.filter(_.collect === name).result)
	
	private val insertQuery = rules returning rules.map(_.id) into ((rule, id) => rule.copy(id = Some(id)))
	
	/** Insert a new rule */
	def insert(rule: TemporaryRule): Future[TemporaryRule] = db.run(insertQuery += rule)//.map { id => rule.copy(id = Some(id)) }
	
	private def rulesAutoIncWithObject =
		(rules returning rules.map(_.id)).into((rule, id) => rule.copy(id = Some(id)))
	
	/** Insert new rules */
	def batchInsert(newRules: Seq[TemporaryRule]): Future[Seq[TemporaryRule]] = {
		db.run { rulesAutoIncWithObject ++= newRules }
	}
	
	/** Update a rule */
	def update(id: Long, rule: TemporaryRule): Future[Unit] = {
		val ruleToUpdate: TemporaryRule = rule.copy(Some(id))
		db.run(rules.filter(_.id === id).update(ruleToUpdate)).map(_ => ())
	}
	
	/** Delete a rule */
	def delete(id: Long): Future[Unit] =
		db.run(rules.filter(_.id === id).delete).map(_ => ())
		
	/** Delete a set of rules */
	def batchDelete(ids: Seq[Long]): Future[Unit] = {
		db.run(rules.filter(_.id.inSet(ids)).delete).map(_ => ())
	}
}