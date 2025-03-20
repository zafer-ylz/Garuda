package models

import providers.{ProviderType, SocialMediaRule}
import java.time.DateTime

case class SocialMediaCollect(
  name: String,
  directory: String,
  accountName: String,
  providerType: ProviderType,
  createdAt: DateTime = DateTime.now()
) {
  private var rules: Seq[SocialMediaRule] = Seq.empty
  private var isActive: Boolean = false
  
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
    isActive = active
  }
  
  def isActive: Boolean = isActive
  
  def close(): Unit = {
    isActive = false
  }
} 