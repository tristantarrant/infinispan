package org.infinispan.embedded.impl;

import org.infinispan.api.common.CacheOptions;
import org.infinispan.api.common.process.CacheEntryProcessorContext;
import org.infinispan.api.common.process.CacheProcessorOptions;

/**
 * @since 16.3
 */
public class EmbeddedCacheEntryProcessorContext implements CacheEntryProcessorContext {
   private final Object[] arguments;
   private final CacheOptions options;

   public EmbeddedCacheEntryProcessorContext(CacheProcessorOptions options) {
      this.arguments = options.arguments();
      this.options = options;
   }

   public EmbeddedCacheEntryProcessorContext(CacheOptions options) {
      this.arguments = null;
      this.options = options;
   }

   @Override
   public Object[] arguments() {
      return arguments;
   }

   @Override
   public CacheOptions options() {
      return options;
   }
}
