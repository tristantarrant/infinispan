package org.infinispan.server.resp.scripting;

import static org.infinispan.server.resp.scripting.EvalTaskEngine.fName;
import static party.iroiro.luajava.lua51.Lua51Consts.LUA_GLOBALSINDEX;
import static party.iroiro.luajava.lua51.Lua51Consts.LUA_REGISTRYINDEX;

import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import org.infinispan.commons.CacheListenerException;
import org.infinispan.commons.util.Util;
import org.infinispan.commons.util.Version;
import org.infinispan.remoting.RemoteException;
import org.infinispan.server.resp.AclCategory;
import org.infinispan.server.resp.Resp3Handler;
import org.infinispan.server.resp.RespCommand;
import org.infinispan.server.resp.RespRequestHandler;
import org.infinispan.server.resp.RespVersion;
import org.infinispan.server.resp.logging.Log;
import org.infinispan.server.resp.scripting.lua.LuaArray;
import org.infinispan.server.resp.scripting.lua.LuaMap;
import org.infinispan.server.resp.scripting.lua.LuaSet;
import org.infinispan.server.resp.serialization.ResponseWriter;
import org.infinispan.server.resp.serialization.SerializationHint;
import org.infinispan.server.resp.serialization.lua.LuaResponseWriter;
import org.jboss.logging.Logger;

import io.netty.channel.ChannelHandlerContext;
import party.iroiro.luajava.JFunction;
import party.iroiro.luajava.Lua;
import party.iroiro.luajava.lua51.Lua51;
import party.iroiro.luajava.lua51.Lua51Consts;

/**
 * LuaEngine manages a {@link Lua} instance.
 */
public class LuaEngine implements AutoCloseable {
   public enum Mode {
      USER,
      LOAD
   }

   private static final String REDIS_API_NAME = "redis";

   private final static String[] LIBRARIES_ALLOW_LIST = {"string", "math", "table", "os"}; // bit, cjson, cmsgpack, struct are not available in luajava
   private final static String[] REDIS_API_ALLOW_LIST = {"redis", "__redis__err__handler"};
   private final static String[] LUA_BUILTINS_ALLOW_LIST = {"xpcall", "tostring", "getfenv", "setmetatable", "next", "assert", "tonumber", "rawequal", "collectgarbage", "getmetatable", "rawset", "pcall", "coroutine", "type", "_G", "select", "unpack", "gcinfo", "pairs", "rawget", "loadstring", "ipairs", "_VERSION", "setfenv", "load", "error"};
   private final static String[] LUA_BUILTINS_NOT_DOCUMENTED_ALLOW_LIST = {"newproxy"};
   private final static String[] LUA_BUILTINS_REMOVED_AFTER_INITIALIZATION_ALLOW_LIST = {"debug"};
   private final static Set<String> ALLOW_LISTS;
   private final static Set<String> DENY_LIST = Set.of("dofile", "loadfile", "print");

   public static final int LOG_DEBUG = 0; // TRACE
   public static final int LOG_VERBOSE = 1; // DEBUG
   public static final int LOG_NOTICE = 2; // INFO
   public static final int LOG_WARNING = 3; // WARN
   private static final Logger.Level[] LEVEL_MAP = {Logger.Level.TRACE, Logger.Level.DEBUG, Logger.Level.INFO, Logger.Level.WARN};

   public static final int PROPAGATE_AOF = 1;
   public static final int PROPAGATE_REPL = 2;
   public static final int PROPAGATE_NONE = 0;
   public static final int PROPAGATE_ALL = PROPAGATE_AOF | PROPAGATE_REPL;

   static {
      ALLOW_LISTS = new HashSet<>();
      ALLOW_LISTS.addAll(Arrays.asList(LIBRARIES_ALLOW_LIST));
      ALLOW_LISTS.addAll(Arrays.asList(REDIS_API_ALLOW_LIST));
      ALLOW_LISTS.addAll(Arrays.asList(LUA_BUILTINS_ALLOW_LIST));
      ALLOW_LISTS.addAll(Arrays.asList(LUA_BUILTINS_NOT_DOCUMENTED_ALLOW_LIST));
      ALLOW_LISTS.addAll(Arrays.asList(LUA_BUILTINS_REMOVED_AFTER_INITIALIZATION_ALLOW_LIST));
   }

