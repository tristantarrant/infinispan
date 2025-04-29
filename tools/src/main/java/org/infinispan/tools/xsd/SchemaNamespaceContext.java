package org.infinispan.tools.xsd;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

public class SchemaNamespaceContext implements NamespaceContext {
   public static final String SCHEMA_NS_PREFIX = "xs";

   Map<String, String> namespaceToPrefix = new HashMap<>();
   Map<String, String> prefixToNamespace = new HashMap<>();

   public SchemaNamespaceContext() {
      super();
      addNs(XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI);
      addNs(XMLConstants.XMLNS_ATTRIBUTE, XMLConstants.XMLNS_ATTRIBUTE_NS_URI);
      addNs(SCHEMA_NS_PREFIX, XMLConstants.W3C_XML_SCHEMA_NS_URI);
   }

   private void addNs(String prefix, String namespace) {
      namespaceToPrefix.put(namespace, prefix);
      prefixToNamespace.put(prefix, namespace);
   }

   @Override
   public String getNamespaceURI(String prefix) {
      return prefixToNamespace.get(prefix);
   }

   @Override
   public String getPrefix(String namespaceURI) {
      return namespaceToPrefix.get(namespaceURI);
   }

   @Override
   public Iterator<String> getPrefixes(String namespaceURI) {
      return prefixToNamespace.keySet().iterator();
   }

   public Map<String, String> prefixToUri() {
      return prefixToNamespace;
   }

}