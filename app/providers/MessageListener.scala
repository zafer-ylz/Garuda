package providers

import models.SocialMediaMessage

trait MessageListener {
  // Méthode appelée quand un nouveau message est reçu
  def onMessage(message: SocialMediaMessage): Unit
  
  // Méthode appelée en cas d'erreur
  def onError(error: Throwable): Unit
  
  // Méthode appelée à la fermeture de la connexion
  def onComplete(): Unit
} 