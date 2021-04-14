package org.infinispan.commons.configuration.io.json;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.infinispan.commons.configuration.io.AbstractConfigurationReader;
import org.infinispan.commons.configuration.io.ConfigurationReaderException;
import org.infinispan.commons.configuration.io.ConfigurationResourceResolver;
import org.infinispan.commons.configuration.io.Feature;
import org.infinispan.commons.configuration.io.Location;
import org.infinispan.commons.configuration.io.NamingStrategy;
import org.infinispan.commons.configuration.io.PropertyReplacer;
import org.infinispan.commons.dataconversion.internal.Json;
import org.infinispan.commons.util.Util;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 12.1
 **/
public class JsonConfigurationReader extends AbstractConfigurationReader {
   private static final String NAMESPACE = "_namespace";
   private final Deque<Iterator<?>> iteratorStack;
   private final Deque<String> nameStack;
   private final BufferedReader reader;
   private String name;
   private Json element;
   private List<Map.Entry<String, Json>> attributes = new ArrayList<>();
   private ElementType type;
   private String namespace;

   public JsonConfigurationReader(Reader reader, ConfigurationResourceResolver resourceResolver, Properties properties, PropertyReplacer replacer, NamingStrategy namingStrategy) {
      super(resourceResolver, properties, replacer, namingStrategy);
      this.reader = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
      try (Stream<String> lines = this.reader.lines()) {
         Map<String, Json> json = Json.read(lines.collect(Collectors.joining())).asJsonMap();
         Json namespace = json.remove(NAMESPACE);
         this.namespace = namespace == null ? "" : namespace.asString();
         iteratorStack = new ArrayDeque<>();
         iteratorStack.push(json.entrySet().iterator());
         nameStack = new ArrayDeque<>();
         type = ElementType.START_DOCUMENT;
      }
   }

   @Override
   public ElementType nextElement() {
      Iterator<?> iterator = iteratorStack.peek();
      if (iterator == null) {
         return null;
      } else if (iterator.hasNext()) {
         Object item = iterator.next();
         if (item instanceof Map.Entry) {
            // this is a map
            Map.Entry<String, ?> e = (Map.Entry<String, ?>) item;
            Json value = (Json) e.getValue();
            if (value.isPrimitive()) {
               // It's an attribute, skip it
               return nextElement();
            } else if (value.isObject()) {
               processObject(value);
            } else if (value.isArray()) {
               processArray(e.getKey(), value);
            } else if (value.isNull()) {
               attributes.clear();
               iteratorStack.push(Collections.emptyIterator());
            }
            name = e.getKey();
            nameStack.push(name);
            return (type = ElementType.START_ELEMENT);
         } else if (item instanceof ElementEntry) {
            ElementEntry entry = (ElementEntry) item;
            name = entry.k;
            type = entry.type;
            if (type == ElementType.START_ELEMENT) {
               nameStack.push(name);
               element = entry.v;
               if (element.isObject()) {
                  processObject(element);
               } else if (element.isArray()) {
                  processArray(name, element);
               }
            } else {
               nameStack.pop();
               iteratorStack.pop();
               element = null;
            }
            return type;
         } else {
            throw new IllegalStateException(item.toString());
         }
      } else {
         // We've reached the end of the current iterator
         iteratorStack.pop();
         element = null;
         attributes.clear();
         if (nameStack.isEmpty()) {
            return (type = ElementType.END_DOCUMENT);
         } else {
            name = nameStack.pop();
            return (type = ElementType.END_ELEMENT);
         }
      }
   }

   private void processArray(String name, Json value) {
      attributes.clear();
      List<Json> list = value.asJsonList();
      List<ElementEntry> array = new ArrayList<>(list.size() * 2 + 2);
      boolean primitive = (list.size() > 0 && list.get(0).isPrimitive());
      if (!primitive) {
         array.add(new ElementEntry(name, null, ElementType.START_ELEMENT));
      }
      for (Json json : list) {
         array.add(new ElementEntry(name, json, ElementType.START_ELEMENT));
         if (!isObjectOfPrimitives(json)) {
            array.add(new ElementEntry(name, json, ElementType.END_ELEMENT));
         }
      }
      if (!primitive) {
         array.add(new ElementEntry(name, null, ElementType.END_ELEMENT));
      }
      Iterator<ElementEntry> it = array.iterator();
      iteratorStack.push(it);
      element = it.next().v; // Already remove the first one
   }

