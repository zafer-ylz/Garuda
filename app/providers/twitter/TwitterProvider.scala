package providers.twitter

import providers._
import models.{SocialMediaAccount, SocialMediaCollect, SocialMediaRule, SocialMediaMessage}
import com.twitter.clientlib.TwitterCredentialsBearer
import com.twitter.clientlib.api.TwitterApi
import java.time.DateTime

class TwitterProvider extends SocialMediaProvider {
  override def providerType: ProviderType = ProviderType.Twitter
  
  override def createConnection(account: SocialMediaAccount): Either[String, StreamingConnection] = {
    try {
      val credentials = account.credentials.getOrElse("bearerToken", 
        throw new IllegalArgumentException("Twitter account must have a bearer token"))
      val apiInstance = new TwitterApi(new TwitterCredentialsBearer(credentials))
      Right(new TwitterStreamingConnection(apiInstance, account))
    } catch {
      case e: Exception => Left(s"Failed to create Twitter connection: ${e.getMessage}")
    }
  }
  
  override def getActiveRules(account: SocialMediaAccount, collectName: String): Either[String, Seq[SocialMediaRule]] = {
    try {
      val credentials = account.credentials.getOrElse("bearerToken", 
        throw new IllegalArgumentException("Twitter account must have a bearer token"))
      val apiInstance = new TwitterApi(new TwitterCredentialsBearer(credentials))
      val result = apiInstance.tweets().getRules().execute()
      
      if (result.getData != null) {
        Right(result.getData.asScala.map(rule =>
          TwitterRule(
            Some(rule.getId.toLong),
            rule.getTag,
            rule.getValue,
            collectName,
            DateTime.now()
          )).toSeq)
      } else {
        Right(Seq.empty)
      }
    } catch {
      case e: Exception => Left(s"Failed to get Twitter rules: ${e.getMessage}")
    }
  }
  
  override def addRules(account: SocialMediaAccount, collectName: String, rules: Seq[SocialMediaRule]): Either[String, Seq[SocialMediaRule]] = {
    try {
      val credentials = account.credentials.getOrElse("bearerToken", 
        throw new IllegalArgumentException("Twitter account must have a bearer token"))
      val apiInstance = new TwitterApi(new TwitterCredentialsBearer(credentials))
      
      val addRulesRequest = new AddRulesRequest()
      rules.foreach { rule =>
        val newRule = new RuleNoId()
        newRule.value(rule.content)
        newRule.tag(rule.tag)
        addRulesRequest.addAddItem(newRule)
      }
      
      val result = apiInstance.tweets().addOrDeleteRules(addRulesRequest).execute()
      
      if (result.getData != null) {
        Right(result.getData.asScala.map(rule =>
          TwitterRule(
            Some(rule.getId.toLong),
            rule.getTag,
            rule.getValue,
            collectName,
            DateTime.now()
          )).toSeq)
      } else {
        Right(Seq.empty)
      }
    } catch {
      case e: Exception => Left(s"Failed to add Twitter rules: ${e.getMessage}")
    }
  }
  
  override def removeRules(account: SocialMediaAccount, rules: Seq[SocialMediaRule]): Either[String, Seq[SocialMediaRule]] = {
    try {
      val credentials = account.credentials.getOrElse("bearerToken", 
        throw new IllegalArgumentException("Twitter account must have a bearer token"))
      val apiInstance = new TwitterApi(new TwitterCredentialsBearer(credentials))
      
      val deleteRulesRequest = new DeleteRulesRequest()
      val deleteRulesRequestDelete = new DeleteRulesRequestDelete()
      rules.foreach { rule =>
        rule.id.foreach { id =>
          deleteRulesRequestDelete.addIdsItem(id.toString)
        }
      }
      deleteRulesRequest.setDelete(deleteRulesRequestDelete)
      
      apiInstance.tweets().addOrDeleteRules(deleteRulesRequest).execute()
      Right(Seq.empty)
    } catch {
      case e: Exception => Left(s"Failed to remove Twitter rules: ${e.getMessage}")
    }
  }
  
  override def startCollect(account: SocialMediaAccount, collect: SocialMediaCollect): Either[String, StreamingConnection] = {
    createConnection(account).map { connection =>
      connection.stream()
      connection
    }
  }
  
  override def stopCollect(connection: StreamingConnection): Boolean = {
    connection.shutdown()
    true
  }
  
  override def validateRule(rule: String): Boolean = {
    // Twitter rules validation logic
    rule.nonEmpty && rule.length <= 512
  }
  
  override def normalizeMessage(rawMessage: String): SocialMediaMessage = {
    // Convert Twitter JSON to SocialMediaMessage
    // This is a placeholder - actual implementation would parse the Twitter JSON
    SocialMediaMessage(
      id = "placeholder",
      providerType = ProviderType.Twitter,
      content = rawMessage,
      authorId = "placeholder",
      createdAt = DateTime.now()
    )
  }
} 