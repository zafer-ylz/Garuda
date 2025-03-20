package providers

import scala.collection.mutable
import scala.reflect.ClassTag

/**
 * Factory pour gérer les différents providers de réseaux sociaux
 */
class ProviderFactory {
  // Map thread-safe pour stocker les providers enregistrés
  private val providers: mutable.Map[ProviderType, SocialMediaProvider] = mutable.Map.empty
  
  // Map thread-safe pour stocker les constructeurs de providers
  private val providerConstructors: mutable.Map[ProviderType, () => SocialMediaProvider] = mutable.Map.empty
  
  /**
   * Enregistre un nouveau type de provider avec son constructeur
   * @param providerType Le type de provider
   * @param constructor Le constructeur du provider
   */
  def registerProvider[T <: SocialMediaProvider](
    providerType: ProviderType,
    constructor: () => T
  )(implicit tag: ClassTag[T]): Unit = {
    synchronized {
      providerConstructors.put(providerType, constructor)
    }
  }
  
  /**
   * Crée une nouvelle instance d'un provider
   * @param providerType Le type de provider à créer
   * @return Une instance du provider ou None si le type n'est pas enregistré
   */
  def createProvider(providerType: ProviderType): Option[SocialMediaProvider] = {
    synchronized {
      providerConstructors.get(providerType).map(_.apply())
    }
  }
  
  /**
   * Obtient une instance existante ou crée une nouvelle instance d'un provider
   * @param providerType Le type de provider
   * @return Une instance du provider ou None si le type n'est pas enregistré
   */
  def getOrCreateProvider(providerType: ProviderType): Option[SocialMediaProvider] = {
    synchronized {
      providers.getOrElseUpdate(providerType, {
        createProvider(providerType).getOrElse(
          throw new IllegalArgumentException(s"Provider type $providerType not registered")
        )
      })
    }
  }
  
  /**
   * Liste tous les types de providers enregistrés
   */
  def registeredProviderTypes: Set[ProviderType] = {
    synchronized {
      providerConstructors.keySet.toSet
    }
  }
  
  /**
   * Vérifie si un type de provider est enregistré
   */
  def isProviderRegistered(providerType: ProviderType): Boolean = {
    synchronized {
      providerConstructors.contains(providerType)
    }
  }
} 