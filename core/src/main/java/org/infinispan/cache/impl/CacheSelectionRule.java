package org.infinispan.cache.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.infinispan.cache.CacheSelectionContext;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.protostream.annotations.ProtoEnumValue;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * @since 15.0
 **/
public interface CacheSelectionRule {

   Context context();

   Operator condition();

   String expression();

   String target();

   String evaluate(CacheSelectionContext context);

   static CacheSelectionRule of(Context context, Operator operator, String expression, String target) {
      switch (context) {
         case NAME:
            return new NameCacheSelectionRule(operator, expression, target);
         case SUBJECT:
            return new SubjectCacheSelectionRule(operator, expression, target);
         default:
            throw new IllegalArgumentException(context.name());
      }
   }

   enum Context {
      NAME,
      SUBJECT
   }

   @ProtoTypeId(ProtoStreamTypeIds.CACHE_SELECTION_OPERATOR)
   enum Operator {
      @ProtoEnumValue(number = 0)
      EQ {
         @Override
         public Object prepareExpression(String expression) {
            return expression;
         }

         @Override
         public String evaluate(Object expression, String input, String target) {
            if (expression.equals(input)) {
               return target;
            } else {
               return null;
            }
         }
      },
      @ProtoEnumValue(number = 1)
      REGEX {
         @Override
         public Object prepareExpression(String expression) {
            return Pattern.compile(expression);
         }

         @Override
         public String evaluate(Object expression, String input, String target) {
            Matcher matcher = ((Pattern) expression).matcher(input);
            if (matcher.matches()) {
               return matcher.replaceAll(target);
            } else {
               return null;
            }
         }
      };

      public abstract String evaluate(Object expression, String input, String target);

      public abstract Object prepareExpression(String expression);
   }
}
