package org.infinispan.cli.wizard;

public record Number(String name, String text, int defaultValue) implements Input {
}
