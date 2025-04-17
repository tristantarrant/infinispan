package org.infinispan.server.resp.scripting;

public record ErrorInfo(String message, String source, String line, boolean ignoreStatsUpdate) {
}
