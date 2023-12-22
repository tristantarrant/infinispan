package org.infinispan.embedded;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.infinispan.api.TransactionException;
import org.infinispan.api.TransactionStatus;
import org.infinispan.api.async.AsyncTransaction;

import jakarta.transaction.Transaction;

/**
 * Wraps a synchronous JTA {@link Transaction} behind the {@link AsyncTransaction} interface.
 *
 * @since 16.3
 */
final class EmbeddedAsyncTransaction implements AsyncTransaction {
   private final Transaction transaction;

   EmbeddedAsyncTransaction(Transaction transaction) {
      this.transaction = transaction;
   }

   @Override
   public CompletionStage<Void> commit() {
      try {
         transaction.commit();
         return CompletableFuture.completedFuture(null);
      } catch (Exception e) {
         return CompletableFuture.failedFuture(new TransactionException(e));
      }
   }

   @Override
   public CompletionStage<Void> rollback() {
      try {
         transaction.rollback();
         return CompletableFuture.completedFuture(null);
      } catch (Exception e) {
         return CompletableFuture.failedFuture(new TransactionException(e));
      }
   }

   @Override
   public CompletionStage<TransactionStatus> status() {
      try {
         return CompletableFuture.completedFuture(TransactionStatus.valueOf(transaction.getStatus()));
      } catch (Exception e) {
         return CompletableFuture.failedFuture(new TransactionException(e));
      }
   }

   @Override
   public EmbeddedAsyncTransaction rollbackOnly() {
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
