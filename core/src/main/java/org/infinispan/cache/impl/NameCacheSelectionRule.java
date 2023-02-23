package org.infinispan.cache.impl;

import org.infinispan.cache.CacheSelectionContext;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * @since 15.0
 **/
@ProtoTypeId(ProtoStreamTypeIds.NAME_CACHE_SELECTION_RULE)
public class NameCacheSelectionRule implements CacheSelectionRule {
   @ProtoField(number = 1)
   final Operator operator;
   @ProtoField(number = 2)
   final String expression;
   @ProtoField(number = 3)
   final String target;

   private final transient Object expr;

   @ProtoFactory
   public NameCacheSelectionRule(Operator operator, String expression, String target) {
      this.operator = operator;
      this.expression = expression;
      this.expr = operator.prepareExpression(expression);
      this.target = target;
   }

   @Override
   public String evaluate(CacheSelectionContext context) {
      return operator.evaluate(expr, context.name(), target);
   }

   @Override
   public Context context() {
      return Context.NAME;
   }

   @Override
   public Operator condition() {
      return Operator.EQ;
   }

   @Override
   public String expression() {
      return expression;
   }

   @Override
   public String target() {
      return target;
   }
}
