package test.mocks

import models.SocialMediaMessage
import providers.bluesky.BlueskyConnection
import providers.ProviderType
import org.joda.time.DateTime

class BlueskyApiMock extends BlueskyConnection {
  private var posts: List[SocialMediaMessage] = List()
  private var isConnected: Boolean = false
  
  override def validateCredentials(): Boolean = {
    isConnected = true
    true
  }
  
  override def addRule(rule: String): Boolean = {
    true
  }
  
  override def getRules(): List[String] = {
    List()
  }
  
  override def removeRule(rule: String): Boolean = {
    true
  }
  
  override def startStreaming(): Boolean = {
    true
  }
  
  override def stopStreaming(): Boolean = {
    true
  }
  
  // Méthodes pour les tests
  def addTestPost(post: SocialMediaMessage): Unit = {
    posts = post :: posts
  }
  
  def clearPosts(): Unit = {
    posts = List()
  }
  
  def getTestPosts: List[SocialMediaMessage] = posts
  
  def simulatePostReceived(post: SocialMediaMessage): Unit = {
    onMessageReceived(post)
  }
  
  def isStreamingActive: Boolean = isConnected
} 