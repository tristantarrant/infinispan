package org.infinispan.server.resp.scripting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.infinispan.commons.util.GlobUtils;
import org.infinispan.commons.util.concurrent.CompletableFutures;
import org.infinispan.scripting.ScriptingManager;
import org.infinispan.scripting.impl.ScriptMetadata;
import org.infinispan.server.resp.Resp3Handler;
import org.infinispan.tasks.Task;
import org.infinispan.tasks.TaskContext;
import org.infinispan.tasks.spi.TaskEngine;
import org.infinispan.util.concurrent.BlockingManager;

import io.netty.channel.ChannelHandlerContext;

/**
 * An Infinispan TaskEngine built specifically for executing RESP functions in the context of the resp connector.
 * It is therefore not a generic task engine or a scripting engine that can be used from Hot Rod or REST.
 */
public class EvalFunctionEngine implements TaskEngine {
   private final EnginePool pool;
   private final ScriptingManager scriptingManager;
   private final Map<String, FunctionLibrary> functionLibraries;
   private final Map<String, CodeFunction> allFunctions;

   public EvalFunctionEngine(ScriptingManager scriptingManager) {
      this.scriptingManager = scriptingManager;
      this.functionLibraries = new ConcurrentHashMap<>();
      this.allFunctions = new ConcurrentHashMap<>();
      this.pool = new EnginePool(() -> new LuaEngine(functionLibraries.values()), 0, 4);
   }

   public void shutdown() {
      pool.shutdown();
   }

   public CompletionStage<Object> fcall(Resp3Handler handler, ChannelHandlerContext ctx, String name, String[] keys, String[] args, boolean ro) {
      return handler.getBlockingManager().supplyBlocking(() -> {
         LuaEngine engine = pool.borrow();
         CodeFunction codeFunction = allFunctions.get(name);
         if (codeFunction == null) {
            throw new IllegalArgumentException("Function not found");
         }
         engine.runFunction(codeFunction, keys, args, ro);
         return engine;
      }, "fcall").handleAsync((luaCtx, t) -> {
         if (t != null) {
            handler.writer().error(t);
         } else {
            // Process the lua object on the stack and send it to the actual writer
            luaCtx.writeResponse(handler.writer());
            // Pop the error handler
            luaCtx.lua.pop(1);
            pool.returnToPool(luaCtx);
         }
         return null;
      }, ctx.channel().eventLoop());
   }

   public List<FunctionLibrary> functionList(String libraryPattern, boolean withCode) {
      Pattern pattern = Pattern.compile(GlobUtils.globToRegex(libraryPattern));
      List<FunctionLibrary> libraries = new ArrayList<>();
      for (FunctionLibrary library : functionLibraries.values()) {
         if (pattern.matcher(library.name()).matches()) {
            libraries.add(library);
         }
      }
      return libraries;
   }

   public void functionDelete(String lib) {
      functionLibraries.remove(lib);
      // Invalidate the engine pool
      pool.invalidate();
   }


   private static String libraryName(String name) {
      return "resp_function_" + name + ".lua";
   }

   public Code functionLoad(String script, boolean replace) {
      Map<String, String> properties = Code.parseShebang(script, true);
      String name = libraryName(properties.get("name"));
      if (scriptingManager.containsScript(name) && !replace) {
         throw new IllegalStateException("Library '" + name + "' already exists");
      }
      ScriptMetadata.Builder builder = new ScriptMetadata.Builder()
            .name(name)
            .extension(properties.get("engine").toLowerCase())
            .language("lua51") // FIXME: we should map engine to language
            .properties(properties);
      ScriptMetadata metadata = builder.build();
      Code code = Code.fromScript(script.substring(script.indexOf('\n') + 1), metadata);
      try (LuaEngine ctx = pool.borrow()) {
         // We need to execute the script to register the functions
         Map<String, CodeFunction> luaFunctions = ctx.registerFunctions(code);
         if (luaFunctions.isEmpty()) {
            throw new IllegalArgumentException("No functions registered");
         }
         allFunctions.putAll(luaFunctions);
         scriptingManager.addScript(name, code.code(), metadata);
         return code;
      } finally {
         pool.invalidate();
      }
   }

   // TaskEngine methods
   @Override
   public String getName() {
      return "resp-function-engine";
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