   final Lua lua;

   // context variables
   long flags;
   Resp3Handler handler;
   ChannelHandlerContext ctx;
   Mode mode = Mode.USER;
   EnginePool pool;
   final Map<String, Integer> localFunctionRefs; // Stores the function refs for this LuaContext
   Map<String, CodeFunction> functions; // A "global" object used to return function information between the register_function callback and the registerFunctions() method

   LuaEngine() {
      lua = new Lua51();
      for (String lib : LIBRARIES_ALLOW_LIST) {
         lua.openLibrary(lib);
      }
      lua.openLibrary("debug");
      installMathRandom();
      installErrorHandler();
      installRedisAPI();
      luaSetErrorMetatable();
      localFunctionRefs = new HashMap<>();
   }

   LuaEngine(Collection<FunctionLibrary> libraries) {
      this();

   }

   public ResponseWriter newResponseWriter() {
      return new LuaResponseWriter(lua);
   }

   private void installRedisAPI() {
      lua.newTable();

      // redis.sha1hex(string)
      tableAdd(lua, "sha1hex", l -> {
         int argc = l.getTop();
         if (argc != 1) {
            l.error("wrong number of arguments");
         }
         String hex = sha1hex(l.toString(1));
         l.push(hex);
         return 1;
      });
      // redis.register_function(string, function() ... )
      tableAdd(lua, "register_function", this::registerFunction);
      // redis.call(string, ...)
      tableAdd(lua, "call", l -> executeRespCommand(l, true));
      // redis.pcall(string, ...)
      tableAdd(lua, "pcall", l -> executeRespCommand(l, false));
      // redis.setresp(int)
      tableAdd(lua, "setresp", l -> {
         int argc = l.getTop();
         if (argc != 1) {
            l.error("redis.setresp() requires one argument.");
         }
         try {
            handler.writer().version(RespVersion.of((int) l.toInteger(-argc)));
         } catch (IllegalArgumentException e) {
            l.error("RESP version must be 2 or 3.");
         }
         return 0;
      });
      // redis.error_reply(string)
      tableAdd(lua, "error_reply", l -> {
         if (l.getTop() != 1 || l.type(-1) != Lua.LuaType.STRING) {
            l.error("wrong number or type of arguments");
            return 1;
         }
         String err = l.toString(-1);
         if (!err.startsWith("-")) {
            err = "-" + err;
         }
         luaPushError(lua, err);
         return 1;
      });
      // redis.status_reply(string)
      tableAdd(lua, "status_reply", l -> {
         if (l.getTop() != 1 || l.type(-1) != Lua.LuaType.STRING) {
            l.error("wrong number or type of arguments");
         }
         l.newTable();
         l.push("ok");
         l.pushValue(-3);
         l.setTable(-3);
         return 1;
      });
      // redis.set_repl(int)
      tableAdd(lua, "set_repl", l -> {
         int argc = l.getTop();
         if (argc != 1) {
            l.error("redis.set_repl() requires one argument.");
         }
         long flags = l.toInteger(-1);
         if ((flags & ~PROPAGATE_ALL) != 0) {
            l.error("Invalid replication flags. Use REPL_AOF, REPL_REPLICA, REPL_ALL or REPL_NONE.");
         }
         // TODO: set repl flags
         return 0;
      });
      tableAdd(lua, "REPL_NONE", PROPAGATE_NONE);
      tableAdd(lua, "REPL_AOF", PROPAGATE_AOF);
      tableAdd(lua, "REPL_SLAVE", PROPAGATE_REPL);
      tableAdd(lua, "REPL_REPLICA", PROPAGATE_REPL);
      tableAdd(lua, "REPL_ALL", PROPAGATE_ALL);
      tableAdd(lua, "log", l -> {
         int j, argc = l.getTop();
         if (argc < 2) {
            luaPushError(lua, "redis.log() requires two arguments or more.");
            return -1;
         } else if (!l.isNumber(-argc)) {
            luaPushError(lua, "First argument must be a number (log level).");
            return -1;
         }
         int level = (int) l.toInteger(-argc);
         if (level < LOG_DEBUG || level > LOG_WARNING) {
            luaPushError(lua, "Invalid log level.");
            return -1;
         }
         StringBuilder sb = new StringBuilder();
         for (j = 1; j < argc; j++) {
            sb.append(l.toString(j - argc));
         }
         Log.SERVER.log(LEVEL_MAP[level], sb);
         return 0;
      });
      tableAdd(lua, "LOG_DEBUG", LOG_DEBUG);
      tableAdd(lua, "LOG_VERBOSE", LOG_VERBOSE);
      tableAdd(lua, "LOG_NOTICE", LOG_NOTICE);
      tableAdd(lua, "LOG_WARNING", LOG_WARNING);
      tableAdd(lua, "REDIS_VERSION_NUM", Version.getVersionShort());
      tableAdd(lua, "REDIS_VERSION", Version.getVersion());
      // Give a name to the table
      lua.setGlobal(REDIS_API_NAME);
   }

