package org.infinispan.cli.commands;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.infinispan.cli.benchmark.BenchmarkRunner;
import org.infinispan.cli.completers.BenchmarkModeCompleter;
import org.infinispan.cli.completers.BenchmarkVerbosityModeCompleter;
import org.infinispan.cli.completers.CacheCompleter;
import org.infinispan.cli.completers.TimeUnitCompleter;
import org.infinispan.cli.impl.ContextAwareCommandInvocation;
import org.kohsuke.MetaInfServices;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 12.0
 **/
@MetaInfServices(Command.class)
@CommandDefinition(name = "benchmark", description = "Benchmarks server performance")
public class Benchmark extends CliCommand {
   @Argument(description = "Specifies the URI of the server to benchmark. Supported protocols are http, https, hotrod, hotrods, redis, rediss. If you do not set a protocol, the benchmark uses the URI of the current connection.")
   String uri;

   @Option(shortName = 't', defaultValue = "10", description = "Specifies the number of threads to create. Defaults to 10.")
   int threads;

   @Option(shortName = 'h', hasValue = false, overrideRequired = true)
   protected boolean help;

   @Option(completer = BenchmarkModeCompleter.class, defaultValue = "Throughput", description = "Specifies the benchmark mode. Possible values are Throughput, AverageTime, SampleTime, SingleShotTime, and All. Defaults to Throughput.")
   String mode;

   @Option(completer = BenchmarkVerbosityModeCompleter.class, defaultValue = "NORMAL", description = "Specifies the verbosity level of the output. Possible values, from least to most verbose, are SILENT, NORMAL, and EXTRA. Defaults to NORMAL.")
   String verbosity;

   @Option(shortName = 'c', defaultValue = "5", description = "Specifies how many measurement iterations to perform. Defaults to 5.")
   int count;

   @Option(defaultValue = "10s", description = "Sets the amount of time, in seconds, that each iteration takes. Defaults to 10.")
   String time;

   @Option(defaultValue = "5", name = "warmup-count", description = "Specifies how many warmup iterations to perform. Defaults to 5.")
   int warmupCount;

   @Option(defaultValue = "1s", name = "warmup-time", description = "Sets the amount of time, in seconds, that each warmup iteration takes. Defaults to 1.")
   String warmupTime;

   @Option(completer = TimeUnitCompleter.class, defaultValue = "MICROSECONDS", name = "time-unit", description = "Specifies the time unit for results in the benchmark report. Possible values are NANOSECONDS, MICROSECONDS, MILLISECONDS, and SECONDS. The default is MICROSECONDS.")
   String timeUnit;

   @Option(completer = CacheCompleter.class, defaultValue = "benchmark", description = "Names the cache against which the benchmark is performed. Defaults to 'benchmark'.")
   String cache;

   @Option(defaultValue = "16", name = "key-size", description = "Sets the size, in bytes, of the key. Defaults to 16 bytes.")
   int keySize;

   @Option(defaultValue = "1000", name = "value-size", description = "Sets the size, in bytes, of the value. Defaults to 1000 bytes.")
   int valueSize;

   @Option(defaultValue = "1000", name = "keyset-size", description = "Defines the size, in bytes, of the test key set. Defaults to 1000.")
   int keySetSize;

   @Override
   public boolean isHelp() {
      return help;
   }

   @Override
   public CommandResult exec(ContextAwareCommandInvocation invocation) throws CommandException {
      if (this.uri == null) {
         if (invocation.getContext().isConnected()) {
            this.uri = invocation.getContext().getConnection().getURI();
         } else {
            throw new IllegalArgumentException("You must specify a URI");
         }
      }
      BenchmarkRunner.run(invocation.getShell(), threads, uri, cache, keySize, valueSize, keySetSize, mode, verbosity, count, time, warmupCount, warmupTime, timeUnit);
      return CommandResult.SUCCESS;
   }
}
