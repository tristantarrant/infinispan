package org.infinispan.server.resp.scripting;

import static org.infinispan.server.resp.scripting.LuaEngine.sha1hex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import org.infinispan.commons.CacheException;
import org.infinispan.commons.util.concurrent.CompletableFutures;
import org.infinispan.scripting.ScriptingManager;
import org.infinispan.scripting.impl.ScriptMetadata;
import org.infinispan.scripting.impl.ScriptWithMetadata;
import org.infinispan.server.resp.Resp3Handler;
import org.infinispan.server.resp.serialization.ResponseWriter;
import org.infinispan.server.resp.tx.TransactionContext;
import org.infinispan.tasks.Task;
import org.infinispan.tasks.TaskContext;
import org.infinispan.tasks.spi.TaskEngine;
import org.infinispan.util.concurrent.BlockingManager;

import io.netty.channel.ChannelHandlerContext;

/**
 * An Infinispan TaskEngine built specifically for executing lua scripts in the context of the RESP connector.
 * It is therefore not a generic task engine or a scripting engine that can be used from Hot Rod or REST.
 */
public class EvalTaskEngine implements TaskEngine {
   private final EnginePool pool;
   private final ScriptingManager scriptingManager;

   public EvalTaskEngine(ScriptingManager scriptingManager) {
      this.scriptingManager = scriptingManager;
      this.pool = new EnginePool(LuaEngine::new, 2, 4);
   }

   public void shutdown() {
      pool.shutdown();
   }

   public CompletionStage<Void> eval(Resp3Handler handler, ChannelHandlerContext ctx, String code, String[] keys, String[] args, long flags) {
      return handler.getBlockingManager().supplyBlocking(() -> {
         LuaEngine engine = pool.borrow();
         try {
            Code script = scriptLoad(code, false);
            engine.registerScript(script);
            runScript(engine, handler, ctx, script, keys, args, flags);
            engine.unregisterScript(script);
            return engine;
         } catch (Throwable t) {
            // a throwable here means it was not handled properly by the script execution logic. We discard the lua
            // context since it may be in an unrecoverable state
            engine.shutdown();
            handler.writer().error(t);
            return null;
         }
      }, "eval").thenApplyAsync(engine -> {
         if (engine != null) {
            // Process the lua object on the stack and send it to the actual writer
            engine.writeResponse(handler.writer());
            // Pop the error handler
            engine.lua.pop(1);
            // Return the lua context to the pool
            pool.returnToPool(engine);
         }
         return null;
      }, ctx.channel().eventLoop());
   }

   public CompletionStage<Void> evalSha(Resp3Handler handler, ChannelHandlerContext ctx, String sha, String[] keys, String[] args, long flags) {
      return handler.getBlockingManager().supplyBlocking(() -> {
         LuaEngine engine = pool.borrow();
         ScriptWithMetadata script;
         try {
            script = scriptingManager.getScriptWithMetadata(scriptName(sha.toUpperCase()));
         } catch (CacheException e) {
            pool.returnToPool(engine);
            throw new RuntimeException("NOSCRIPT No matching script. Please use EVAL.");
         }
         try {
            Code code = Code.fromScript(script);
            engine.registerScript(code);
            runScript(engine, handler, ctx, code, keys, args, flags);
            return engine;
         } catch (Throwable t) {
            // a throwable here means it was not handled properly by the script execution logic. We discard the lua
            // context since it may be in an unrecoverable state
            engine.shutdown();
            handler.writer().error(t);
            return null;
         }
      }, "evalsha").thenApplyAsync(engine -> {
         if (engine != null) {
            // Process the lua object on the stack and send it to the actual writer
            engine.writeResponse(handler.writer());
            // Pop the error handler
            engine.lua.pop(1);
            pool.returnToPool(engine);
         }
         return null;
      }, ctx.channel().eventLoop());
   }

   private void runScript(LuaEngine engine, Resp3Handler handler, ChannelHandlerContext ctx, Code script, String[] keys, String[] args, long flags) {
      engine.handler = handler;
      engine.ctx = ctx;
      engine.flags = flags;
      engine.flags |= script.flags();
      ResponseWriter oldWriter = handler.writer(engine.newResponseWriter());
      try {
         TransactionContext.startTransactionContext(ctx);
         engine.runScript(script, keys, args);
      } catch (Throwable t) {
         throw new RuntimeException(t);
      } finally {
         TransactionContext.endTransactionContext(ctx);
         handler.writer(oldWriter);
      }
   }

   static String fName(String sha) {
      return "f_" + sha;
   }

   public Code scriptLoad(String script, boolean persistent) {
      Map<String, String> properties = Code.parseShebang(script, false);
      String sha = sha1hex(script);
      properties.put("sha", sha);
      String name = scriptName(sha);
      ScriptMetadata.Builder builder = new ScriptMetadata.Builder()
            .name(name)
            .extension("lua")
            .language("lua51")
            .properties(properties);
      ScriptMetadata metadata = builder.build();
      if (persistent) {
         scriptingManager.addScript(name, script, metadata);
      }
      return Code.fromScript(script, metadata);
   }

   private static String scriptName(String sha) {
      return "resp_script_" + sha + ".lua";
   }

   public List<Integer> scriptExists(List<String> shas) {
      List<Integer> exists = new ArrayList<>(shas.size());
      Set<String> names = scriptingManager.getScriptNames();
      for (String sha : shas) {
         exists.add(names.contains(scriptName(sha)) ? 1 : 0);
      }
      return exists;
   }

   public void scriptFlush() {
      Set<String> names = scriptingManager.getScriptNames();
      for (String name : names) {
         if (name.startsWith("resp_script_")) {
            scriptingManager.removeScript(name);
         }
      }
   }

   // TaskEngine methods
   @Override
   public String getName() {
      return "resp-eval-engine";
   }

   @Override
   public List<Task> getTasks() {
      return List.of();
   }

   @Override
   public <T> CompletionStage<T> runTask(String taskName, TaskContext context, BlockingManager blockingManager) {
      return CompletableFutures.completedNull();
   }

   @Override
   public boolean handles(String taskName) {
      return false;
   }
}