   private void installMathRandom() {
      lua.getGlobal("math");
      lua.push("random");
      lua.push(l -> {
         switch (l.getTop()) {
            case 0: {
               lua.push(handler.respServer().random().nextDouble());
               break;
            }
            case 1: {
               long upper = lua.toInteger(1);
               if (upper <= 1) {
                  lua.error("interval is empty");
               }
               lua.push(handler.respServer().random().nextLong(1, upper));
               break;
            }
            case 2: {
               long lower = lua.toInteger(1);
               long upper = lua.toInteger(2);
               lua.push(handler.respServer().random().nextLong(lower, upper));
               break;
            }
            default:
               lua.error("wrong number of arguments");
         }
         return 1;
      });
      lua.setTable(-3);
      lua.push("randomseed");
      lua.push(l -> {
         handler.respServer().random().setSeed(l.toInteger(1));
         return 0;
      });
      lua.setTable(-3);
      lua.setGlobal("math");
   }

   private void installErrorHandler() {
      String err_handler = """
            -- copy the `debug` global to a local, and nil it so it cannot be used by user scripts
            local dbg = debug
            debug = nil
            function __redis__err__handler(err)
              -- get debug information for the previous call (type, source and line)
              local i = dbg.getinfo(2,'nSl')
              -- if it was a native call, get the information for the previous element in the stack
              if i and i.what == 'C' then
                i = dbg.getinfo(3,'nSl')
              end
              if type(err) ~= 'table' then
                err = {err='ERR ' .. tostring(err)}
              end
              if i then
                err['source'] = i.source
                err['line'] = i.currentline
              end
              return err
            end
            """;
      byte[] bytes = err_handler.getBytes(StandardCharsets.US_ASCII);
      ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
      buffer.put(bytes);
      lua.load(buffer, "@err_handler_def");
      lua.pCall(0, 0);
   }

