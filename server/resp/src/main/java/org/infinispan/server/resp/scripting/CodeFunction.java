package org.infinispan.server.resp.scripting;

/**
 * A function.
 *
 * @param name the name of the function
 * @param description an optional description
 * @param flags a long representing the or-ed {@link ScriptFlags}
 */
public record CodeFunction(String name, String description, long flags) {
}
