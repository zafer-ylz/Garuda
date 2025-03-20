package models.forms

import play.api.data.Forms._
import play.api.data._
import modules.exporter.postgresql.PostgresConfiguration

object PostgresConfigurationForm extends BaseForm[PostgresConfiguration] {
  override val form: Form[PostgresConfiguration] = Form(
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