   public int executeRespCommand(Lua l, boolean raiseError) {
      int argc = l.getTop();
      String command = l.toString(-argc);
      RespCommand respCommand = RespCommand.fromString(command);
      if (respCommand == null) {
         l.push("Unknown Redis command called from script");
         return -1;
      }
      long commandMask = respCommand.aclMask();
      if (AclCategory.CONNECTION.matches(commandMask)) {
         l.push("This Redis command is not allowed from script");
         return -1;
      }
      if (ScriptFlags.NO_WRITES.isSet(flags) && AclCategory.WRITE.matches(commandMask)) {
         l.push("Write commands are not allowed from read-only scripts.");
         return -1;
      }
      List<byte[]> args = new ArrayList<>(argc - 1);
      for (int i = -argc + 1; i < 0; i++) {
         args.add(l.toString(i).getBytes(StandardCharsets.US_ASCII));
      }
      CompletableFuture<RespRequestHandler> future = handler.handleRequest(ctx, respCommand, args).toCompletableFuture();
      try {
         future.get(); // TODO: handle timeouts ?
      } catch (Throwable t) {
         Throwable cause = filterCause(t);
         Log.SERVER.debugf(cause, "Error while processing command '%s'", respCommand);
         return -1;
      }
      if (lua.type(-1) == Lua.LuaType.TABLE) {
         lua.push("err");
         lua.rawGet(-2);
         if (lua.type(-1) == Lua.LuaType.STRING && raiseError) {
            String error = lua.toString(-1);
            lua.pop(2);
            lua.error(error);
         }
         lua.pop(1);
      }
      return 1;
   }

   public static Throwable filterCause(Throwable re) {
      if (re == null) return null;
      Class<? extends Throwable> tClass = re.getClass();
      Throwable cause = re.getCause();
      if (cause != null && (tClass == ExecutionException.class || tClass == CompletionException.class || tClass == InvocationTargetException.class || tClass == RemoteException.class || tClass == RuntimeException.class || tClass == CacheListenerException.class))
         return filterCause(cause);
      else
         return re;
   }

   public int registerFunction(Lua l) {
      if (mode != Mode.LOAD) {
         lua.error("redis.register_function can only be called on FUNCTION LOAD command");
      }
      int argc = l.getTop();
      if (argc < 1 || argc > 2) {
         l.error("wrong number of arguments to redis.register_function");
      }
      String name = null;
      String description = null;
      int functionRef = 0;
      long flags = 0;
      if (argc == 1) {
         if (!l.isTable(1)) {
            l.error("calling redis.register_function with a single argument is only applicable to Lua table (representing named arguments).");
         }
         l.pushNil();
         while (l.next(-2) > 0) {
            if (!l.isString(-2)) {
               l.error("named argument key given to redis.register_function is not a string");
            }
            String key = l.toString(-2);
            switch (key) {
               case "function_name":
                  if (!l.isString(-1)) {
                     l.error("function_name argument given to redis.register_function must be a string");
                  }
                  name = l.toString(-1);
                  break;
               case "description":
                  if (!l.isString(-1)) {
                     l.error("description argument given to redis.register_function must be a string");
                  }
                  description = l.toString(-1);
                  break;
               case "callback":
                  if (!l.isFunction(-1)) {
                     l.error("callback argument given to redis.register_function must be a function");
                  }
                  functionRef = l.ref();
                  break;
               case "flags":
                  if (!l.isTable(-1)) {
                     l.error("flags argument to redis.register_function must be a table representing function flags");
                  }
                  // read the flags table
                  for (int j = 1; ; j++) {
                     l.push(j);
                     l.getTable(-2);
                     if (l.isNil(-1)) {
                        l.pop(1);
                        break;
                     }
                     if (!l.isString(-1)) {
                        l.pop(1);
                        l.error("unknown flag given");
                     }
                     String flag = l.toString(-1);
                     flags |= ScriptFlags.valueOf(flag.toUpperCase()).value();
                     l.pop(1); // pop and iterate
                  }
                  break;
               default:
                  lua.error("unknown argument given to redis.register_function");
                  break;
            }
            lua.pop(1);
         }
      } else {
         if (!l.isString(1)) {
            l.error("first argument to redis.register_function must be a string");
         }
         if (!l.isFunction(2)) {
            l.error("second argument to redis.register_function must be a function");
         }
         name = l.toString(1);
         functionRef = l.ref();
      }
      if (name == null) {
         lua.error("redis.register_function must get a function name argument");
      }
      if (functionRef == 0) {
         lua.error("redis.register_function must get a callback argument");
      }
      if (localFunctionRefs.containsKey(name)) {
         lua.error("Function " + name + " already exists");
      }
      localFunctionRefs.put(name, functionRef);
      functions.put(name, new CodeFunction(name, description, flags));
      return 0;
   }


