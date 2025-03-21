package modules.dao

import java.sql.Timestamp

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.{Inject, Singleton}
import modules.ModuleFileProcessed
import org.joda.time.DateTime
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import dao.BaseDao

@Singleton
class ModuleFileProcessedDao @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
	extends BaseDao[ModuleFileProcessed, (String, String, String)](dbConfigProvider) {
	
	import profile.api._
	
	protected class ModulesFilesProcessedTable(tag: Tag) extends Table[ModuleFileProcessed](tag, "module_file_processed") {
		/**
		 * Fields
		 */
		def collect = column[String]("collect")
		
		def module = column[String]("module_name")
		
		def file = column[String]("file_name")
		
		implicit def jodaTimeMapping: BaseColumnType[DateTime] = MappedColumnType.base[DateTime, Timestamp](
			dateTime => new Timestamp(dateTime.getMillis),
			timeStamp => new DateTime(timeStamp.getTime)
		)
		def processedAt = column[DateTime]("processed_at")
		
		def pk = primaryKey("pk_a", (collect, module, file))
		
		override def * = (collect, module, file, processedAt) <> ((ModuleFileProcessed.apply _).tupled, ModuleFileProcessed.unapply)
	}
	
	override protected val tableQuery = TableQuery[ModulesFilesProcessedTable]
	
	override protected def filterById(id: (String, String, String)): Query[ModulesFilesProcessedTable, ModuleFileProcessed, Seq] =
		tableQuery.filter(f => f.collect === id._1 && f.module === id._2 && f.file === id._3)
	
	/** Retrieve the files processed from the collect and the module names */
	def findByCollect(collect: String, module: String): Future[Seq[ModuleFileProcessed]] = {
		db.run(tableQuery.filter(f => f.collect === collect && f.module === module).result)
	}
	
	/** Insert a new file processed configuration */
	def insert(moduleFileProcessed: ModuleFileProcessed): Future[Unit] = {
		db.run(tableQuery += moduleFileProcessed).map { _ => () }
	}
}

