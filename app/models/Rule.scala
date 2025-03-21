// Ce fichier est maintenu pour des raisons de compatibilité avec l'ancien code.
// Pour les nouveaux développements, utiliser providers.SocialMediaRule à la place.

package models

import java.time.LocalDateTime

// Objet companion pour créer des instances de Rule
// La classe Rule elle-même est définie dans Collect.scala
object Rule {
  // Méthode factory pour créer des instances de Rule
  def apply(id: Long, tag: String, content: String, collectName: String,
            createdAt: LocalDateTime = LocalDateTime.now(), isActive: Boolean = false): Rule = {
    val rule = new Rule(id, tag, content, collectName, createdAt, isActive)
    rule
  }
}

