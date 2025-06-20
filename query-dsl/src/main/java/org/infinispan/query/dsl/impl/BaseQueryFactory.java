package org.infinispan.query.dsl.impl;

import java.lang.invoke.MethodHandles;

import org.infinispan.query.dsl.Expression;
import org.infinispan.query.dsl.FilterConditionEndContext;
import org.infinispan.query.dsl.QueryFactory;
import org.infinispan.query.dsl.impl.logging.Log;
import org.jboss.logging.Logger;

/**
 * @author anistor@redhat.com
 * @since 6.0
 */
public abstract class BaseQueryFactory implements QueryFactory {

   private static final Log log = Logger.getMessageLogger(MethodHandles.lookup(), Log.class, BaseQueryFactory.class.getName());

   private FilterConditionEndContext having(Expression expression) {
      return new AttributeCondition(this, expression);
   }

}
