package modules

import models.SocialCollect
import models.SocialMediaMessage
import providers.ProviderType

/**
 * Interface générique pour les modules de traitement de données
 */
trait Module {
	/**
	 * Collecte associée à ce module
	 */
	val collect: SocialCollect
	
	/**
	 * Types de providers supportés par ce module
	 */
	def supportedProviders: Seq[ProviderType]
	
	/**
	 * Vérifie si le module supporte un type de provider donné
	 */
	def supportsProvider(providerType: ProviderType): Boolean = 
		supportedProviders.contains(providerType)
	
	/**
	 * Traite un message
	 */
	def processMessage(message: SocialMediaMessage): Unit
	
	/**
	 * Arrête le module
	 */
	def stop(): Unit
}
