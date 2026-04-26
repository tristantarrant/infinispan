package org.infinispan.server.resp.commands.connection;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.infinispan.commons.util.Version;
import org.infinispan.security.AuthorizationPermission;
import org.infinispan.server.resp.AclCategory;
import org.infinispan.server.resp.Resp3Handler;
import org.infinispan.server.resp.RespCommand;
import org.infinispan.server.resp.RespRequestHandler;
import org.infinispan.server.resp.RespUtil;
import org.infinispan.server.resp.commands.Resp3Command;
import org.infinispan.server.resp.serialization.Resp3Type;

import io.netty.channel.ChannelHandlerContext;

/**
 * MODULE LIST
 *
 * @see <a href="https://redis.io/commands/module/">MODULE</a>
 * @since 15.0
 */
public class MODULE extends RespCommand implements Resp3Command {
   private static final byte[] LIST = "LIST".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] LOAD = "LOAD".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] LOADEX = "LOADEX".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] UNLOAD = "UNLOAD".getBytes(StandardCharsets.US_ASCII);

   public MODULE() {
      super(-1, 0, 0, 0, AclCategory.SLOW.mask());
   }

   @Override
   public CompletionStage<RespRequestHandler> perform(Resp3Handler handler, ChannelHandlerContext ctx,
                                                      List<byte[]> arguments) {
      handler.checkPermission(AuthorizationPermission.ADMIN);
      byte[] subcommand = arguments.get(0);
      if (RespUtil.isAsciiBytesEquals(LIST, subcommand)) {
         handler.writer().array(allModulesRESP2(), Resp3Type.AUTO); // RESP2 for Redis Insight
      } else if (RespUtil.isAsciiBytesEquals(LOAD, subcommand)
            || RespUtil.isAsciiBytesEquals(LOADEX, subcommand)
            || RespUtil.isAsciiBytesEquals(UNLOAD, subcommand)) {
         handler.writer().customError("module loading/unloading unsupported");
      }
      return handler.myStage();
   }

   public static List<List<Object>> allModulesRESP2() {
      long rVersion = Long.parseLong(Version.getMajor()) * 10000 + Long.parseLong(Version.getMinor()) * 100 + Long.parseLong(Version.getPatch());
      List<Object> jsonModule = List.of(
            "name", "ReJSON",
            "ver", rVersion,
            "path", "internal",
            "args", Collections.emptyList()
      );
      return List.of(jsonModule);
   }

   public static List<Map<String, Object>> allModulesRESP3() {
      long rVersion = Long.parseLong(Version.getMajor()) * 10000 + Long.parseLong(Version.getMinor()) * 100 + Long.parseLong(Version.getPatch());
      LinkedHashMap<String, Object> jsonModule = new LinkedHashMap<>(4); // Preserve order
      jsonModule.put("name", "ReJSON");
      jsonModule.put("ver", rVersion);
      jsonModule.put("path", "internal");
      jsonModule.put("args", Collections.emptyList());
      return List.of(jsonModule);
   }
}
