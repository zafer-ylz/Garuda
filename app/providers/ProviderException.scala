package providers

/**
 * Exceptions spécifiques aux providers
 */
sealed trait ProviderException extends Exception
case class ProviderNotRegisteredException(providerType: ProviderType) 
  extends ProviderException {
  override def getMessage: String = s"Provider $providerType is not registered"
}

case class ProviderCreationException(providerType: ProviderType, cause: Throwable)
  extends ProviderException {
  override def getMessage: String = s"Failed to create provider $providerType: ${cause.getMessage}"
  override def getCause: Throwable = cause
} 