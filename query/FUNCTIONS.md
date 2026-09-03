# UPDATE Statement Expressions — Investigation & Plan

Status: design (not yet implemented)
Scope: `query/` module only
Goal: allow expressions on the right-hand side of `SET`/`ADD`/`REMOVE` in Ickle
`UPDATE` statements — increments/decrements, math, date manipulation, string
concatenation, and (deferred) user-defined functions.

---

## 1. Background

Today, update statements only allow **constant value assignments**:

```
update from E set a = 1, b = 'x' where ...
```

`set a = a + 1`, `set name = first + ' ' + last`, `set due = due + 1d` are all
parse errors. The restriction is structural (grammar + visitor), not a runtime
check.

## 2. Current architecture

### 2.1 Flow

```
"update from E set a = 1 where ..."
  → IckleParser.g4 updateValue (L72-76)          grammar accepts ONLY constants
  → QueryParser.parseQuery                        ANTLR errors → ISPN028526
  → IckleParserVisitorResult.visitUpdateOperation (L210-237)
      parseUpdateValues / parseConstantValue (L239-276) → raw List<Object>
  → IckleParsingResult.UpdateOperation
        { UpdateOperationType type, String[] propertyPath, List<Object> values }
  → EmbeddedQuery / HybridQuery / IndexedQueryImpl / CQUpdate (clustered)
  → UpdateQueryHelper.toProtobufOps() + applyUpdate()     ← single choke point
```

File map (all `query/src/main/java` unless noted):

| Concern | Location |
|---|---|
| Grammar | `src/main/antlr4/org/infinispan/query/grammar/IckleParser.g4`, `IckleLexer.g4` |
| Parse → AST | `org/infinispan/query/objectfilter/impl/syntax/parser/IckleParserVisitorResult.java` |
| Shape checks | `.../syntax/parser/IckleQueryStringParser.java` (L29-36) |
| Parse result | `.../syntax/parser/IckleParsingResult.java` (`UpdateOperation` L45-72) |
| Execution (shared) | `org/infinispan/query/core/impl/UpdateQueryHelper.java` |
| Execution (non-indexed) | `.../core/impl/EmbeddedQuery.java` (`executeUpdate` L182-207) |
| Execution (indexed) | `.../impl/IndexedQueryImpl.java` (`executeUpdate` ~L282-307) |
| Execution (hybrid) | `.../core/impl/HybridQuery.java` (`executeUpdate` L129-156) |
| Execution (clustered) | `.../clustered/commandworkers/CQUpdate.java` |
| Predicate AST (WHERE) | `.../objectfilter/impl/syntax/*.java` |
| Type coercion util | `.../syntax/ConstantValueExpr.java` (`getConstantValueAs` L74-155) |
| Date format helper | `.../objectfilter/impl/util/DateHelper.java` |
| Tests | `src/test/.../ql/test/UpdateGrammarTest.java`, `src/test/.../core/tests/UpdateQueryCoreTest.java` |

### 2.2 Key findings

| # | Finding | Implication |
|---|---------|-------------|
| 1 | Constants-only restriction is purely structural: grammar rule `updateValue` (IckleParser.g4:72) + visitor (`IckleParserVisitorResult` L239-276). No runtime check. | Expressions = grammar + visitor + a new value-AST. Nothing else gates it. |
| 2 | All 5 execution paths funnel through `UpdateQueryHelper.toProtobufOps` + `applyUpdate` (EmbeddedQuery.java:192, HybridQuery.java:139, IndexedQueryImpl.java:292, CQUpdate.java:51). | One integration point for expression support. |
| 3 | The parse result **never crosses the wire**. `QueryDefinition` (`.../impl/QueryDefinition.java:29-33`) serializes only `queryString` + `statementType`; clustered `CQUpdate` re-parses the query string on the owner node (CQUpdate.java:38-39); Hot Rod/REST send strings. | The expression AST can be plain in-memory Java objects — **no protostream annotations, no serialization design work**. |
| 4 | The grammar has **no arithmetic at all** — `additiveExpression` (IckleParser.g4:444-453) despite its name contains no infix `+ - * /`. Lexer has `PLUS`, `MINUS`, `ASTERISK` tokens (IckleLexer.g4:125-127) but no `SLASH`/`PERCENT`. | Introduce a small arithmetic precedence ladder scoped to update values; existing WHERE parsing untouched. |
| 5 | `ProtobufFieldUpdater.update` (protostream dependency) already does a **full parse-and-rewrite** of the message bytes (collectFields → apply ops → serializeFields). No in-place patching, no public field-read API. | Evaluating via "deserialize entity → compute → write back" costs about the same as today's per-field path. **No protostream changes required.** |
| 6 | Only 3 classes implement the predicate `Visitor<BE,VE>` (SearchQueryMaker, IndexedSearchPredicateDetector, ExprVisitor), tied to the `BooleanExpr`/WHERE compilation machinery. | The SET-value AST should be a **separate hierarchy**, not new `ValueExpr` subtypes — avoids forcing new visit methods onto the predicate pipeline. |
| 7 | `UpdateQueryHelper` already has reflection get/set of property paths (L213-232), used by the `REFLECTION` strategy. | Reuse it as the property reader for expression evaluation. |
| 8 | Existing date handling (`DateHelper`) and type coercion (`ConstantValueExpr.getConstantValueAs`) live in the query module. | Reuse for date functions and numeric coercion. |

