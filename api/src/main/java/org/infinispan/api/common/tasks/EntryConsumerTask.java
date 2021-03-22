package org.infinispan.api.common.tasks;

import java.util.Map;
import java.util.function.Consumer;

/**
 * @since 13.0
 **/
public interface EntryConsumerTask<K, V> extends Consumer<Map.Entry<K, V>> {}
