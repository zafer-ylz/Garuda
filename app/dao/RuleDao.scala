package dao

import java.sql.Timestamp
import javax.inject.{Inject, Singleton}
import models.Rule
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import play.api.libs.json.{JodaReads, JodaWrites}
import com.github.tototoshi.slick.MySQLJodaSupport._

/**
 * DAO pour les règles de collecte
 */
@Singleton
class RuleDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                       (implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  /**
   * Table des règles
   */
  class RuleTable(tag: Tag) extends Table[Rule](tag, "rule") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def ruleTag = column[String]("tag")
    def content = column[String]("content")
    def collect = column[String]("collect_name")
    
    implicit def jodaTimeMapping: BaseColumnType[DateTime] = MappedColumnType.base[DateTime, Timestamp](
      dateTime => new Timestamp(dateTime.getMillis),
      timeStamp => new DateTime(timeStamp.getTime)
    )
    def createdAt = column[DateTime]("created_at")
    def isActive = column[Boolean]("is_active")

    // Méthode pour faire la conversion entre les colonnes de la base de données et l'objet Rule
    def toRule(id: Long, tag: String, content: String, collect: String, jodaTime: DateTime, active: Boolean): Rule = {
      val rule = new Rule(id, tag, content, collect, jodaTime, active)
      if (active) rule.setActive(true)
      rule
    }
    
    def fromRule(rule: Rule): Option[(Long, String, String, String, DateTime, Boolean)] = {
      Some((rule.id, rule.tag, rule.content, rule.collectName, rule.createdAt, rule.isActive))
    }
    
    def * = (id, ruleTag, content, collect, createdAt, isActive) <> (toRule, fromRule)
  }

  private val rules = TableQuery[RuleTable]

  /**
   * Récupère toutes les règles
   */
  def all(): Future[Seq[Rule]] = db.run(rules.result)

  /**
   * Récupère une règle par son ID
   */
  def findById(id: Long): Future[Option[Rule]] = db.run(rules.filter(_.id === id).result.headOption)

  /**
   * Récupère toutes les règles pour une collecte donnée
   */
  def findByCollect(collectName: String): Future[Seq[Rule]] = 
    db.run(rules.filter(_.collect === collectName).result)

  /**
   * Insère une nouvelle règle
   */
  def insert(rule: Rule): Future[Rule] = {
    val insertQuery = rules returning rules.map(_.id) into ((rule, id) => rule.copy(id))
    db.run(insertQuery += rule)
  }

  /**
   * Insère plusieurs règles en une seule opération
   */
  def batchInsert(newRules: Seq[Rule]): Future[Seq[Rule]] = {
    val insertQuery = rules returning rules.map(_.id) into ((rule, id) => rule.copy(id))
    db.run(DBIO.sequence(newRules.map(insertQuery += _)))
  }

  /**
   * Met à jour une règle existante
   */
  def update(id: Long, rule: Rule): Future[Int] = {
    val ruleToUpdate = rule.copy(id)
    db.run(rules.filter(_.id === id).update(ruleToUpdate))
  }

  /**
   * Supprime une règle par son ID
   */
  def delete(id: Long): Future[Int] = 
    db.run(rules.filter(_.id === id).delete)

  /**
   * Supprime plusieurs règles en une seule opération
   */
  def batchDelete(ids: Seq[Long]): Future[Int] =
    db.run(rules.filter(_.id.inSet(ids)).delete)

  /**
   * Supprime toutes les règles pour une collecte donnée
   */
  def deleteByCollect(collectName: String): Future[Int] =
    db.run(rules.filter(_.collect === collectName).delete)
}