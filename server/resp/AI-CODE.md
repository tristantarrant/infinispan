# RESP Module Instructions

This module implements the Redis Serialization Protocol (RESP) for Infinispan. Commands live under `org.infinispan.server.resp.commands`, grouped by data type.

## Compatibility with Redis

Commands must model the original Redis behavior as closely as possible.

- **Documentation:** consult `https://redis.io/docs/latest/commands/<command>/` for the command's functionality, syntax, argument semantics, and examples.
- **Reference Redis instance:** spin up a temporary container to verify command metadata and behavior:
  ```
  docker run -p 6379:6379 --rm redis
  ```
  Then use `redis-cli` (or `COMMAND INFO <command>`) to obtain:
  - **arity** and **key positions** (firstKey, lastKey, step)
  - **ACL categories** (flags)
  - **response types** for each code path (success, error, edge cases)
- **Error messages must match Redis exactly.** Run the command against the container with invalid arguments and copy the error strings verbatim into `handler.writer().customError(...)` calls.

## Adding a new command

### 1. Create the command class

- Place the class in the appropriate subpackage under `commands/` (e.g., `commands/string/`, `commands/hash/`, `commands/bitmap/`).
- Name the class **exactly** after the RESP command in uppercase (e.g., `GETRANGE.java`, `BITCOUNT.java`). Multi-word sub-commands use underscores (e.g., `BITFIELD_RO`, `SORT_RO`).
- Extend `RespCommand` and implement `Resp3Command`.
- For multi-word / family commands (e.g., `CLIENT`, `CLUSTER`), extend `FamilyCommand` instead and implement `getFamilyCommands()`.

### 2. Constructor: arity and key positions

Call `super(arity, firstKeyPos, lastKeyPos, steps, aclMask)`:

| Parameter     | Meaning                                                                                                                          |
|---------------|----------------------------------------------------------------------------------------------------------------------------------|
| `arity`       | Total argument count **including** the command name. Positive = fixed, negative = minimum.                                       |
| `firstKeyPos` | 1-based position of the first key (0 = no keys, e.g., `PING`).                                                                   |
| `lastKeyPos`  | Position of the last key. Use `-1` for unbounded multi-key commands (e.g., `MGET`, `DEL`).                                       |
| `steps`       | Increment between consecutive keys. `1` = consecutive keys, `2` = alternating key/value pairs (e.g., `MSET`).                    |
| `aclMask`     | Combine `AclCategory` flags with `\|` (e.g., `AclCategory.READ.mask() \| AclCategory.STRING.mask() \| AclCategory.FAST.mask()`). |

### 3. Implement `perform()`

```java
@Override
public CompletionStage<RespRequestHandler> perform(
      Resp3Handler handler, ChannelHandlerContext ctx, List<byte[]> arguments) {
   // arguments does NOT include the command name — index 0 is the first user argument.
   // ...
}
```

Key rules:
- **All cache operations are asynchronous.** Return a `CompletionStage` obtained via `handler.stageToReturn(stage, ctx, responseWriter)`.
- **Never block the Netty event loop.** Do not call `.join()` or `.get()` on futures.
- Use `ArgumentUtils` (`toInt`, `toLong`, `toDouble`) for numeric argument parsing — they throw `NumberFormatException` which you must catch and convert to an error response.
- For complex option parsing (flags like `NX`, `XX`, `GT`, `LT`), parse in a `while` loop consuming arguments until a non-option is found.
- When the command needs a type-specific cache, use the appropriate accessor on `Resp3Handler`: `cache()`, `ignorePreviousValuesCache()`, `getHashMapMultimap()`, `getListMultimap()`, `getEmbeddedSetCache()`, `getSortedSeMultimap()`, `getJsonCache()`.

### 4. Error responses

Use `handler.writer()` methods for error responses and return `handler.myStage()`:

| Method                      | When to use               |
|-----------------------------|---------------------------|
| `wrongArgumentNumber(this)` | Wrong number of arguments |
| `syntaxError()`             | Malformed syntax          |
| `customError(String)`       | Domain-specific errors    |
| `valueNotAValidFloat()`     | Invalid numeric argument  |
| `nulls()`                   | Null / nil response       |

