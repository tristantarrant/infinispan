package org.infinispan.api.sync;

import org.infinispan.api.TransactionStatus;
import org.jspecify.annotations.Nullable;

/**
 * Represents a transaction. Does not depend on the Jakarta Transaction API directly.
 * Use {@link #unwrap(Class)} to obtain the underlying {@code jakarta.transaction.Transaction}.
 *
 * @since 16.3
 */
public interface SyncTransaction {

   void commit();

   void rollback();

   TransactionStatus status();

   SyncTransaction rollbackOnly();

   <T> @Nullable T unwrap(Class<T> clazz);
}