   /**
    * Returns this instance to the pool it was borrowed from
    */
   @Override
   public void close() {
      if (pool != null) {
         pool.returnToPool(this);
      }
   }

   /**
    * Releases the Lua context. This is only called by {@link EnginePool}
    */
   void shutdown() {
      pool = null;
      lua.close();
   }

   // Internal methods

   private static void tableAdd(Lua lua, String name, JFunction function) {
      lua.push(name);
      lua.push(function);
      lua.setTable(-3);
   }

   private static void tableAdd(Lua lua, String name, int i) {
      lua.push(name);
      lua.push(i);
      lua.setTable(-3);
   }

   private static void tableAdd(Lua lua, String name, String value) {
      lua.push(name);
      lua.push(value);
      lua.setTable(-3);
   }

   public static String sha1hex(String s) {
      try {
         MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
         return Util.toHexString(sha1.digest(s.getBytes(StandardCharsets.UTF_8)));
      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException(e);
      }
   }

   /*
    * There are two possible formats for the received `error` string:
    * 1) "-CODE msg": in this case we remove the leading '-' since we don't store it as part of the lua error format.
    * 2) "msg": in this case we prepend a generic 'ERR' code since all error statuses need some error code.
    */
   public static void luaPushError(Lua lua, String error) {
      String msg;
      // Trim any CR/LF at the end
      int endpos = error.length() - (error.endsWith("\r\n") ? 2 : 0);
      if (error.startsWith("-")) {
         int pos = error.indexOf(' ');
         if (pos < 0) {
            msg = "ERR " + error.substring(1, endpos);
         } else {
            msg = error.substring(1, endpos);
         }
      } else {
         msg = "ERR " + error.substring(0, endpos);
      }
      lua.newTable();
      tableAdd(lua, "err", msg);
   }

   private static int luaProtectedTableError(Lua lua) {
      int argc = lua.getTop();
      if (argc != 2) {
         lua.error("Wrong number of arguments to luaProtectedTableError");
      }
      if (!lua.isString(-1) && !lua.isNumber(-1)) {
         lua.error("Second argument to luaProtectedTableError must be a string or number");
      }
      String variableName = lua.toString(-1);
      lua.error("Script attempted to access nonexistent global variable '" + variableName + "'");
      return 0;
   }

   private void luaSetErrorMetatable() {
      lua.push(LUA_GLOBALSINDEX);
      lua.newTable();
      lua.push(LuaEngine::luaProtectedTableError);
      lua.setField(-2, "__index");
      lua.setMetatable(-2);
      lua.pop(1);
   }

   private static int luaNewIndexAllowList(Lua lua) {
      int argc = lua.getTop();
      if (argc != 3) {
         lua.error("Wrong number of arguments to luaNewIndexAllowList"); // Same error Redis reports
      }
      if (!lua.isTable(-3)) {
         lua.error("first argument to luaNewIndexAllowList must be a table");
      }
      if (!lua.isString(-2) && !lua.isNumber(-2)) {
         lua.error("Second argument to luaNewIndexAllowList must be a string or number");
      }
      String variableName = lua.toString(-2);
      if (ALLOW_LISTS.contains(variableName)) {
         lua.rawSet(-3);
      } else if (!DENY_LIST.contains(variableName)) {
         Log.SERVER.warnf("A key '%s' was added to Lua globals which is not on the globals allow list nor listed on the deny list.", variableName);
      }
      return 0;
   }

   private static int luaSetAllowListProtection(Lua lua) {
      lua.newTable();
      lua.push(LuaEngine::luaNewIndexAllowList);
      lua.setField(-2, "__newindex");
      lua.setMetatable(-2);
      return 0;
   }

