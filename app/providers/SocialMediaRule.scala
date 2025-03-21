package providers

import org.joda.time.DateTime

/**
 * Interface pour les règles de filtrage de messages sur les réseaux sociaux.
 * Définit les caractéristiques communes à toutes les règles, quel que soit le fournisseur.
 */
trait SocialMediaRule {
  /**
   * Identifiant unique de la règle (optionnel pour les nouvelles règles)
   */
  def id: Option[String]
  
  /**
   * Tag/étiquette associé à la règle pour faciliter son identification
   */
  def tag: String
  
  /**
   * Contenu de la règle (termes de recherche)
   */
  def content: String
  
  /**
   * Nom de la collection associée à cette règle
   */
  def collectName: String
  
  /**
   * Date de création de la règle
   */
  def createdAt: DateTime
  
  /**
   * Type de fournisseur de médias sociaux pour cette règle
   * 
   * @return Le type de fournisseur (Twitter, Bluesky, etc.)
   */
  def providerType: ProviderType.ProviderType
  
  /**
   * Vérifie si un contenu donné correspond à cette règle
   * 
   * @param content Le contenu à vérifier
   * @return true si le contenu correspond à la règle, false sinon
   */
  def matches(content: String): Boolean
  
  /**
   * Indique si la règle est active
   * 
   * @return true si la règle est active, false sinon
   */
  def isActive: Boolean
  
  /**
   * Définit si la règle est active ou non
   * 
   * @param active Le nouvel état d'activation
   */
  def setActive(active: Boolean): Unit

  /**
   * Convertit la règle en format spécifique au fournisseur
   * 
   * @return La règle dans un format compatible avec l'API du fournisseur
   */
  def toProviderSpecificFormat: Any
} 