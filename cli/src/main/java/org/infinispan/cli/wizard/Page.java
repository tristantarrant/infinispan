package org.infinispan.cli.wizard;

import java.util.function.Function;

public record Page(String name, String title, String text, Function<Values, Action> action,
                   Input... inputs) {
   private static final Action NEXT_ACTION = new Action(Kind.NEXT, "");
   public static final Function<Values, Action> NEXT = (v) -> NEXT_ACTION;

   public static Action GOTO(String name) {
      return new Action(Kind.GOTO, name);
   }

   public record Action(Kind kind, String name) {
   }

   public enum Kind {
      NEXT,
      FINISH,
      GOTO
   }
}
