package modules.exporter.postgresql

import play.api.data.Forms._
import play.api.data._

/**
 * Formulaire pour la configuration PostgreSQL
 */
object PostgresConfigurationForm {
  val form: Form[PostgresConfiguration] = Form(
    mapping(
      "Collect" -> nonEmptyText,
      "Host" -> nonEmptyText,
      "Port" -> number,
      "Base" -> nonEmptyText,
      "Schema" -> nonEmptyText,
      "User" -> nonEmptyText,
      "Password" -> nonEmptyText
    )(PostgresConfiguration.apply)(PostgresConfiguration.unapply)
  )
} 