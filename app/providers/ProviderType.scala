package providers

sealed trait ProviderType {
  def id: String
  def name: String
  def description: String
}

object ProviderType {
  case object Twitter extends ProviderType {
    val id = "twitter"
    val name = "Twitter/X"
    val description = "Twitter/X API pour la collecte de tweets"
  }
  
  case object Bluesky extends ProviderType {
    val id = "bluesky"
    val name = "Bluesky"
    val description = "Bluesky API pour la collecte de posts"
  }
  
  // Méthode pour récupérer un ProviderType depuis son id
  def fromId(id: String): Option[ProviderType] = id match {
    case Twitter.id => Some(Twitter)
    case Bluesky.id => Some(Bluesky)
    case _ => None
  }
  
  // Liste de tous les providers disponibles
  def values: Seq[ProviderType] = Seq(Twitter, Bluesky)
} 