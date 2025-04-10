package org.infinispan.cli.quarkus;

import org.aesh.command.CommandException;
import org.aesh.command.shell.Shell;
import org.infinispan.cli.benchmark.BenchmarkRunner;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * @since 16.0
 **/
@TargetClass(BenchmarkRunner.class)
@Substitute
public final class SubstituteBenchmarkRunner {

   @Substitute
   public static void run(Shell shell, int threads, String uri, String cache, int keySize, int valueSize, int keySetSize, String mode, String verbosity, int count, String time, int warmupCount, String warmupTime, String timeUnit) throws CommandException {
      throw new UnsupportedOperationException("The native CLI doesn't implement the benchmark command. Use the JVM version instead.");
   }
}
