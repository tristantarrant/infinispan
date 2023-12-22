package org.infinispan.client.hotrod;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.infinispan.api.Experimental;
import org.infinispan.api.TransactionException;
import org.infinispan.api.TransactionStatus;
import org.infinispan.api.async.AsyncTransaction;
import org.infinispan.api.async.AsyncTransactionManager;

import jakarta.transaction.TransactionManager;

/**
 * Wraps a synchronous JTA {@link TransactionManager} behind the {@link AsyncTransactionManager} interface.
 *
 * @since 16.1
 */
@Experimental
final class HotRodAsyncTransactionManager implements AsyncTransactionManager {
   private final TransactionManager transactionManager;

   HotRodAsyncTransactionManager(TransactionManager transactionManager) {
      this.transactionManager = transactionManager;
   }

   @Override
   public CompletionStage<Void> begin() {
      try {
         transactionManager.begin();
         return CompletableFuture.completedFuture(null);
      } catch (Exception e) {
         return CompletableFuture.failedFuture(new TransactionException(e));
      }
   }

   @Override
   public CompletionStage<Void> commit() {
      try {
         transactionManager.commit();
         return CompletableFuture.completedFuture(null);
      } catch (Exception e) {
         return CompletableFuture.failedFuture(new TransactionException(e));
      }
   }

   @Override
   public CompletionStage<Void> rollback() {
      try {
         transactionManager.rollback();
         return CompletableFuture.completedFuture(null);
      } catch (Exception e) {
         return CompletableFuture.failedFuture(new TransactionException(e));
      }
   }

   @Override
   public HotRodAsyncTransactionManager rollbackOnly() {
      try {
         transactionManager.setRollbackOnly();
         return this;
      } catch (Exception e) {
         throw new TransactionException(e);
      }
   }

   @Override
   public HotRodAsyncTransactionManager timeout(int seconds) {
      try {
         transactionManager.setTransactionTimeout(seconds);
         return this;
      } catch (Exception e) {
         throw new TransactionException(e);
      }
   }

   @Override
   public CompletionStage<TransactionStatus> getStatus() {
      try {
         return CompletableFuture.completedFuture(TransactionStatus.valueOf(transactionManager.getStatus()));
      } catch (Exception e) {
         return CompletableFuture.failedFuture(new TransactionException(e));
      }
   }

   @Override
   public AsyncTransaction getTransaction() {
      try {
         jakarta.transaction.Transaction tx = transactionManager.getTransaction();
         return tx == null ? null : new HotRodAsyncTransaction(tx);
      } catch (Exception e) {
         throw new TransactionException(e);
      }
   }

   @Override
   public CompletionStage<AsyncTransaction> suspend() {
      try {
         jakarta.transaction.Transaction tx = transactionManager.suspend();
         return CompletableFuture.completedFuture(tx == null ? null : new HotRodAsyncTransaction(tx));
      } catch (Exception e) {
         return CompletableFuture.failedFuture(new TransactionException(e));
      }
   }

   @Override
   public CompletionStage<Void> resume(AsyncTransaction transaction) {
      try {
         transactionManager.resume(transaction.unwrap(jakarta.transaction.Transaction.class));
         return CompletableFuture.completedFuture(null);
      } catch (Exception e) {
         return CompletableFuture.failedFuture(new TransactionException(e));
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
