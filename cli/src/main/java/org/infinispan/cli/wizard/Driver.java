package org.infinispan.cli.wizard;

import java.util.Optional;

public interface Driver {
   Optional<Values> run();
}
