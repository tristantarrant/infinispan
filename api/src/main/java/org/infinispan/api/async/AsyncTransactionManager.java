package org.infinispan.api.async;

import java.util.concurrent.CompletionStage;

import org.infinispan.api.TransactionStatus;
import org.jspecify.annotations.Nullable;

/**
 * An async transaction manager that does not depend on the Jakarta Transaction API directly.
 * Use {@link #unwrap(Class)} to obtain the underlying {@code jakarta.transaction.TransactionManager}.
 * <p>
 * The current implementation wraps synchronous JTA calls; a future version may provide truly non-blocking
 * transaction coordination.
 *
 * @since 16.1
 */
public interface AsyncTransactionManager {

   CompletionStage<Void> begin();

   CompletionStage<Void> commit();

   CompletionStage<Void> rollback();

   AsyncTransactionManager rollbackOnly();

   AsyncTransactionManager timeout(int seconds);

   CompletionStage<TransactionStatus> getStatus();

   @Nullable AsyncTransaction getTransaction();

   CompletionStage<AsyncTransaction> suspend();

   CompletionStage<Void> resume(AsyncTransaction transaction);

   <T> @Nullable T unwrap(Class<T> clazz);
}
