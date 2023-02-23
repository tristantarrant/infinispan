package org.infinispan.client.rest;

/**
 * @since 15.0
 **/
public class CacheSelectionRule {
   public enum Context {
      NAME,
      SUBJECT
   }
   public enum Operator {
      EQ,
      REGEX
   }
   private final Context context;
   private final Operator operator;
   private final String expression;
   private final String target;

   public CacheSelectionRule(Context context, Operator operator, String expression, String target) {
      this.context = context;
      this.operator = operator;
      this.expression = expression;
      this.target = target;
   }

   public Context getDiscriminator() {
      return context;
   }

   public Operator getCondition() {
      return operator;
   }

   public String getExpression() {
      return expression;
   }

   public String getTarget() {
      return target;
   }
}
