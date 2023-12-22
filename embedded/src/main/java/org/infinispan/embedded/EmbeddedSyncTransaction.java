package org.infinispan.embedded;

import org.infinispan.api.TransactionException;
import org.infinispan.api.TransactionStatus;
import org.infinispan.api.sync.SyncTransaction;

import jakarta.transaction.Transaction;

/**
 * @since 16.3
 */
final class EmbeddedSyncTransaction implements SyncTransaction {
   private final Transaction transaction;

   EmbeddedSyncTransaction(Transaction transaction) {
      this.transaction = transaction;
   }

   @Override
   public void commit() {
      try {
         transaction.commit();
      } catch (Exception e) {
         throw new TransactionException(e);
      }
   }

   @Override
   public void rollback() {
      try {
         transaction.rollback();
      } catch (Exception e) {
         throw new TransactionException(e);
      }
   }

   @Override
   public TransactionStatus status() {
      try {
         return TransactionStatus.valueOf(transaction.getStatus());
      } catch (Exception e) {
         throw new TransactionException(e);
      }
   }

   @Override
   public EmbeddedSyncTransaction rollbackOnly() {
      try {
         transaction.setRollbackOnly();
         return this;
      } catch (Exception e) {
         throw new TransactionException(e);
      }
   }

   @SuppressWarnings("unchecked")
   @Override
   public <T> T unwrap(Class<T> clazz) {
      if (clazz.isInstance(transaction)) {
         return (T) transaction;
      }
      throw new IllegalArgumentException("Cannot unwrap to " + clazz);
   }
}
