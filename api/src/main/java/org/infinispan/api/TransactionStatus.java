package org.infinispan.api;

/**
 * Transaction status codes, mirroring the values defined by the Jakarta Transaction specification.
 *
 * @since 16.3
 */
public enum TransactionStatus {
   ACTIVE(0),
   MARKED_ROLLBACK(1),
   PREPARED(2),
   COMMITTED(3),
   ROLLEDBACK(4),
   UNKNOWN(5),
   NO_TRANSACTION(6),
   PREPARING(7),
   COMMITTING(8),
   ROLLING_BACK(9);

   private final int code;

   TransactionStatus(int code) {
      this.code = code;
   }

   public int code() {
      return code;
   }

   public static TransactionStatus valueOf(int code) {
      return switch (code) {
         case 0 -> ACTIVE;
         case 1 -> MARKED_ROLLBACK;
         case 2 -> PREPARED;
         case 3 -> COMMITTED;
         case 4 -> ROLLEDBACK;
         case 5 -> UNKNOWN;
         case 6 -> NO_TRANSACTION;
         case 7 -> PREPARING;
         case 8 -> COMMITTING;
         case 9 -> ROLLING_BACK;
         default -> throw new IllegalArgumentException("Unknown transaction status code: " + code);
      };
   }
}
