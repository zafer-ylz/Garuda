package providers.bluesky

import providers.{ProviderType, SocialMediaRule}
import org.joda.time.DateTime

case class BlueskyRule(
  override val id: Option[Long],
  override val tag: String,
  override val content: String,
  override val collectName: String,
  override val createdAt: DateTime = DateTime.now()
) extends SocialMediaRule {
  private var active: Boolean = false
  
  override def providerType: ProviderType = ProviderType.Bluesky
  
  override def isActive: Boolean = active
  
  override def setActive(active: Boolean): Unit = {
    this.active = active
  }
  
  override def toProviderSpecificFormat: Any = {
    // TODO: Implement Bluesky rule format
    Map(
      "tag" -> tag,
      "content" -> content
    )
  }
} 