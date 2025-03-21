package models

import providers.{ProviderType, SocialMediaRule}
import org.joda.time.DateTime

case class SocialMediaCollect(
  name: String,
  directory: String,
  accountName: String,
  providerType: ProviderType,
  createdAt: DateTime = DateTime.now()
) {
  private var rules: Seq[SocialMediaRule] = Seq.empty
  private var _isActive: Boolean = false
  
  def activeRules: Seq[SocialMediaRule] = rules.filter(_.isActive)
  
  def addRule(rule: SocialMediaRule): Boolean = {
    rules = rules :+ rule
    true
  }
  
  def removeRules(rulesToRemove: Seq[SocialMediaRule]): Boolean = {
    rules = rules.filterNot(rule => rulesToRemove.map(_.id).contains(rule.id))
    true
  }
  
  def setActive(active: Boolean): Unit = {
    _isActive = active
  }
  
  def isActive: Boolean = _isActive
  
  def close(): Unit = {
    _isActive = false
  }
} 