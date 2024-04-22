package org.infinispan.cli.wizard;

public record Secret(String name, String text, String initialValue) implements Input {
   @Override
   public String value() {
      return initialValue;
   }
}
