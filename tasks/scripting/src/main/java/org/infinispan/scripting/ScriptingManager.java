package org.infinispan.scripting;

import static org.infinispan.commons.internal.InternalCacheNames.SCRIPT_CACHE_NAME;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.infinispan.scripting.impl.ScriptMetadata;
import org.infinispan.scripting.impl.ScriptWithMetadata;
import org.infinispan.tasks.TaskContext;

/**
 * ScriptingManager. Defines the operations that can be performed on scripts. Scripts are stored in
 * a dedicated cache.
 *
 * @author Tristan Tarrant
 * @since 7.2
 */
public interface ScriptingManager {
   @Deprecated(forRemoval = true, since = "15.0")
   String SCRIPT_CACHE = SCRIPT_CACHE_NAME;

   /**
    * @deprecated since 12.1. Will be removed in 15.0. Use the CREATE permission instead.
    */
   @Deprecated(forRemoval=true, since = "12.1")
   String SCRIPT_MANAGER_ROLE = "___script_manager";

   /**
    * Adds a new script.
    *
    * @param name
    *           the name of the script. The name should contain an extension identifying its
    *           language
    * @param script
    *           the source of the script
    */
   void addScript(String name, String script);

   /**
    * Adds a new script with user-specified metadata
    *
    * @param name
    *           the name of the script. The name should contain an extension identifying its
    *           language
    * @param metadata the metadata for the script
    * @param script
    *           the source of the script
    */
   void addScript(String name, String script, ScriptMetadata metadata);

   /**
    * Removes a script.
    *
    * @param name
    *           the name of the script ro remove
    */
   void removeScript(String name);

   /**
    * Runs a named script
    *
    * @param scriptName The name of the script to run. Use {@link #addScript(String, String)} to add a script
    * @return a {@link CompletableFuture} which will return the result of the script execution
    */
   <T> CompletionStage<T> runScript(String scriptName);

   /**
    * Runs a named script using the specified {@link TaskContext}
    *
    * @param scriptName The name of the script to run. Use {@link #addScript(String, String)} to add a script
    * @param context A {@link TaskContext} within which the script will be executed
    * @return a {@link CompletableFuture} which will return the result of the script execution
    */
   <T> CompletionStage<T> runScript(String scriptName, TaskContext context);

   /**
    * Retrieves the source code of an existing script.
    *
    * @param scriptName The name of the script
    * @return the source code of the script
     */
   String getScript(String scriptName);

   /**
    * Retrieves the source code of an existing script together with its metadata
    *
    * @param scriptName The name of the script
    * @return the source code of the script
    */
   ScriptWithMetadata getScriptWithMetadata(String scriptName);

   /**
    * Retrieves names of all available scripts.
    *
    * @return {@link Set<String>} containing names of available scripts.
    */
   Set<String> getScriptNames();

   /**
    * Returns whether a script exists
    * @param scriptName the name of the script
    * @return a boolean indicating script existence
    */
   boolean containsScript(String scriptName);

   /**
    * Invoked when a script matching the supplied filter is added or updated.
    * @param filter a predicate which is passed a script name and returns true if the script should be processed.
    * @param consumer a consumer which is passed a script with metadata.
    */
   void onScriptUpdate(Predicate<String> filter, Consumer<String> consumer);
}
