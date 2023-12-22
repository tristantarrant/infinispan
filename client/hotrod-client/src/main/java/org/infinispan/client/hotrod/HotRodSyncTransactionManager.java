package org.infinispan.client.hotrod;

import org.infinispan.api.Experimental;
import org.infinispan.api.TransactionException;
import org.infinispan.api.TransactionStatus;
import org.infinispan.api.sync.SyncTransaction;
import org.infinispan.api.sync.SyncTransactionManager;

import jakarta.transaction.TransactionManager;

/**
 * @since 16.1
 */
@Experimental
final class HotRodSyncTransactionManager implements SyncTransactionManager {
   private final TransactionManager transactionManager;

   HotRodSyncTransactionManager(TransactionManager transactionManager) {
      this.transactionManager = transactionManager;
   }

   @Override
   public void begin() {
      try {
         transactionManager.begin();
      } catch (Exception e) {
         throw new TransactionException(e);
      }
   }

   @Override
   public void commit() {
      try {
         transactionManager.commit();
      } catch (Exception e) {
         throw new TransactionException(e);
      }
   }

   @Override
   public void rollback() {
      try {
         transactionManager.rollback();
      } catch (Exception e) {
         throw new TransactionException(e);
      }
   }

   @Override
   public HotRodSyncTransactionManager setRollbackOnly() {
      try {
         transactionManager.setRollbackOnly();
         return this;
      } catch (Exception e) {
         throw new TransactionException(e);
      }
   }

   @Override
   public HotRodSyncTransactionManager setTransactionTimeout(int seconds) {
      try {
         transactionManager.setTransactionTimeout(seconds);
         return this;
      } catch (Exception e) {
         throw new TransactionException(e);
      }
   }

   @Override
   public TransactionStatus getStatus() {
      try {
         return TransactionStatus.valueOf(transactionManager.getStatus());
      } catch (Exception e) {
         throw new TransactionException(e);
      }
   }

   @Override
   public SyncTransaction getTransaction() {
      try {
         jakarta.transaction.Transaction tx = transactionManager.getTransaction();
         return tx == null ? null : new HotRodSyncTransaction(tx);
      } catch (Exception e) {
         throw new TransactionException(e);
      }
   }

   @Override
   public SyncTransaction suspend() {
      try {
         jakarta.transaction.Transaction tx = transactionManager.suspend();
         return tx == null ? null : new HotRodSyncTransaction(tx);
      } catch (Exception e) {
         throw new TransactionException(e);
      }
   }

   @Override
   public void resume(SyncTransaction transaction) {
      try {
         transactionManager.resume(transaction.unwrap(jakarta.transaction.Transaction.class));
      } catch (Exception e) {
         throw new TransactionException(e);
      }
   }

   @SuppressWarnings("unchecked")
   @Override
   public <T> T unwrap(Class<T> clazz) {
      if (clazz.isInstance(transactionManager)) {
         return (T) transactionManager;
      }
      throw new IllegalArgumentException("Cannot unwrap to " + clazz);
   }
}
