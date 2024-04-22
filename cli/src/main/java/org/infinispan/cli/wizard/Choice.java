package org.infinispan.cli.wizard;

public record Choice(String name, String text, Item... items) implements Input {
   public record Item(String name, String text) {
   }
}