   void registerScript(Code code) {
      String name = fName(code.sha());
      lua.getField(Lua51Consts.LUA_REGISTRYINDEX, name);
      if (lua.get().type() == Lua.LuaType.NIL) {
         byte[] bytes = code.code().getBytes(StandardCharsets.US_ASCII);
         ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
         buffer.put(bytes);
         lua.load(buffer, "@user_script");
         lua.setField(Lua51Consts.LUA_REGISTRYINDEX, name);
      }
   }

   void unregisterScript(Code code) {
      lua.pushNil();
      lua.setField(Lua51Consts.LUA_REGISTRYINDEX, fName(code.sha()));
   }

   public Map<String, CodeFunction> registerFunctions(Code code) {
      try {
         functions = new HashMap<>();
         mode = LuaEngine.Mode.LOAD;
         byte[] bytes = code.code().getBytes(StandardCharsets.US_ASCII);
         ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
         buffer.put(bytes);
         lua.load(buffer, "@user_function");
         // run the code to register the functions
         lua.pCall(0, 0);
         return functions;
      } catch (Throwable t) {
         throw new RuntimeException(t);
      } finally {
         mode = Mode.USER;
      }
   }

   void runScript(Code script, String[] keys, String[] args) {
      // Populate the ARGV table and set it as a global
      lua.newTable();
      for (int i = 0; i < args.length; i++) {
         lua.push(args[i]);
         lua.rawSetI(-2, i + 1);
      }
      lua.setGlobal("ARGV");
      // Populate the KEYS table and set it as a global
      lua.newTable();
      for (int i = 0; i < keys.length; i++) {
         lua.push(keys[i]);
         lua.rawSetI(-2, i + 1);
      }
      lua.setGlobal("KEYS");
      lua.getGlobal("__redis__err__handler");
      lua.getField(LUA_REGISTRYINDEX, fName(script.sha()));
      // Invoke the function using the supplied error handler which will need to be popped from the stack after
      // the return value has been processed
      lua.getLuaNatives().lua_pcall(lua.getPointer(), 0, 1, -2);
   }


   void runFunction(CodeFunction codeFunction, String[] keys, String[] args, boolean ro) {
      long flags = codeFunction.flags() | (ro ? ScriptFlags.NO_WRITES.value() : 0);
      if (ScriptFlags.EVAL_COMPAT_MODE.isSet(flags)) {
         if (ScriptFlags.NO_CLUSTER.isSet(flags) && handler.cache().getCacheConfiguration().clustering().cacheMode().isClustered()) {
            throw new IllegalStateException("Can not run script on cluster, 'no-cluster' flag is set.");
         }
      }
      /* Push error handler */
      lua.push("__ERROR_HANDLER__");
      lua.getTable(LUA_REGISTRYINDEX);
      lua.rawGetI(LUA_REGISTRYINDEX, localFunctionRefs.get(codeFunction.name()));
      if (!lua.isFunction(-1)) {
         throw new IllegalArgumentException(codeFunction.name() + " is not a function");
      }
      // Populate the KEYS table
      lua.newTable();
      for (int i = 0; i < args.length; i++) {
         lua.push(keys[i]);
         lua.rawSetI(-2, i + 1);
      }
      // Populate the ARGV table
      lua.newTable();
      for (int i = 0; i < args.length; i++) {
         lua.push(args[i]);
         lua.rawSetI(-2, i + 1);
      }
      int err = lua.getLuaNatives().lua_pcall(lua.getPointer(), 2, 1, -4);
      if (err != 0) {
         if (!lua.isTable(-1)) {
            String msg = "execution failure";
            if (lua.isString(-1)) {
               msg = lua.toString(-1);
            }
            lua.pop(1); // Consume the Lua error
         } else {
            //luaReplyToRedisReply(c, run_ctx->c, lua); /* Convert and consume the reply. */
         }
         lua.pop(1); /* Pop error handler */
         lua.gc();
         // Invoke the function using the supplied error handler which will need to be popped from the stack after
         // the return value has been processed
         lua.getLuaNatives().lua_pcall(lua.getPointer(), 0, 1, -2);

         //scriptResetRun( & run_ctx);
      }
   }

