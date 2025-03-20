package dao

import java.sql.Timestamp

import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.{Inject, Singleton}
import models.Rule
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
class RuleDao @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
	extends BaseDao[Rule, Long](dbConfigProvider) {
	
	import profile.api._
	
	override protected val tableQuery = TableQuery[RulesTable]
	
	override protected def filterById(id: Long): Query[RulesTable, Rule, Seq] =
		tableQuery.filter(_.id === id)
	
	/** Retrieve rules from the collect name */
	def findByCollectName(name: String): Future[Seq[Rule]] =
		db.run(tableQuery.filter(_.collect === name).result)
	
	/** Update a rule */
	def update(id: Long, rule: Rule): Future[Unit] = {
		val ruleToUpdate: Rule = rule.copy(id)
		db.run(tableQuery.filter(_.id === id).update(ruleToUpdate)).map(_ => ())
	}
	
	/** Delete a set of rules */
	def batchDelete(ids: Seq[Long]): Future[Unit] = {
		db.run(tableQuery.filter(_.id.inSet(ids)).delete).map(_ => ())
	}
	
	private class RulesTable(tag: Tag) extends Table[Rule](tag, "rule") {
		/**
		 * Fields
		 */
		def id = column[Long]("id", O.PrimaryKey)
		
		def ruleTag = column[String]("tag")
		
		def content = column[String]("content")
		
		def collect = column[String]("collect")
		
		implicit def jodaTimeMapping: BaseColumnType[DateTime] = MappedColumnType.base[DateTime, Timestamp](
			dateTime => new Timestamp(dateTime.getMillis),
			timeStamp => new DateTime(timeStamp.getTime)
		)
		def createdAt = column[DateTime]("created_at")
		
		
		override def * = (id, ruleTag, content, collect, createdAt).mapTo[Rule]
	}
}