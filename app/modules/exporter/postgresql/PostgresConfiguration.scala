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
	
	/**
	 * Convertit cette configuration en PostgresConfig
	 */
	def toPostgresConfig: PostgresConfig = {
		PostgresConfig(
			host = this.host,
			port = this.port,
			database = this.base,
			schema = this.schema,
			user = this.user,
			password = this.password
		)
	}
}
