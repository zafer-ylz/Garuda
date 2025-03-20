package providers

import scala.collection.mutable
import scala.reflect.ClassTag

/**
 * Registre global pour les providers de réseaux sociaux
 * Fusionne les anciennes classes ProviderRegistry et ProviderFactory
 */
object ProviderRegistry {
  // Map thread-safe pour stocker les providers enregistrés
  private val providers: mutable.Map[ProviderType, SocialMediaProvider] = mutable.Map.empty
  
  // Map thread-safe pour stocker les constructeurs de providers
  private val providerConstructors: mutable.Map[ProviderType, () => SocialMediaProvider] = mutable.Map.empty
  
  /**
   * Enregistre un nouveau provider
   */
  def register[T <: SocialMediaProvider](
    providerType: ProviderType,
    constructor: () => T
  )(implicit tag: ClassTag[T]): Unit = {
    synchronized {
      providerConstructors.put(providerType, constructor)
    }
  }
  
  /**
   * Crée une nouvelle instance d'un provider
   */
  private def createProvider(providerType: ProviderType): Option[SocialMediaProvider] = {
    synchronized {
      providerConstructors.get(providerType).map(_.apply())
    }
  }
  
  /**
   * Obtient une instance d'un provider
   */
  def getProvider(providerType: ProviderType): Option[SocialMediaProvider] = {
    synchronized {
      providers.getOrElseUpdate(providerType, {
        createProvider(providerType).getOrElse(
          throw new IllegalArgumentException(s"Provider type $providerType not registered")
        )
      })
    }
  }
  
  /**
   * Liste tous les providers enregistrés
   */
  def registeredProviders: Set[ProviderType] = {
    synchronized {
      providerConstructors.keySet.toSet
    }
  }
  
  /**
   * Vérifie si un provider est enregistré
   */
  def isProviderRegistered(providerType: ProviderType): Boolean = {
    synchronized {
      providerConstructors.contains(providerType)
    }
  }
} 