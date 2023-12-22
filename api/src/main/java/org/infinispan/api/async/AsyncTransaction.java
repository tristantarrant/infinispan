package org.infinispan.api.async;

import java.util.concurrent.CompletionStage;

import org.infinispan.api.TransactionStatus;
import org.jspecify.annotations.Nullable;

/**
 * Represents a transaction with an async API. Does not depend on the Jakarta Transaction API directly.
 * Use {@link #unwrap(Class)} to obtain the underlying {@code jakarta.transaction.Transaction}.
 * <p>
 * The current implementation wraps synchronous JTA calls; a future version may provide truly non-blocking
 * transaction coordination.
 *
 * @since 16.3
 */
public interface AsyncTransaction {

   CompletionStage<Void> commit();

   CompletionStage<Void> rollback();

   CompletionStage<TransactionStatus> status();

   AsyncTransaction rollbackOnly();

   <T> @Nullable T unwrap(Class<T> clazz);
}
