package org.infinispan.rest.assertion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.infinispan.commons.dataconversion.internal.Json;

/**
 * Assertions on JSON contents.
 *
 * @author Dan Berindei
 * @since 12
 */
public class JsonAssertion {
   String path;
   private final Json node;

   public JsonAssertion(Json node) {
      this(node, "");
   }

   public JsonAssertion(Json node, String path) {
      this.node = node;
      this.path = path;
      assertNotNull(path, node);
   }

   public JsonAssertion hasProperty(String propertyName) {
      return new JsonAssertion(node.at(propertyName), propertyPath(propertyName));
   }

   public JsonAssertion hasNullProperty(String propertyName) {
      hasProperty(propertyName).isNull();
      return this;
   }

   public JsonAssertion hasNoProperty(String propertyName) {
      assertFalse(propertyPath(propertyName), node.has(propertyName));
      return this;
   }

   public void is(int value) {
      assertEquals(value, node.asInteger());
   }

   public void is(String value) {
      assertEquals(value, node.asString());
   }

   public void isNull() {
      assertTrue(path, node.isNull());
   }

   private String propertyPath(String propertyName) {
      return this.path.isEmpty() ? propertyName : this.path + "." + propertyName;
   }
}
