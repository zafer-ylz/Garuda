package providers.twitter

import org.joda.time.DateTime
import providers.{ProviderType, SocialMediaRule}

/**
 * Représente une règle de filtrage pour les messages Twitter.
 * Cette classe implémente l'interface SocialMediaRule et fournit
 * des fonctionnalités spécifiques au format des règles Twitter.
 *
 * @param id Identifiant optionnel de la règle (peut être null pour les nouvelles règles)
 * @param tag Tag/étiquette associé à la règle pour faciliter son identification
 * @param content Contenu de la règle (termes de recherche)
 * @param collectName Nom de la collection associée à cette règle
 * @param createdAt Date de création de la règle
 */
case class TwitterRule(
  override val id: Option[String],
  override val tag: String, 
  override val content: String,
  override val collectName: String,
  override val createdAt: DateTime = DateTime.now()
) extends SocialMediaRule {
  
  private var _isActive: Boolean = true
  
  /**
   * Renvoie le type de fournisseur pour cette règle.
   *
   * @return Le type de fournisseur Twitter
   */
  override def providerType: ProviderType.ProviderType = ProviderType.Twitter
  
  /**
   * Convertit la règle en format compatible avec l'API Twitter.
   * Pour Twitter, il s'agit simplement du contenu de la règle, nettoyé.
   *
   * @return Le contenu de la règle formaté pour l'API Twitter
   */
  def toTwitterFormat: String = {
    content.trim
  }
  
  /**
   * Convertit la règle en format spécifique au fournisseur Twitter.
   * Pour l'API Twitter v2, le format est un Map contenant la valeur et le tag.
   *
   * @return Un Map compatible avec l'API Twitter v2
   */
  override def toProviderSpecificFormat: Any = {
    Map(
      "value" -> content.trim,
      "tag" -> tag
    )
  }
  
  /**
   * Vérifie si un contenu donné correspond à cette règle.
   * 
   * @param content Le contenu à vérifier
   * @return true si le contenu correspond à la règle, false sinon
   */
  override def matches(content: String): Boolean = {
    // Si la règle contient des opérateurs OR, diviser en sous-termes
    if (this.content.contains(" OR ")) {
      val terms = this.content.split(" OR ").map(_.trim.toLowerCase)
      terms.exists(term => content.toLowerCase.contains(term))
    } else {
      // Recherche simple de terme exact
      content.toLowerCase.contains(this.content.toLowerCase)
    }
  }
  
  /**
   * Indique si la règle est active ou non.
   * 
   * @return true si la règle est active, false sinon
   */
  override def isActive: Boolean = _isActive
  
  /**
   * Modifie l'état d'activation de la règle.
   * 
   * @param active Le nouvel état d'activation
   */
  override def setActive(active: Boolean): Unit = {
    _isActive = active
  }
  
  /**
   * Crée une copie de cette règle avec de nouveaux paramètres.
   */
  def copy(
    id: Option[String] = this.id,
    tag: String = this.tag,
    content: String = this.content,
    collectName: String = this.collectName,
    createdAt: DateTime = this.createdAt
  ): TwitterRule = {
    val rule = TwitterRule(id, tag, content, collectName, createdAt)
    rule._isActive = this._isActive
    rule
  }
} 