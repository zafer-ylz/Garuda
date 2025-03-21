package providers

object ProviderType extends Enumeration {
  type ProviderType = Value
  
  val Twitter = Value("twitter")
  val Bluesky = Value("bluesky")
  
  def fromId(id: String): Option[ProviderType] = values.find(_.toString == id)
  
  // Méthode pour obtenir le nom lisible d'un type de provider
  def getDisplayName(providerType: ProviderType): String = providerType match {
    case Twitter => "Twitter/X"
    case Bluesky => "Bluesky"
    case _ => providerType.toString
  }
  
  // Méthode pour obtenir la description d'un type de provider
  def getDescription(providerType: ProviderType): String = providerType match {
    case Twitter => "Twitter/X API pour la collecte de tweets"
    case Bluesky => "Bluesky API pour la collecte de posts"
    case _ => "Provider non documenté"
  }
} 