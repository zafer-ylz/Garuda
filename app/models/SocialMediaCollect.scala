package models

import providers.ProviderType
import org.joda.time.DateTime

/**
 * Représente une collecte de données de médias sociaux
 */
case class SocialMediaCollect(
  name: String,
  directory: String,
  accountName: String,
  providerType: ProviderType.ProviderType,
  isActive: Boolean = false,
  createdAt: DateTime = new DateTime()
) {
  private var rules: Seq[providers.SocialMediaRule] = Seq.empty
  // Maintenir une copie privée de isActive qu'on peut modifier
  private var _isActive: Boolean = isActive
  
  def getRules: Seq[providers.SocialMediaRule] = rules
  
  def setRules(newRules: Seq[providers.SocialMediaRule]): Unit = {
    rules = newRules
  }
  
  def addRule(rule: providers.SocialMediaRule): Unit = {
    rules = rules :+ rule
  }
  
  def removeRules(rulesToRemove: Seq[providers.SocialMediaRule]): Boolean = {
    rules = rules.filterNot(rule => rulesToRemove.map(_.id).contains(rule.id))
    true
  }
  
  // Surcharge la méthode isActive pour utiliser la version privée modifiable
  def isActive: Boolean = _isActive
  
  // Permet de modifier l'état actif
  def setActive(active: Boolean): Unit = {
    _isActive = active
  }
  
  def close(): Unit = {
    _isActive = false
  }
} 