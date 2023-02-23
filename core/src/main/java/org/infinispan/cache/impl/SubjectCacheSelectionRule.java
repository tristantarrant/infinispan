package org.infinispan.cache.impl;

import java.security.Principal;

import javax.security.auth.Subject;

import org.infinispan.cache.CacheSelectionContext;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * @since 15.0
 **/
@ProtoTypeId(ProtoStreamTypeIds.SUBJECT_CACHE_SELECTION_RULE)
public class SubjectCacheSelectionRule implements CacheSelectionRule {
   @ProtoField(number = 1)
   final Operator operator;
   @ProtoField(number = 2)
   final String target;
   @ProtoField(number = 3)
   final String expression;
   private transient final Object expr;

   @ProtoFactory
   public SubjectCacheSelectionRule(Operator operator, String expression, String target) {
      this.operator = operator;
      this.expression = expression;
      this.expr = operator.prepareExpression(expression);
      this.target = target;
   }

   @Override
   public String evaluate(CacheSelectionContext context) {
      Subject subject = context.subject();
      if (subject != null) {
         for (Principal principal : subject.getPrincipals()) {
            String result = operator.evaluate(expr, principal.getName(), target);
            if (result != null) {
               return result;
            }
         }
      }
      return null;
   }

   @Override
   public Context context() {
      return Context.SUBJECT;
   }

   @Override
   public Operator condition() {
      return operator;
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
