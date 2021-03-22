package org.infinispan.api.exception;

/**
 * Exception raised when a configuration error is found
 *
 * @author Katia Aresti, karesti@redhat.com
 * @since 10.0
 */
public class InfinispanConfigurationException extends InfinispanException {
   public InfinispanConfigurationException(String message) {
      super(message);
   }
}
