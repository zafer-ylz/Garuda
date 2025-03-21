// Ce fichier est maintenu pour des raisons de compatibilité avec l'ancien code.
// Pour les nouveaux développements, utiliser providers.SocialMediaRule à la place.

package models

import java.time.LocalDateTime

case class Rule(id: Long, tag: String, content: String, collectName: String, createdAt: LocalDateTime = LocalDateTime.now()) {
	/**
	 * A rule is active when it is currently known by Twitter API.
	 */
	private var _isActive: Boolean = false
	
	/**
	 * Définit si la règle est active
	 */
	def setActive(active: Boolean): Unit = {
		_isActive = active
	}
	
	/**
	 * Indique si la règle est active
	 */
	def isActive: Boolean = _isActive
	
	/**
	 * Accesseur pour la compatibilité avec collectName/collect
	 */
	def collect: String = collectName
	
	/**
	 * Propriété pour l'accès via la nouvelle architecture
	 */
	def active: Boolean = _isActive
}

