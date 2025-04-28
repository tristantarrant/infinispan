package org.infinispan.scripting.impl;

import java.util.function.Consumer;
import java.util.function.Predicate;

public record ScriptingListener(Predicate<String> filter, Consumer<String> consumer) {
}
