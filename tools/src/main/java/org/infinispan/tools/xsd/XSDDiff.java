package org.infinispan.tools.xsd;

import static org.infinispan.tools.ToolUtils.DOCUMENT_BUILDER;
import static org.infinispan.tools.ToolUtils.attrToString;
import static org.infinispan.tools.ToolUtils.findNode;
import static org.infinispan.tools.ToolUtils.printNodeSignature;
import static org.infinispan.tools.ToolUtils.textFromNode;
import static org.infinispan.tools.ToolUtils.wideContext;
import static org.infinispan.tools.ToolUtils.xpathDepth;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Comparison;
import org.xmlunit.diff.ComparisonType;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

public class XSDDiff {

   private final Source controlSource;
   private final Source testSource;
   private final Document controlDoc;
   private final Document testDoc;

   private XSDDiff(Path file1, Path file2) throws IOException, SAXException, ParserConfigurationException {
      this.controlSource = Input.fromPath(file1).build();
      this.testSource = Input.fromPath(file2).build();
      controlDoc = DOCUMENT_BUILDER.parse(file1.toFile());
      testDoc = DOCUMENT_BUILDER.parse(file2.toFile());
   }

   public void diff(PrintStream out) throws IOException, SAXException {
      Diff diff = DiffBuilder.compare(controlSource).withTest(testSource).build();
      for (Difference d : diff.getDifferences()) {
         final Comparison comparison = d.getComparison();
         if (isAdded(comparison)) {
            processAdd(out, comparison);
         } else if (isDeleted(comparison)) {
            processDelete(out, comparison);
         } else {
            processModified(out, comparison);
         }
      }
   }

   private void processAdd(PrintStream out, Comparison c) {
      Comparison.Detail details = c.getTestDetails();
      Node parentNode = findNode(testDoc, details.getParentXPath());

      String nodeText = textFromNode(findNode(testDoc, details.getXPath()));
      out.print("ADDED <!-- xpath: " + details.getXPath() + " (parent node: " + printNodeSignature(parentNode) + " - " + details.getParentXPath() + " ) -->");
      out.print("+ ");
      out.println();
   }

   private void processDelete(PrintStream out, Comparison c) {
      Comparison.Detail details = c.getControlDetails();
      Node parentNode = findNode(controlDoc, details.getParentXPath());
      out.print("DELETED <!-- xpath: " + details.getXPath() + " (parent node: " + printNodeSignature(parentNode) + " - " + details.getParentXPath() + " ) -->");
      out.print("- ");
      out.println();

      String nodeText = textFromNode(findNode(controlDoc, details.getXPath()));
   }

   private void processModified(PrintStream out, Comparison c) {

      Comparison.Detail details = c.getControlDetails();
      if (xpathDepth(details.getXPath()) == 1) {
         out.print("MODIFIED ; " + details.getXPath() + ".");
         out.print(" _ ");
         out.println();
      } else {
         if (c.getType() == ComparisonType.CHILD_NODELIST_SEQUENCE) {
            out.print(". node order different: " + c.getTestDetails().getXPath());
            out.print(" * ");
            out.println();
         } else if (c.getType() == ComparisonType.CHILD_NODELIST_LENGTH) {
            printChildNodesChanged(out, c);
         } else if (c.getType() == ComparisonType.ATTR_NAME_LOOKUP || c.getType() == ComparisonType.ATTR_VALUE) {
            printAttrChanged(out, c);
         } else {
            printNodeDiff(out, c);
         }
      }
   }

   public void printChildNodesChanged(PrintStream out, Comparison c) {
      String parentNodeXpath = c.getTestDetails().getXPath();

      int sizeControl = (int) c.getControlDetails().getValue();
      int sizeTest = (int) c.getTestDetails().getValue();
      if (sizeTest > sizeControl) {
         // nodes added
         out.printf(". %s node(s) added: %s <!-- %s -->", sizeTest - sizeControl, printNodeSignature(c.getTestDetails().getTarget()), parentNodeXpath);
         out.print(" * ");
         out.println();
      } else {
         // nodes removed
         out.printf(". %s node(s) removed: %s <!-- %s -->", sizeControl - sizeTest, printNodeSignature(c.getTestDetails().getTarget()), parentNodeXpath);
         out.print(" * ");
         out.println();
      }
   }

   private String holderNodeText(Document doc, Comparison.Detail details) {
      long xpathDepth = xpathDepth(details.getXPath());
      boolean shouldTakeParent = xpathDepth > 2;
      String xpathExpr = shouldTakeParent ? wideContext(details.getParentXPath()) : details.getXPath();
      return textFromNode(findNode(doc, xpathExpr));
   }

   private void printAttrChanged(PrintStream out, Comparison c) {
      if (isAttrAdded(c)) {
         String nodeText = textFromNode(findNode(testDoc, c.getTestDetails().getParentXPath()));
         String attributeText = attrToString(c.getTestDetails().getTarget(), (QName) c.getTestDetails().getValue());
         out.print("MODIFIED ; new attribute [" + attributeText + "] <!-- xpath: " + c.getTestDetails().getXPath() + " -->");
         out.print(" . ");
         out.println();
         String parentNodeXpath = wideContext(c.getTestDetails().getXPath());

      } else if (isAttrDeleted(c)) {
         String controlNodeText = textFromNode(findNode(controlDoc, c.getControlDetails().getParentXPath()));
         String controlAttributeText = attrToString(c.getControlDetails().getTarget(), (QName) c.getControlDetails().getValue());
         out.print("MODIFIED ; removed attribute [" + controlAttributeText + "] <!-- xpath: " + c.getControlDetails().getXPath() + " -->");
         out.print(" . ");
         out.println();

         String parentNodeXpath = wideContext(c.getControlDetails().getXPath());

      } else {
         // modified in place
         String nodeText = textFromNode(findNode(testDoc, c.getTestDetails().getParentXPath()));
         String attributeText = attrToString((Attr) c.getTestDetails().getTarget());
         out.print("MODIFIED ; changed attribute [" + attributeText + "] <!-- xpath: " + c.getTestDetails().getXPath() + " -->");
         out.print(" . ");
         out.println();
         String parentNodeXpath = wideContext(c.getTestDetails().getXPath());
      }

   }

   private void printNodeDiff(PrintStream out, Comparison c) {
      String oldText = textFromNode(c.getControlDetails().getTarget());
      String newText = textFromNode(c.getTestDetails().getTarget());

      printFullNodeDiff(out, testDoc, c, oldText, newText);
   }

   private void printFullNodeDiff(PrintStream out, Document testDoc, Comparison comparison, String oldText, String newText) {
      out.print("NODE MODIFIED [" + comparison.getType() + "] ; " + comparison.toString() + "\n");
      out.print(" . ");
      out.println();
   }

   private static boolean isAdded(Comparison comparison) {
      return comparison.getControlDetails().getTarget() == null;
   }

   private static boolean isDeleted(Comparison comparison) {
      return comparison.getTestDetails().getTarget() == null;
   }

   private static boolean isAttrAdded(Comparison comparison) {
      return comparison.getControlDetails().getValue() == null;
   }

   private static boolean isAttrDeleted(Comparison comparison) {
      return comparison.getTestDetails().getValue() == null;
   }


   public static void main(String[] args) {
      try {
         XSDDiff xsdDiff = new XSDDiff(Paths.get(args[0]), Paths.get(args[1]));
         xsdDiff.diff(System.out);
      } catch (SAXException | IOException | ParserConfigurationException e) {
         throw new RuntimeException(e);
      }
   }
}
