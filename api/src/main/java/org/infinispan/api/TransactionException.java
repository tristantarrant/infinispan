package org.infinispan.api;

/**
 * Unchecked exception wrapping transaction-related errors.
 *
 * @since 16.1
 */
public class TransactionException extends RuntimeException {

   public TransactionException(String message) {
      super(message);
   }

   public TransactionException(String message, Throwable cause) {
      super(message, cause);
   }

   public TransactionException(Throwable cause) {
      super(cause);
   }
}
