package org.infinispan.server.resp.commands.scripting.function;

import org.infinispan.server.resp.AclCategory;
import org.infinispan.server.resp.RespCommand;
import org.infinispan.server.resp.commands.FamilyCommand;

/**
 * FUNCTION
 *
 * @see <a href="https://redis.io/docs/latest/commands/function/">FUNCTION</a>
 * @since 16.0
 */
public class FUNCTION extends FamilyCommand {
   private static final RespCommand[] FUNCTION_COMMANDS = new RespCommand[]{
         new DELETE(),
         new DUMP(),
         new FLUSH(),
         new KILL(),
         new LIST(),
         new LOAD(),
         new RESTORE(),
         new STATS()
   };

   public FUNCTION() {
      super(-2, 0, 0, 0, AclCategory.STRING.mask());
   }

   @Override
   public RespCommand[] getFamilyCommands() {
      return FUNCTION_COMMANDS;
   }

}
