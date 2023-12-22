package org.infinispan.api.configuration;

/**
 * Flags which affect administrative operations such as cache creation and removal.
 *
 * @since 16.3
 */
public enum AdminFlag {
   /**
    * Configuration changes will not be persisted to the global state.
    */
   VOLATILE
}
