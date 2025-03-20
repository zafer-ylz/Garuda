package providers

/**
 * Registre global pour les providers de réseaux sociaux
 */
object ProviderRegistry {
  // Instance unique de la factory
  private val factory = new ProviderFactory()
  
  /**
   * Enregistre un nouveau provider
   */
  def register[T <: SocialMediaProvider](
    providerType: ProviderType,
    constructor: () => T
  )(implicit tag: ClassTag[T]): Unit = {
    factory.registerProvider(providerType, constructor)
  }
  
  /**
   * Obtient une instance d'un provider
   */
  def getProvider(providerType: ProviderType): Option[SocialMediaProvider] = {
    factory.getOrCreateProvider(providerType)
  }
  
  /**
   * Liste tous les providers enregistrés
   */
  def registeredProviders: Set[ProviderType] = {
    factory.registeredProviderTypes
  }
  
  /**
   * Vérifie si un provider est enregistré
   */
  def isProviderRegistered(providerType: ProviderType): Boolean = {
    factory.isProviderRegistered(providerType)
  }
} 