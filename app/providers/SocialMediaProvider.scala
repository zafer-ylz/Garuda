package providers

import models.{SocialMediaAccount, SocialMediaCollect, SocialMediaRule, SocialMediaMessage}
import java.time.DateTime

trait SocialMediaProvider {
  // Identifiant du type de provider (Twitter, Bluesky, etc.)
  def providerType: ProviderType
  
  // Crée une connexion pour ce provider avec les identifiants spécifiés
  def createConnection(account: SocialMediaAccount): Either[String, StreamingConnection]
  
  // Récupère les règles actives pour un compte
  def getActiveRules(account: SocialMediaAccount, collectName: String): Either[String, Seq[SocialMediaRule]]
  
  // Ajoute des règles au compte
  def addRules(account: SocialMediaAccount, collectName: String, rules: Seq[SocialMediaRule]): Either[String, Seq[SocialMediaRule]]
  
  // Supprime des règles du compte
  def removeRules(account: SocialMediaAccount, rules: Seq[SocialMediaRule]): Either[String, Seq[SocialMediaRule]]
  
  // Démarre une collecte pour les règles spécifiées
  def startCollect(account: SocialMediaAccount, collect: SocialMediaCollect): Either[String, StreamingConnection]
  
  // Arrête une collecte en cours
  def stopCollect(connection: StreamingConnection): Boolean
  
  // Valide une règle pour ce provider (syntaxe spécifique)
  def validateRule(rule: String): Boolean
  
  // Convertit un message brut en format normalisé pour l'application
  def normalizeMessage(rawMessage: String): SocialMediaMessage
} 