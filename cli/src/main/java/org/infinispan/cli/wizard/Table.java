package org.infinispan.cli.wizard;

public record Table(String name, String text, Input... cells) implements Input {
}
