package org.infinispan.cli.benchmark;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.aesh.command.CommandException;
import org.aesh.command.shell.Shell;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.runner.options.VerboseMode;

public class BenchmarkRunner {

   public static void run(Shell shell, int threads, String uri, String cache, int keySize, int valueSize, int keySetSize, String mode, String verbosity, int count, String time, int warmupCount, String warmupTime, String timeUnit) throws CommandException {
      OptionsBuilder opt = new OptionsBuilder();
      URI uri0 = URI.create(uri);
      switch (uri0.getScheme()) {
         case "hotrod":
         case "hotrods":
            opt.include(HotRodBenchmark.class.getSimpleName());
            break;
         case "http":
         case "https":
            opt.include(HttpBenchmark.class.getSimpleName());
            break;
         case "redis":
         case "rediss":
            opt.include(RespBenchmark.class.getSimpleName());
            break;
         default:
            throw new IllegalArgumentException("Unknown scheme " + uri0.getScheme());
      }
      opt
            .forks(0)
            .threads(threads)
            .param("uri", uri)
            .param("cacheName", cache)
            .param("keySize", Integer.toString(keySize))
            .param("valueSize", Integer.toString(valueSize))
            .param("keySetSize", Integer.toString(keySetSize))
            .mode(Mode.valueOf(mode))
            .verbosity(VerboseMode.valueOf(verbosity))
            .measurementIterations(count)
            .measurementTime(TimeValue.fromString(time))
            .warmupIterations(warmupCount)
            .warmupTime(TimeValue.fromString(warmupTime))
            .timeUnit(TimeUnit.valueOf(timeUnit));
      try {
         new Runner(opt.build(), new BenchmarkOutputFormat(shell, VerboseMode.valueOf(verbosity))).run();
      } catch (RunnerException e) {
         throw new CommandException(e);
      }
   }
}
