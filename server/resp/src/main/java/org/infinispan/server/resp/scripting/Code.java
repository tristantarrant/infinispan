package org.infinispan.server.resp.scripting;

import java.util.HashMap;
import java.util.Map;

import org.infinispan.scripting.impl.ScriptMetadata;
import org.infinispan.scripting.impl.ScriptWithMetadata;

/**
 * A named code fragment.
 *
 * @param name the name of the code fragment
 * @param code the code fragment
 * @param sha  the sha of the code fragment
 * @param flags the execution flags for the code fragment. See {@link ScriptFlags}
 */
public record Code(String name, String code, String sha, long flags) {

   static Code fromScript(ScriptWithMetadata script) {
      return fromScript(script.code(), script.metadata());
   }

   public static Code fromScript(String script, ScriptMetadata metadata) {
      Map<String, String> properties = metadata.properties();
      return new Code(properties.get("name"), script, properties.get("sha"), Long.parseLong(properties.get("flags")));
   }

   public static Map<String, String> parseShebang(String script, boolean required) {
      Map<String, String> properties = new HashMap<>();
      long flags = 0;
      if (script.startsWith("#!")) {
         int end = script.indexOf('\n');
         if (end < 0) {
            throw new IllegalArgumentException("Invalid script shebang");
         }
         String[] parts = script.substring(2, end).split(" ");
         String engine = parts[0];
         if (engine.isBlank()) {
            throw new IllegalArgumentException("Invalid library metadata");
         }
         properties.put("engine", engine.toUpperCase());
         for (int i = 1; i < parts.length; i++) {
            if (parts[i].startsWith("flags=")) {
               String[] fNames = parts[i].substring(6).split(",");
               for (String fName : fNames) {
                  flags |= ScriptFlags.valueOf(fName).value();
               }
            } else if (parts[i].startsWith("name=")) {
               // Process the name
               properties.put("name", parts[i].substring(5));
            } else {
               throw new IllegalArgumentException("Unknown lua shebang option: " + parts[i]);
            }
         }
         if (!properties.containsKey("name") && required) {
            throw new IllegalArgumentException("Library name was not given");
         }
      } else {
         if (required) {
            throw new IllegalArgumentException("Missing library metadata");
         } else {
            flags = ScriptFlags.EVAL_COMPAT_MODE.value();
         }
      }
      properties.put("flags", Long.toString(flags));
      return properties;
   }
}