   private boolean isObjectOfPrimitives(Json json) {
      if (json.isObject()) {
         for(Json item : json.asJsonMap().values()) {
            if (!item.isPrimitive()) {
               return false;
            }
         }
         return true;
      } else {
         return false;
      }
   }

   private void processObject(Json value) {
      // Find all the attributes
      attributes.clear();
      Map<String, Json> map = value.asJsonMap();
      for (Map.Entry<String, Json> entry : map.entrySet()) {
         if (entry.getValue().isPrimitive()) {
            attributes.add(entry);
         }
      }
      iteratorStack.push(map.entrySet().iterator());
   }

   @Override
   public Location getLocation() {
      return Location.of(1, 0);
   }

   @Override
   public String getAttributeName(int index, NamingStrategy strategy) {
      String name = attributes.get(index).getKey();
      int colon = name.lastIndexOf(':');
      return strategy.convert(colon < 0 ? name : name.substring(colon + 1));
   }

   @Override
   public String getLocalName(NamingStrategy strategy) {
      int colon = name.lastIndexOf(':');
      return strategy.convert(colon < 0 ? name : name.substring(colon + 1));
   }

   @Override
   public String getAttributeNamespace(int index) {
      String name = attributes.get(index).getKey();
      int colon = name.lastIndexOf(':');
      return colon < 0 ? "" : name.substring(0, colon);
   }

   @Override
   public String getAttributeValue(String localName) {
      for (int i = 0; i < attributes.size(); i++) {
         Map.Entry<String, Json> attribute = attributes.get(i);
         if (localName.equals(attribute.getKey())) {
            return attribute.getValue().asString();
         }
      }
      return null;
   }

   @Override
   public String getAttributeValue(int index) {
      Json value = attributes.get(index).getValue();
      return replaceProperties(value.isString() ? value.asString() : value.toString());
   }

   @Override
   public String getElementText() {
      String text = (element != null && element.isPrimitive()) ? element.asString() : null;
      nextElement();// Consume the end element
      return text;
   }

   @Override
   public String getNamespace() {
      int colon = name.lastIndexOf(':');
      return colon < 0 ? namespace : name.substring(0, colon);
   }

   @Override
   public boolean hasNext() {
      return !iteratorStack.isEmpty();
   }

   @Override
   public int getAttributeCount() {
      return attributes.size();
   }

   @Override
   public void require(ElementType type, String namespace, String name) {
      if (type != this.type
            || (namespace != null && !namespace.equals(getNamespace()))
            || (name != null && !name.equals(getLocalName()))) {
         throw new ConfigurationReaderException("Expected event " + type
               + (name != null ? " with name '" + name + "'" : "")
               + (namespace != null && name != null ? " and" : "")
               + (namespace != null ? " with namespace '" + namespace + "'" : "")
               + " but got"
               + (type != this.type ? " " + this.type : "")
               + (name != null && getLocalName() != null && !name.equals(getLocalName())
               ? " name '" + getLocalName() + "'" : "")
               + (namespace != null && name != null
               && getLocalName() != null && !name.equals(getLocalName())
               && getNamespace() != null && !namespace.equals(getNamespace())
               ? " and" : "")
               + (namespace != null && getNamespace() != null && !namespace.equals(getNamespace())
               ? " namespace '" + getNamespace() + "'" : ""), Location.of(1, 1));
      }
   }

   @Override
   public Map.Entry<String, String> getMapItem(String nameAttribute) {
      String name = getLocalName(NamingStrategy.IDENTITY);
      nextElement();
      String type = getLocalName();
      return new MapEntry(name, type);
   }

   @Override
   public void endMapItem() {
      nextElement();
   }

   @Override
   public String[] readArray(String outer, String inner) {
      Iterator<ElementEntry> iterator = (Iterator<ElementEntry>) iteratorStack.peek();
      List<String> array = new ArrayList<>();
      while (iterator.hasNext()) {
         ElementEntry next = iterator.next();
         if (next.type == ElementType.END_ELEMENT) {
            array.add(next.v.asString());
         }
      }
      nextElement();
      require(ElementType.END_ELEMENT, null, outer);
      return array.stream().toArray(String[]::new);
   }

   @Override
   public boolean hasFeature(Feature feature) {
      return false;
   }

   @Override
   public void close() {
      Util.close(reader);
   }

   private static class ElementEntry {
      final String k;
      final Json v;
      final ElementType type;

      ElementEntry(String k, Json v, ElementType type) {
         this.k = k;
         this.v = v;
         this.type = type;
      }
   }
}
