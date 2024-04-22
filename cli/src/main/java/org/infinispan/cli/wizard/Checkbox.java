package org.infinispan.cli.wizard;

public record Checkbox(String name, String text, Item... items) implements Input {
   public record Item(String name, String text, boolean selected) {
   }
}