**Feasibility verdict: feasible, moderate effort, no changes to protostream,
commons, core, or any wire format.** All work is contained in the `query/` module.

## 3. Design

### 3.1 Grammar

**Lexer** (`IckleLexer.g4`) — add two tokens alongside the existing operators:

```
SLASH:   '/';
PERCENT: '%';
```

Safe: a bare `/` is a lexer error today, so no valid query regresses.
`REGEXP_LITERAL` (`/.../`) wins via max-munch for full `/re/` matches; only
full-text queries use it. Add lexer tests to lock this down.

**Parser** (`IckleParser.g4`) — replace `updateValue` with an arithmetic ladder:

```
updateValue
    :  updateExpression
    |  LPAREN updateValue (COMMA updateValue)* RPAREN     // tuple (ADD/REMOVE/SET-collection)
    |  LSQUARE updateValue (COMMA updateValue)* RSQUARE   // list
    ;

updateExpression
    :  updateTerm ( (PLUS | MINUS) updateTerm )*
    ;

updateTerm
    :  updateFactor ( (ASTERISK | SLASH | PERCENT) updateFactor )*
    ;

updateFactor
    :  (PLUS | MINUS)? updatePrimary
    ;

updatePrimary
    :  constant
    |  path                    // reference to the entity's current property value
    |  functionCall
    |  LPAREN updateExpression RPAREN
    ;

functionCall
    :  identifier LPAREN (updateExpression (COMMA updateExpression)*)? RPAREN
    ;
```

Notes:
- Standard precedence: `* / %` > `+ -` > unary; left-associative (ANTLR's
  `(op updateTerm)*` visited left-to-right).
- `path` (existing rule) cannot match `f(...)`, so `functionCall` and property
  refs are unambiguous.
