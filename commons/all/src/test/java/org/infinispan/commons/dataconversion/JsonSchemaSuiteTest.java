package org.infinispan.commons.dataconversion;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.infinispan.commons.dataconversion.internal.Json;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class JsonSchemaSuiteTest {

   private final String group;
   private final String description;
   private final Json.Schema schema;
   private final Json data;
   private final boolean valid;

   @Parameterized.Parameters
   public static Collection<Object[]> data() throws Exception {
      List<Object[]> tests = new ArrayList<>();
      URI jsonSuite = Thread.currentThread().getContextClassLoader().getResource("json").toURI();
      List<Path> paths = Files.list(Paths.get(jsonSuite)).toList();

      for (Path path : paths) {
         if (!Files.isDirectory(path)) {
            String json = Files.readString(path);
            Json set = Json.read(json);
            if (!set.isArray()) set = Json.array().add(set);
            for (Json one : set.asJsonList()) {
               try {
                  Json.Schema schema = Json.schema(one.at("schema"));
                  for (Json t : one.at("tests").asJsonList())
                     tests.add(new Object[]{path.toString(), t.at("description", "***").asString() + "/" + one.at("description", "---").asString(), schema, t.at("data"), t.at("valid", true).asBoolean()});
               } catch (Throwable t) {
                  throw new RuntimeException("While adding tests from file " + path + " - " + one, t);
               }
            }
         }
      }
      return tests;
   }

   public JsonSchemaSuiteTest(String group, String description, Json.Schema schema, Json data, boolean valid) {
      this.group = group;
      this.description = description;
      this.schema = schema;
      this.data = data;
      this.valid = valid;
   }

   @Test
   public void doTest() {
      assertEquals("Running test " + description + " from " + group, valid, schema.validate(data).is("ok", true));
   }

}
