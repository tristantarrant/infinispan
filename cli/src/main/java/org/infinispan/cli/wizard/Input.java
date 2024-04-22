package org.infinispan.cli.wizard;

public interface Input {
   String name();

   default String value() {
      return "";
   }
}
