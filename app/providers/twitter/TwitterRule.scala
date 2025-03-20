package providers.twitter

import providers.{ProviderType, SocialMediaRule}
import org.joda.time.DateTime

case class TwitterRule(
  override val id: Option[Long],
  override val tag: String,
  override val content: String,
  override val collectName: String,
  override val createdAt: DateTime = DateTime.now()
) extends SocialMediaRule {
  private var active: Boolean = false
  
  override def providerType: ProviderType = ProviderType.Twitter
  
  override def isActive: Boolean = active
  
  override def setActive(active: Boolean): Unit = {
    this.active = active
  }
  
  override def toProviderSpecificFormat: Any = {
    import com.twitter.clientlib.model.RuleNoId
    val rule = new RuleNoId()
    rule.value(content)
    rule.tag(tag)
    rule
  }
} 