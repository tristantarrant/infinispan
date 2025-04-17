package org.infinispan.server.resp.scripting;

import java.util.function.Supplier;

import io.netty.util.internal.shaded.org.jctools.queues.MpmcArrayQueue;

/**
 * A pool for {@link LuaEngine} objects.
 */
public class EnginePool {
   private final MpmcArrayQueue<LuaEngine> pool;
   private final Supplier<LuaEngine> supplier;

   EnginePool(Supplier<LuaEngine> supplier, int min, int max) {
      this.supplier = supplier;
      pool = new MpmcArrayQueue<>(max);
      // Pre-fill the pool
      for (int i = 0; i < min; i++) {
         pool.add(this.supplier.get());
      }
   }

   LuaEngine borrow() {
      LuaEngine engine;
      if ((engine = pool.poll()) == null) {
         // We couldn't get an engine from the pool, create one now
         engine = supplier.get();
      }
      engine.pool = this;
      return engine;
   }

   void returnToPool(LuaEngine lua) {
      if (lua == null) {
         return;
      }
      if (lua.lua.getTop() > 0) {
         // This should never happen, but if it does it's our mistake
         throw new RuntimeException("Lua stack was not empty: " + lua.lua.getTop());
      }
      lua.pool = null;
      if (!this.pool.offer(lua)) {
         lua.shutdown();
      }
   }

   /**
    * Clears the pool
    */
   public void invalidate() {
      pool.drain(LuaEngine::shutdown);
   }

   /**
    * Shuts down this pool
    */
   public void shutdown() {
      invalidate();
   }
}
