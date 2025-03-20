package modules.exporter.postgresql

import modules.ModuleFileProcessed

case class PostgresConfiguration(collect: String,
								 host: String,
								 port: Int,
								 base: String,
								 schema: String,
								 user: String,
								 password: String) {
	var filesProcessed: Seq[ModuleFileProcessed] = Seq[ModuleFileProcessed]()
}