   /**
    * Convert the response found on the Lua stack to a RESP response
    */
   void writeResponse(ResponseWriter writer) {
      try {
         lua.checkStack(4);
      } catch (RuntimeException e) {
         writer.customError("reached lua stack limit");
         lua.pop(1);
         return;
      }
      switch (lua.type(-1)) {
         case STRING:
            writer.string(lua.toString(-1));
            break;
         case BOOLEAN:
            writer.booleans(lua.toBoolean(-1));
            break;
         case NUMBER:
            writer.integers((long) lua.toNumber(-1));
            break;
         case TABLE:
            // Is it an error ?
            lua.push("err");
            lua.rawGet(-2);
            if (lua.type(-1) == Lua.LuaType.STRING) {
               lua.pop(1); // pop the error message
               ErrorInfo errorInfo = extractErrorInformation();
               writer.error("-" + errorInfo.message());
               lua.pop(1); // pop the result table
               return;
            }
            lua.pop(1);

            // Is it a simple status ?
            lua.push("ok");
            lua.rawGet(-2);
            if (lua.type(-1) == Lua.LuaType.STRING) {
               String ok = lua.toString(-1).replaceAll("[\\r\\n]", " ");
               writer.string(ok);
               lua.pop(2);
               return;
            }
            lua.pop(1);

            // Is it a double ?
            lua.push("double");
            lua.rawGet(-2);
            if (lua.type(-1) == Lua.LuaType.NUMBER) {
               writer.doubles(lua.toNumber(-1));
               lua.pop(2);
               return;
            }
            lua.pop(1);

            // Is it a map ?
            lua.push("map");
            lua.rawGet(-2);
            if (lua.type(-1) == Lua.LuaType.TABLE) {
               lua.push("len");
               lua.rawGet(-3);
               int size = (int) lua.toInteger(-1);
               lua.pop(1);
               LuaMap<Object, Object> map = new LuaMap<>(lua, -2, size);
               writer.map(map, new SerializationHint.KeyValueHint(
                     (object, w) -> {
                        lua.pushValue(-2); // duplicate key for iteration
                        writeResponse(w); // key
                     },
                     (object, w) -> {
                        writeResponse(w); // value
                     }
               ));
               lua.pop(2);
               return;
            }
            lua.pop(1);

            // Is it a set ?
            lua.push("set");
            lua.rawGet(-2);
            if (lua.type(-1) == Lua.LuaType.TABLE) {
               lua.push("len");
               lua.rawGet(-3);
               int size = (int) lua.toInteger(-1);
               lua.pop(1);
               LuaSet<Object> set = new LuaSet<>(lua, -2, size);
               writer.set(set, (o, w) -> {
                  // Stack: table, key (object), value (boolean)
                  lua.pop(1); // Discard the value
                  lua.pushValue(-1); // duplicate the key, for iteration
                  writeResponse(w); // write the object to the handler
                  // Stack: table, key
               });
               lua.pop(2);
               return;
            }
            lua.pop(1);

            // It's an array !
            LuaArray<Object> array = new LuaArray<>(lua, -1);
            writer.array(array, (o, w) -> writeResponse(w));
            break;
         default:
            writer.nulls();
      }
      lua.pop(1);
   }

   private String stringField(String name) {
      lua.getField(-1, name);
      try {
         return lua.isString(-1) ? lua.toString(-1) : null;
      } finally {
         lua.pop(1);
      }
   }

   private boolean booleanField(String name) {
      lua.getField(-1, name);
      try {
         return lua.isBoolean(-1) && lua.toBoolean(-1);
      } finally {
         lua.pop(1);
      }
   }

   private ErrorInfo extractErrorInformation() {
      if (lua.isString(-1)) {
         return new ErrorInfo("ERR " + lua.toString(-1), null, null, false);
      } else {
         return new ErrorInfo(
               stringField("err"),
               stringField("source"),
               stringField("line"),
               booleanField("ignore_error_stats_update")
         );
      }
   }

}
