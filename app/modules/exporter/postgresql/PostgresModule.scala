package modules.exporter.postgresql

import java.io.File

import cache.ModuleCache
import models.SocialCollect
import models.SocialMediaMessage
import modules.{MessageAdapter, Module, ModuleConfiguration, ModuleFileProcessed, ModulePlugin}
import modules.dao.ModuleFileProcessedDao
import providers.ProviderType
import scala.concurrent.ExecutionContext

/**
 * Module d'exportation vers PostgreSQL
 */
class PostgresModule(
	override val collect: SocialCollect,
	val config: PostgresConfig,
	val moduleFileProcessedDao: ModuleFileProcessedDao
)(implicit ec: ExecutionContext) extends Module with ModulePlugin {
	
	ModuleCache.add(collect.name, this)
	
	// Initialisation de l'écouteur de fichiers
	private val listener = new PostgresFileListener(collect.observableFile, config, this)
	
	// Traitement des fichiers existants
	private val existingFiles = new File(collect.directory)
		.listFiles()
		.filterNot(_.getAbsolutePath.endsWith("Errors"))
		.flatMap(_.listFiles())
	
	private val filesToProcess = existingFiles.filterNot(
		f => config.filesProcessed.exists(
			mfp => mfp.file == s"${f.getParent}${File.separator}${f.getName}"
		)
	)
	
	// Traite les fichiers existants
	for (file <- filesToProcess) {
		val fullFileReader = new PostgresFullFileReader(file.getAbsolutePath, config, this)
		fullFileReader.readFile()
		val fileProcessed = ModuleFileProcessed(collect.name, "ExporterPostgresql", s"${f.getParent}${File.separator}${f.getName}")
		config.filesProcessed :+= fileProcessed
		moduleFileProcessedDao.insert(fileProcessed)
	}
	
	/**
	 * Nom du plugin
	 */
	override def name: String = "PostgresExporter"
	
	/**
	 * Description du plugin
	 */
	override def description: String = "Exporte les données vers PostgreSQL"
	
	/**
	 * Version du plugin
	 */
	override def version: String = "1.0.0"
	
	/**
	 * Liste des providers supportés
	 */
	override def supportedProviders: Seq[ProviderType] = Seq(ProviderType.Twitter, ProviderType.Bluesky)
	
	/**
	 * Traite un message standardisé
	 */
	override def processMessage(message: SocialMediaMessage): Unit = {
		val postgresInsertion = new PostgresInsertion(config)
		try {
			// Insertion dans la base de données PostgreSQL
			message.providerType match {
				case ProviderType.Twitter =>
					// Pour Twitter, utilisation de l'insertion existante
					postgresInsertion.insertLine(message.toJson)
				case ProviderType.Bluesky =>
					// Pour Bluesky, utilisation de l'insertion adaptée
					postgresInsertion.insertBlueskyMessage(message)
				case _ =>
					// Pour tout autre provider non supporté
					throw new UnsupportedOperationException(s"Provider ${message.providerType} not supported by PostgresModule")
			}
		} catch {
			case t: Throwable => 
				t.printStackTrace()
		} finally {
			postgresInsertion.close()
		}
	}
	
	/**
	 * Initialise le plugin
	 */
	override def initialize(): Unit = {
		// Initialisation des tables si nécessaire
		val postgresDao = new PostgresDao(config)
		postgresDao.setupCollect()
		postgresDao.close()
	}
	
	/**
	 * Arrête le plugin
	 */
	override def shutdown(): Unit = {
		listener.stop()
		ModuleCache.remove(collect.name)
	}
}