### 5. Response writers

Pass a `ResponseWriter` constant to `handler.stageToReturn()`:

- `ResponseWriter.OK` — simple `+OK`
- `ResponseWriter.INTEGER` — integer reply
- `ResponseWriter.DOUBLE` — double reply
- `ResponseWriter.BULK_STRING_BYTES` — single bulk string
- `ResponseWriter.ARRAY_BULK_STRING` — array of bulk strings
- `ResponseWriter.ARRAY_INTEGER` — array of integers

For custom serialization, pass a `BiConsumer<T, ResponseWriter>` lambda.

### 6. Register the command

Add the new command instance to `Commands.java` in the `ALL_COMMANDS` static initializer.

- Commands are bucketed by first letter (index 0–25).
- **Order within a bucket matters**: frequently-used commands (e.g., `GET`, `SET`, `DEL`) must appear first because lookup is sequential.
- If adding a command whose first letter has no bucket yet, create a new array entry.

### 7. Use FunctionalMap for read-modify-write operations

When a command needs to atomically read, modify, and write back a value, use the FunctionalMap API instead of separate `get` + `put` calls. This is especially important in clustered mode: FunctionalMap sends the function to the key's owner node and executes it there atomically, avoiding extra network roundtrips and race conditions.

**Obtaining a FunctionalMap:**
```java
FunctionalMap.ReadWriteMap<byte[], Object> rwMap =
      FunctionalMap.create(handler.typedCache(null)).toReadWriteMap();
```

**Single-key eval:**
```java
CompletableFuture<R> result = rwMap.eval(key, view -> {
   Object current = view.peek().orElse(null);
   // ... compute new value ...
   view.set(newValue);
   return result;
});
```

**When to use each map type:**

| Map type       | Use case                                      | Locking                     |
|----------------|-----------------------------------------------|-----------------------------|
| `ReadOnlyMap`  | Read without modification                     | No locks                    |
| `WriteOnlyMap` | Write without needing the previous value      | Write lock, no remote fetch |
| `ReadWriteMap` | Atomic read-modify-write (most RESP commands) | Full read-write lock        |

**Encapsulate logic in a function class** when the operation is complex. The function must be serializable for cluster distribution. See existing examples: `BloomFilterInsertFunction`, `BitfieldFunction`, `JsonSetFunction`.

### 8. Code reuse

- If two commands share logic, have one extend the other (e.g., `HSET` extends `HMSET`) or extract shared logic into a helper in an `internal/` or `operation/` subpackage.
- For commands with complex option sets, create a dedicated `*Operation` or `*Args` class to keep the command class focused on argument parsing and delegation.

## Writing tests

New commands must be tested in **both standalone and clustered** modes.

### Standalone tests

- Extend `SingleNodeRespBaseTest`.
- Override `factory()` to return both a default and a `.withAuthorization()` variant:
  ```java
  @Override
  public Object[] factory() {
     return new Object[] {
           new MyCommandTest(),
           new MyCommandTest().withAuthorization(),
     };
  }
  ```
- Use `redisConnection.sync()` to obtain a synchronous Lettuce handle.

### Clustered tests

- Extend `BaseMultipleRespTest`.
- The base class sets up a 2-node `DIST_SYNC` cluster with `redisConnection1` and `redisConnection2`.
- Test that data written through one node is readable from the other.
- Override `amendCacheConfiguration(ConfigurationBuilder)` if the command needs specific cache settings.

### General test guidelines

- Tests go in `server/resp/src/test/java/org/infinispan/server/resp/`.
- Use the **Lettuce** Redis client (`io.lettuce.core`).
- Use **AssertJ** assertions (`assertThat(...)`).
- Test **wrong-type errors** using `assertWrongType()` from `RespTestingUtil`.
- When Lettuce does not have a built-in method for the command, use the low-level `dispatch()` API with `CommandType` and `CommandArgs`.
- Use `k()` (from `TestingUtil`) for generating unique per-test key names to avoid test interference.
- Annotate test classes with `@Test(groups = "functional", testName = "server.resp.MyCommandTest")`.
