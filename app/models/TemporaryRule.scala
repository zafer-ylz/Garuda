package models

import org.joda.time.DateTime

/**
 * Représente une règle temporaire pour les collectes de médias sociaux
 */
case class TemporaryRule(
	id: Option[Long],
	tag: String,
	content: String,
	collectName: String,
	createdAt: DateTime = new DateTime()
) {
	// Alias pour compatibilité
	def ruleTag: String = tag
	def collect: String = collectName
}

object TemporaryRule {
	def apply(tag: String, content: String, collectName: String): TemporaryRule = {
		new TemporaryRule(None, tag, content, collectName)
	}
	
	def apply(id: Long, tag: String, content: String, collectName: String, createdAt: DateTime): TemporaryRule = {
		new TemporaryRule(Some(id), tag, content, collectName, createdAt)
	}
	
	def tupled: ((Option[Long], String, String, String, DateTime)) => TemporaryRule = {
		case (id, tag, content, collectName, createdAt) =>
			new TemporaryRule(id, tag, content, collectName, createdAt)
	}
	
	def unapply(rule: TemporaryRule): Option[(Option[Long], String, String, String, DateTime)] = {
		Some((rule.id, rule.tag, rule.content, rule.collectName, rule.createdAt))
	}
}

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