package models

import providers.ProviderType
import org.joda.time.DateTime

/**
 * Représente un compte de média social
 */
case class SocialMediaAccount(
  name: String,
  providerType: ProviderType.ProviderType,
  credentials: Map[String, String],
  createdAt: DateTime = new DateTime(),
  isActive: Boolean = false
) {
  private var activeCollect: Option[SocialMediaCollect] = None
  
  def hasActiveCollect: Boolean = activeCollect.isDefined
  
  def getActiveCollect: Option[SocialMediaCollect] = activeCollect
  
  def setActiveCollect(collect: Option[SocialMediaCollect]): Unit = {
    activeCollect = collect
  }

  def getIdentifier: String = credentials.getOrElse("identifier", name)
} 