package dao

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

/**
 * Classe de base générique pour les DAO
 * @tparam T Le type d'entité géré par ce DAO
 * @tparam I Le type de l'identifiant
 */
abstract class BaseDao[T, I] @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {
  
  import profile.api._
  
  /**
   * Définition de la table utilisée par ce DAO
   */
  protected val tableQuery: TableQuery[_ <: Table[T]]
  
  /**
   * Fonction pour filtrer un élément par son ID
   */
  protected def filterById(id: I): Query[_ <: Table[T], T, Seq]
  
  /**
   * Récupère tous les éléments
   */
  def all(): Future[Seq[T]] = db.run(tableQuery.result)
  
  /**
   * Récupère un élément par son ID
   */
  def findById(id: I): Future[Option[T]] = db.run(filterById(id).result.headOption)
  
  /**
   * Insère un nouvel élément
   */
  def insert(entity: T): Future[Unit] = db.run(tableQuery += entity).map { _ => () }
  
  /**
   * Insère plusieurs éléments
   */
  def batchInsert(entities: Seq[T]): Future[Unit] = db.run(tableQuery ++= entities).map { _ => () }
  
  /**
   * Supprime un élément par son ID
   */
  def delete(id: I): Future[Unit] = db.run(filterById(id).delete).map(_ => ())
} 