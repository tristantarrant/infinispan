package org.infinispan.commons.configuration.attributes;

import java.util.Collection;

import org.infinispan.commons.configuration.io.ConfigurationWriter;

/**
 * AttributeSerializer.
 *
 * @since 7.2
 */
public interface AttributeSerializer<T> {
   AttributeSerializer<Object> DEFAULT = (writer, name, value) -> {
      if (Boolean.class == value.getClass()) {
         writer.writeAttribute(name, (Boolean) value);
      } else {
         writer.writeAttribute(name, value.toString());
      }
   };
   AttributeSerializer<Object> SECRET = (writer, name, value) -> writer.writeAttribute(name, "***");
   AttributeSerializer<String[]> STRING_ARRAY = (writer, name, value) -> writer.writeAttribute(name, value);
   AttributeSerializer<Collection<String>> STRING_COLLECTION = (writer, name, value) -> writer.writeAttribute(name, value.toArray(new String[0]));
   AttributeSerializer<Collection<? extends Enum>> ENUM_COLLECTION = (writer, name, value) -> writer.writeAttribute(name, value.stream().map(Enum::toString).toArray(String[]::new));
   AttributeSerializer<Object> CLASS_NAME = ((writer, name, value) -> writer.writeAttribute(name, value.getClass().getName()));

   void serialize(ConfigurationWriter writer, String name, T value);
}
