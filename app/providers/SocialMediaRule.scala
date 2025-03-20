package providers

import org.joda.time.DateTime

trait SocialMediaRule {
  // Identifiant unique de la règle
  def id: Option[Long]
  
  // Type de provider pour cette règle
  def providerType: ProviderType
  
  // Tag/étiquette associé à cette règle
  def tag: String
  
  // Contenu/expression de la règle
  def content: String
  
  // Nom de la collecte associée à cette règle
  def collectName: String
  
  // Date de création de la règle
  def createdAt: DateTime
  
  // Indique si la règle est active
  def isActive: Boolean
  
  // Définit si la règle est active ou non
  def setActive(active: Boolean): Unit
  
  // Convertit la règle dans le format spécifique au provider
  def toProviderSpecificFormat: Any
} 