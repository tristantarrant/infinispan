package org.infinispan.api.sync;

import org.infinispan.api.TransactionStatus;
import org.jspecify.annotations.Nullable;

/**
 * A transaction manager that does not depend on the Jakarta Transaction API directly.
 * Use {@link #unwrap(Class)} to obtain the underlying {@code jakarta.transaction.TransactionManager}.
 *
 * @since 16.1
 */
public interface SyncTransactionManager {

   void begin();

   void commit();

   void rollback();

   SyncTransactionManager setRollbackOnly();

   SyncTransactionManager setTransactionTimeout(int seconds);

   TransactionStatus getStatus();

   @Nullable SyncTransaction getTransaction();

   @Nullable SyncTransaction suspend();

   void resume(SyncTransaction transaction);

   <T> @Nullable T unwrap(Class<T> clazz);
}
