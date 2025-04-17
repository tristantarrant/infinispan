package org.infinispan.server.resp.scripting;

import java.util.Map;

/**
 * A library of functions. Each library has a name and can contain multiple named functions
 *
 * @param name the name of the library
 * @param functions a {@link Map} of named functions
 */
public record FunctionLibrary(String name, Map<String, CodeFunction> functions) {
}
