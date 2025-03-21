package models

import java.time.LocalDateTime

case class TemporaryRule(id: Option[Long], tag: String, content: String, collectName: String, createdAt: LocalDateTime = LocalDateTime.now())

object TemporaryRuleForm {
	
	/**
	 * Forms related
	 */
	import play.api.data.Forms._
	import play.api.data._
	
	case class TemporaryRuleData(tag: String, content: String)
	
	val form: Form[TemporaryRuleData] = Form(
		mapping(
			"Tag" -> nonEmptyText,
			"Content" -> nonEmptyText
		)(TemporaryRuleData.apply)(TemporaryRuleData.unapply)
	)
}