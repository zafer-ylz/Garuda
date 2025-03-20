package providers

import models.{SocialMediaAccount, SocialMediaCollect, SocialMediaMessage}
import java.io.FileWriter

trait StreamingConnection {
  // Le compte associé à cette connexion
  def account: SocialMediaAccount
  
  // La collecte associée à cette connexion
  def collect: SocialMediaCollect
  
  // Indique si la connexion est active
  def isActive: Boolean
  
  // Initialise et démarre le streaming
  def stream(): StreamingConnection
  
  // Lance les listeners qui traitent les messages
  def executeListeners(): Unit
  
  // Arrête proprement la connexion
  def shutdown(): Unit
  
  // Ajoute un écouteur pour traiter les messages
  def addMessageListener(listener: MessageListener): Unit
  
  // Définit l'objet qui écrit les messages dans un fichier
  def setFileWriter(fileWriter: FileWriter): Unit
} 