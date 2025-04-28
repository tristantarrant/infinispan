package org.infinispan.server.resp.scripting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.infinispan.commons.util.GlobUtils;
import org.infinispan.commons.util.concurrent.CompletableFutures;
import org.infinispan.scripting.ScriptingManager;
import org.infinispan.scripting.impl.ScriptMetadata;
import org.infinispan.scripting.impl.ScriptWithMetadata;
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
public class FunctionTaskEngine implements TaskEngine {
   private final EnginePool pool;
   private final ScriptingManager scriptingManager;
   private final Map<String, FunctionLibrary> functionLibraries;
   private final Map<String, CodeFunction> allFunctions;

   public FunctionTaskEngine(ScriptingManager scriptingManager) {
      this.functionLibraries = new ConcurrentHashMap<>();
      this.allFunctions = new ConcurrentHashMap<>();
      this.pool = new EnginePool(() -> new LuaEngine(functionLibraries.values()), 0, 4);
      this.scriptingManager = scriptingManager;
      this.scriptingManager.onScriptUpdate(name -> name.startsWith("resp_function_"), name -> rebuild());
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

   public List<Map<String, Object>> functionList(String libraryPattern, boolean withCode) {
      Pattern pattern = Pattern.compile(GlobUtils.globToRegex(libraryPattern));
      List<Map<String, Object>> list = new ArrayList<>();
      for (FunctionLibrary library : functionLibraries.values()) {
         if (pattern.matcher(library.name()).matches()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("library_name", library.name());
            map.put("engine", "LUA");
            List<Map<String, Object>> functions = new ArrayList<>();
            for (CodeFunction codeFunction : library.functions().values()) {
               Map<String, Object> function = new LinkedHashMap<>();
               function.put("name", codeFunction.name());
               function.put("description", codeFunction.description());
               Set<String> flags = new LinkedHashSet<>();
               for (ScriptFlags f : ScriptFlags.values()) {
                  if (f.isSet(codeFunction.flags())) {
                     flags.add(f.name().toLowerCase().replace('_', '-'));
                  }
               }
               function.put("flags", flags);
               functions.add(function);
            }
            map.put("functions", functions);
            if (withCode) {
               map.put("library_code", scriptingManager.getScript(libraryName(library.name())));
            }
            list.add(map);
         }
      }
      return list;
   }

   public void functionDelete(String lib) {
      functionLibraries.remove(lib);
      // Invalidate the engine pool
      pool.invalidate();
   }

   public CharSequence functionDump() {
      throw new UnsupportedOperationException("Not yet implemented.");
   }

   public void functionFlush() {
      functionLibraries.clear();
      // Invalidate the engine pool
      pool.invalidate();
   }

   public void functionKill() {
      throw new UnsupportedOperationException("Not yet implemented.");
   }

   public void functionRestore(String dump) {
      throw new UnsupportedOperationException("Not yet implemented.");
   }

   public Map<String, Object> functionStats() {
      Map<String, Object> stats = new LinkedHashMap<>();
      stats.put("running_script", null);
      Map<String, Map<String, Object>> engines = new LinkedHashMap<>();
      Map<String, Object> engineStats = new LinkedHashMap<>();
      engineStats.put("libraries_count", functionLibraries.size());
      engineStats.put("functions_count", allFunctions.size());
      engines.put("LUA", engineStats);
      stats.put("engines", engines);
      return stats;
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
            .language("lua51") // TODO: we should map engine to language
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

   public void rebuild() {
      for (String scriptName : scriptingManager.getScriptNames()) {
         ScriptWithMetadata scriptWithMetadata = scriptingManager.getScriptWithMetadata(scriptName);
         
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
