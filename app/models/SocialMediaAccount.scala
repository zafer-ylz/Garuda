package models

import providers.ProviderType
import java.time.DateTime

case class SocialMediaAccount(
  name: String,
  providerType: ProviderType,
  credentials: Map[String, String],
  createdAt: DateTime = DateTime.now()
) {
  private var activeCollect: Option[SocialMediaCollect] = None
  
  def hasActiveCollect: Boolean = activeCollect.isDefined
  
  def getActiveCollect: Option[SocialMediaCollect] = activeCollect
  
  def setActiveCollect(collect: Option[SocialMediaCollect]): Unit = {
    activeCollect = collect
  }
} 