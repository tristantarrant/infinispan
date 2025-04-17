package org.infinispan.server.resp;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.sync.RedisCommands;

@Test(groups = "functional", testName = "server.resp.FunctionCommandsTest")
public class FunctionCommandsTest extends SingleNodeRespBaseTest {

   private static String functionCode(String engine, String libraryName, String functionName, String code) {
      return String.format(
            """
               #!%s name=%s
               redis.register_function('%s', function(KEYS, ARGV)
               %s
               end)""",
            engine, libraryName, functionName,  code);
   }

   private static String noWritesfunctionCode(String engine, String name, String fName, String code) {
      return String.format(
            """
                  #!%s name=%s
                  redis.register_function{function_name='%s', callback=function(KEYS, ARGV)
                  %s
                  end, flags={'no-writes'}}""",
            engine, name, fName,  code);
   }

   @Test
   public void testBasicUsage() {
      RedisCommands<String, String> redis = redisConnection.sync();
      redis.functionLoad(functionCode("LUA", "test", "test", "return 'hello'"));
      String out = redis.fcall("test", ScriptOutputType.STATUS, new String[]{});
      assertEquals("hello", out);
   }
}
