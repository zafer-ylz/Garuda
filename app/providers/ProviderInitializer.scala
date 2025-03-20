package providers

/**
 * Configuration initiale des providers
 */
object ProviderInitializer {
  /**
   * Initialise tous les providers disponibles
   */
  def initialize(): Unit = {
    // Enregistrement du provider Twitter
    ProviderRegistry.register(
      ProviderType.Twitter,
      () => new TwitterProvider()
    )
    
    // Enregistrement du provider Bluesky
    ProviderRegistry.register(
      ProviderType.Bluesky,
      () => new BlueskyProvider()
    )
  }
} 