- Tuples vs parenthesized expressions: ANTLR adaptive prediction disambiguates
  (`(1, 2)` can't be an expression).
- `ADD`/`REMOVE` use `collectionAssignment: path EQUALS updateValue`, so
  collection ops get expressions for free (`add tags = 'tag-' + id`).
- This deliberately does **not** reuse the boolean `expression` rule (which
  supports WHERE-only constructs like full-text, KNN, geo) — update values get
  their own, smaller ladder.

### 3.2 Value AST — new package `org.infinispan.query.objectfilter.impl.syntax.update`

A standalone hierarchy (does **not** extend `ValueExpr`, so the predicate
`Visitor` is untouched):

```java
public interface UpdateValueExpr {          // root
   Object evaluate(QueryFunctionContext ctx);  // or accept(UpdateExprEvaluator)
   void appendQueryString(StringBuilder sb);
}
```

Nodes (all small, immutable, plain Java — no proto annotations needed per
finding #3):

| Node | Purpose |
|---|---|
| `ConstantUpdateValue(Object value)` | `null` allowed (unlike `ConstantValueExpr`, which rejects null); `Long`, `Double`, `String`, `Boolean` |
| `ParamPlaceholder` | **reuse the existing** `ConstantValueExpr.ParamPlaceholder` as-is |
| `PropertyRefUpdateValue(String[] path)` | reference to the entity's current value; validated at parse time against entity metadata |
| `BinaryArithmeticUpdateValueExpr(Operator op, left, right)` | `ADD, SUBTRACT, MULTIPLY, DIVIDE, MODULO` |
| `NegateUpdateValueExpr(operand)` | unary minus |
| `FunctionCallUpdateValueExpr(String name, List<UpdateValueExpr> args)` | named function with evaluated args |

`UpdateOperation.values` (`List<Object>`) stays as the carrier: each element is
either a legacy raw constant/placeholder (fast path, bit-for-bit today's
behavior) or an `UpdateValueExpr` tree. Detection is one `instanceof` scan.

### 3.3 Expression evaluator

Tree-walking interpreter — the extensibility point:

```java
public final class UpdateValueEvaluator {
   public Object evaluate(UpdateValueExpr expr, QueryFunctionContext ctx) { ... }
}

public final class QueryFunctionContext {
   Object entity;                                  // materialized target entity
   BiFunction<String[], Object, Object> propertyReader;  // path → current value (reflection, reuse UpdateQueryHelper L213)
   Map<String, Object> namedParameters;
   UpdateFunctionRegistry functions;
}
```

**Semantics** (document in the user manual):

- **Numeric arithmetic**: both integer → `long` math (Java overflow-wrap
  semantics); either floating → `double`. Result coerced to the target property
  type on write (extend `convertValue`, UpdateQueryHelper.java:270).
- **`+` with any String operand → concatenation** (Java semantics). This
  sidesteps a lexer conflict: `||` is the `OR` token (IckleLexer.g4:111), so
  `||` is not available for concatenation.
- **NULL propagation** (SQL-like): `null + 1 → null`, `null * 5 → null`; null
  in string concat → null.
- **Division by zero** → per-entry failure via the existing
  `updateByQueryFailed` error path.
- **Pre-image semantics**: all operations are evaluated against the *original*
  entity before any assignment is applied
  (`set a = a+1, b = a*2` sees the old `a` in both) — matches SQL. Implement by
  evaluating all ops first, then applying.
- **Parse-time validation** (fail fast, better DX): property refs in
  `PropertyRefUpdateValue` checked via `propertyHelper.hasProperty` (same as
  WHERE, IckleParserVisitorResult.java:1499-1508); unknown functions rejected
  (see 3.4); named params registered in the existing `namedParameters` map.

### 3.4 Function registry — the UDF extension point

```java
public interface UpdateFunction {
   Object evaluate(List<Object> args, QueryFunctionContext ctx);  // args pre-evaluated
}
public final class UpdateFunctionRegistry {
   static UpdateFunctionRegistry standard();        // built-ins
   UpdateFunctionRegistry with(String name, UpdateFunction f);
}
```

Phase-1 built-ins:

| Group | Functions |
|---|---|
| math | `abs`, `round`, `floor`, `ceil`, `min(a,b)`, `max(a,b)`, `power(a,b)`, `sqrt` |
| string | `concat(a[,b]...)`, `upper`, `lower`, `trim`, `length`, `substring(s,i[,j])`, `replace(s,from,to)` |
| date | `addDays/addHours/addMinutes/addMonths/addYears(d, n)` over `Date`/`Instant` (reusing `DateHelper`); optionally a generic `dateAdd(d, n, 'DAY')` |
| conversion | `toLong`, `toDouble`, `toString` (arithmetic on string-typed numeric columns) |

**UDF path (deferred, but shaped for it)**: the AST stores only a *name*;
resolution happens in the registry at evaluation time, and the registry is
constructed where the query engine already has access to cache configuration.
Later, a `ServiceLoader<UpdateFunctionProvider>` or a cache-config option
injects user functions with zero changes to grammar, AST, or evaluator. The
only thing to decide later is whether unknown-function errors should be
deferrable (for UDFs registered after parse); CQUpdate's re-parse on the owner
node means the *owner's* registry validates, which is the correct behavior.

### 3.5 Execution integration (`UpdateQueryHelper`)

Keep the existing fast path untouched when no value contains an expression (zero
behavior/perf change for current users). When any value is an
`UpdateValueExpr`, per key:

1. **Materialize**: read the entry; if `byte[]`/`WrappedByteArray` →
   `ProtobufUtil.fromWrappedByteArray(serCtx, ...)`; else use the object as-is.
2. **Evaluate** all ops' values via `UpdateValueEvaluator` (propertyReader =
   reflection on the materialized entity; uniform across strategies — the three
   current strategies collapse to "bytes round-trip" vs "in-place object").
3. **Apply** SET/ADD/REMOVE on the object with the existing reflection helpers
   (UpdateQueryHelper.java:187-204; final fields handled via
   `field.setAccessible` as today).
4. **Persist**: re-serialize to wrapped bytes if the entry was bytes, else
   `put` the object.

This is the same cost profile as today's `PROTOBUF_ROUNDTRIP` strategy, and
finding #5 shows the current protobuf path pays a comparable full-rewrite cost
anyway.

Callers (`EmbeddedQuery.executeUpdate`, `HybridQuery`, `IndexedQueryImpl`,
`CQUpdate`) pass `updateOperations` + `namedParameters` through a new
`UpdateQueryHelper` entry point; the 5 call sites change by a few lines each.

### 3.6 Parser-side integration (`IckleParserVisitorResult`)

New visitor methods for the six grammar rules build the AST in
`visitUpdateOperation`'s place (L210-245). No new `Phase` needed in
`VirtualExpressionBuilder` — update values are not predicates.
`IckleQueryStringParser` shape checks (L29-36) unchanged.

## 4. Risks and open questions

1. **Non-atomic read-modify-write** (pre-existing, amplified by expressions).
   `set a = a + 1` is get→compute→put; concurrent updates can lose increments.
   The existing constant SET has the same window, but expressions make counters
   a realistic use case. *Follow-up*: apply via a cache-level mutation
   (`Transformer`/`Mapper`) for true atomicity — larger change, out of scope
   for phase 1, but flag it.
2. **Type coercion on write**: evaluated `Double` into a `long` field —
   truncate or reject? Proposal: reject non-integral doubles with a clear
   error, convert integral ones (documented).
3. **Integer division**: Java truncating semantics vs SQL decimal — proposal:
   truncate (Java users), document.
4. **Lexer regression surface**: `/` now lexes where it previously errored.
   Need full-text + update regression tests (e.g. `x : /re/` still parses;
   `set a = b / c` parses; `set a = 1 / 2` isn't swallowed by anything).
5. **Indexed caches**: index stays consistent via the normal indexing listener
   on put (same as today); no extra work.
6. **Performance**: expression updates always materialize the entity (one extra
   deserialize+serialize vs byte-patch). Acceptable; the constant fast path is
   untouched.

## 5. Phased plan

- **Phase 1 (MVP)**: lexer tokens; grammar ladder; 6 AST nodes; evaluator
  (arithmetic, concat, null propagation, pre-image semantics);
  `UpdateQueryHelper` expression path; ~12 built-in functions; tests:
  `UpdateGrammarTest` (new cases), `UpdateQueryCoreTest`-style e2e with a
  numeric-field entity (current `Game` model has no numerics — add e.g.
  `int plays`), clustered functional test (CQUpdate path re-parses, so it's
  covered by the same engine — add a test to prove it), Hot Rod/REST smoke.
- **Phase 2**: parse-time type checking with precise errors; date + string +
  conversion function sets; documentation; benchmarks proving the constant
  path is unchanged.
- **Phase 3 (deferred)**: UDF registry (ServiceLoader/config); reuse of the
  same AST for computed SELECT projections; atomic application via cache
  mutation.

**Estimate**: phase 1 ≈ grammar (~15 lines) + visitor (~150 LOC) + AST
(~150 LOC) + evaluator & registry (~400 LOC) + helper integration (~150 LOC) +
tests. No changes outside the `query/` module.

## 6. Examples (target syntax)

```sql
-- increment / decrement
update from E set balance = balance + 1 where id = 1
update from E set balance = balance - :amount where id = :id

-- math
update from E set price = price * 1.1 + 5 where id = 1
update from E set qty = (qty / 2) - 1 where id = 1
update from E set score = abs(score) where id = 1

-- string concatenation
update from E set fullName = firstName + ' ' + lastName where id = 1
update from E set label = concat('id-', id) where id = 1

-- date manipulation
update from E set dueDate = addDays(dueDate, 7) where id = 1

-- expressions in collection ops
update from E add tags = 'tag-' + id where id = 1
```
