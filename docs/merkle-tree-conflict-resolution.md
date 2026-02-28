# Merkle Tree Conflict Resolution

## Overview

When an Infinispan cluster recovers from a network partition, the **Conflict Manager**
must identify and resolve entries that diverged across replicas. Previously, this
required fetching **all entries** from **all write owners** for **every segment** —
an expensive operation that scales linearly with data size regardless of how many
entries actually conflict.

This work introduces a three-level hash comparison hierarchy that progressively
narrows the scope of data transfer, dramatically reducing network traffic when
conflicts are sparse (the common case).

```
Level 1: Segment Hash Comparison
  All hashes match? ──► Skip segment entirely (zero entry transfer)
         │
         ▼ mismatch
Level 2: Bucket Hash Comparison
  Compare 32 buckets per segment
  Identify mismatched bucket IDs
         │
         ▼ mismatched buckets
Level 3: Selective Entry Fetch
  Fetch entries only from mismatched buckets (~3% per bucket)
         │
         ▼ fallback on any error
Level 0: Full Segment Fetch (original behavior)
  getAllReplicasForSegment() — fetch everything
```

---

## Hashing Scheme

### Entry Hashing

Each cache entry is hashed by marshalling its key and value to byte arrays and
computing MurmurHash3\_x64\_64 on each:

```
entryHash(entry) = MurmurHash3(keyBytes, 9001) XOR MurmurHash3(valueBytes, 9001)
```

### Segment Hashing

A segment hash is the XOR of all entry hashes within that segment:

```
segmentHash(seg) = entryHash(e1) XOR entryHash(e2) XOR ... XOR entryHash(eN)
```

XOR is **commutative** and **associative**, making the result independent of
iteration order. Two nodes with identical segment contents produce the same hash
regardless of the order entries were inserted.

### Bucket Hashing

Each segment is subdivided into 32 **buckets**. An entry's bucket is determined
solely by its key:

```
bucketId(key) = MurmurHash3(keyBytes, 9001) AND 0x1F    (bitmask for 32 buckets)
```

Each bucket hash is computed the same way as a segment hash, but only over
entries belonging to that bucket:

```
bucketHash(seg, b) = XOR of entryHash(e) for all e where bucketId(e.key) == b
```

Because XOR is associative, the segment hash can be **derived** from bucket
hashes without a separate data container iteration:

```
segmentHash(seg) = bucketHash(seg, 0) XOR bucketHash(seg, 1) XOR ... XOR bucketHash(seg, 31)
entryCount(seg)  = count(seg, 0) + count(seg, 1) + ... + count(seg, 31)
```

This is the key insight that allows a single pass over the data to produce both
bucket-level and segment-level hashes.

---

## Architecture

### Data Structures

```
┌─────────────────────────────────────────────────┐
│                  SegmentHash                     │
│  ┌───────────┬────────────────┬──────────────┐  │
│  │ segmentId │     hash       │  entryCount  │  │
│  │   (int)   │    (long)      │    (int)     │  │
│  └───────────┴────────────────┴──────────────┘  │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                      BucketHash                              │
│  ┌───────────┬──────────┬────────────────┬──────────────┐   │
│  │ segmentId │ bucketId │     hash       │  entryCount  │   │
│  │   (int)   │  (int)   │    (long)      │    (int)     │   │
│  └───────────┴──────────┴────────────────┴──────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

Both records use a combined `matches()` check that compares **hash** and **entry
count**. The entry count acts as a secondary consistency check — two different
data sets could theoretically produce the same XOR hash, but matching both hash
and count makes false positives vanishingly unlikely.

### RPC Commands

```
┌──────────────────────────────────────────────────────────────────────┐
│                    GetBucketHashesCommand                            │
│                                                                      │
│  Request:   cacheName, topologyId, segments (IntSet), bucketCount   │
│  Response:  List<BucketHash>  (flat list, grouped by segmentId)     │
│                                                                      │
│  One RPC per remote node, covering ALL segments that node owns.     │
│  Amortizes network latency across all segments.                      │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                    GetBucketEntriesCommand                            │
│                                                                      │
│  Request:   cacheName, topologyId, segmentId, bucketIds, bucketCount│
│  Response:  List<CacheEntry<?,?>>  (entries from requested buckets) │
│                                                                      │
│  Sent per segment to all remote write owners.                        │
│  Optimization: when bucketIds.size() >= bucketCount, skips the      │
│  bucketForKey() computation and returns all entries.                  │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Flow

### Phase 1: Prefetch Bucket Hashes

When conflict resolution starts, the `ReplicaSpliterator` constructor calls
`prefetchAllBucketHashes()`. This sends **one batched RPC per remote node**
covering all segments that node owns, and computes local bucket hashes in bulk.

```
     ┌─────────┐                ┌─────────┐                ┌─────────┐
     │  Node A  │                │  Node B  │                │  Node C  │
     │ (local)  │                │ (remote) │                │ (remote) │
     └────┬─────┘                └────┬─────┘                └────┬─────┘
          │                           │                           │
          │  GetBucketHashesCommand   │                           │
          │  segments={0,1,3,5,7}     │                           │
          │──────────────────────────►│                           │
          │                           │                           │
          │  GetBucketHashesCommand   │                           │
          │  segments={0,2,4,6,8}     │                           │
          │──────────────────────────────────────────────────────►│
          │                           │                           │
          │ compute local hashes      │ compute bucket hashes     │ compute bucket hashes
          │ for owned segments        │ for requested segments    │ for requested segments
          │                           │                           │
          │   List<BucketHash>        │                           │
          │◄──────────────────────────│                           │
          │                           │                           │
          │                List<BucketHash>                       │
          │◄─────────────────────────────────────────────────────│
          │                           │                           │
          ▼                           │                           │
  Store in prefetched map:            │                           │
  segment → (address → buckets)       │                           │
```

### Phase 2: Per-Segment Comparison (in tryAdvance)

For each segment, `findMismatchedBuckets()` performs a purely local comparison
using the prefetched data. No RPCs are sent at this stage.

```
Segment S: prefetched hashes from Node A, B, C

Step 1 ─ Derive segment hashes from bucket hashes
  ┌──────────┐     ┌──────────┐     ┌──────────┐
  │  Node A   │     │  Node B   │     │  Node C   │
  │SegHash: X │     │SegHash: X │     │SegHash: Y │ ◄── mismatch!
  │Count:  50 │     │Count:  50 │     │Count:  51 │
  └──────────┘     └──────────┘     └──────────┘

Step 2 ─ Check segment size
  maxEntries = 51
  51 ≤ 64 (SMALL_SEGMENT_THRESHOLD)?
    YES → return ALL_BUCKETS (skip per-bucket comparison)
    NO  → proceed to per-bucket comparison

Step 3 ─ Compare individual bucket hashes (large segments only)
  Bucket  0: A=match B=match C=match  →  skip
  Bucket  1: A=match B=match C=match  →  skip
  ...
  Bucket  7: A≠C                      →  MISMATCHED
  ...
  Bucket 31: A=match B=match C=match  →  skip

  Result: IntSet{7} → only bucket 7 needs entry fetch
```

### Phase 3: Selective Entry Fetch

Only entries from mismatched buckets are fetched. The result is formatted
identically to the original `getAllReplicasForSegment()` output.

```
     ┌─────────┐                ┌─────────┐                ┌─────────┐
     │  Node A  │                │  Node B  │                │  Node C  │
     │ (local)  │                │ (remote) │                │ (remote) │
     └────┬─────┘                └────┬─────┘                └────┬─────┘
          │                           │                           │
          │ iterate local entries     │                           │
          │ in segment S where        │                           │
          │ bucketForKey(k) ∈ {7}     │                           │
          │                           │                           │
          │  GetBucketEntriesCommand  │                           │
          │  segment=S, buckets={7}   │                           │
          │──────────────────────────►│                           │
          │──────────────────────────────────────────────────────►│
          │                           │                           │
          │   List<CacheEntry>        │                           │
          │◄──────────────────────────│                           │
          │                List<CacheEntry>                       │
          │◄─────────────────────────────────────────────────────│
          │                           │                           │
          ▼                           │                           │
  Group entries by key:               │                           │
  ┌────────────────────────────────┐  │                           │
  │ key1 → {A: val1, B: val1,     │  │                           │
  │         C: val1_modified}      │  │                           │
  │ key2 → {A: val2, B: NULL,     │  │                           │
  │         C: val2}               │  │                           │
  └────────────────────────────────┘  │                           │
          │                           │                           │
          ▼                           │                           │
  Downstream: filterConsistentEntries + merge policy (unchanged)
```

### Fallback

If any step fails (RPC error, unexpected response, prefetch failure), the system
falls back to the original `getAllReplicasForSegment()` full fetch. The
optimization is **fail-safe** — it can only help, never break correctness.

---

## Optimizations

### 1. Single-Pass Dual Hash

Bucket hashes are computed in a single iteration over the data container.
The segment hash is derived from bucket hashes via XOR associativity, avoiding
a second pass.

```
Single iteration over segment entries:
  ┌─────────────────────────────────────────────────────┐
  │ for each entry:                                      │
  │   bucket = bucketForKey(entry.key, 32)               │
  │   bucketHashes[bucket] ^= hashEntry(entry)           │
  │   bucketCounts[bucket]++                             │
  └─────────────────────────────────────────────────────┘
                         │
                         ▼
  ┌─────────────────────────────────────────────────────┐
  │ segmentHash = bucketHashes[0] ^ ... ^ bucketHashes[31] │
  │ entryCount  = bucketCounts[0] + ... + bucketCounts[31] │
  └─────────────────────────────────────────────────────┘
```

### 2. Batched RPCs

Instead of one RPC per segment per node, a single `GetBucketHashesCommand` is
sent per remote node covering **all segments** that node owns. This amortizes
RPC latency.

```
Before (per-segment):  segments × remoteNodes RPCs
After  (batched):      remoteNodes RPCs (typically 1-2)
```

### 3. Small Segment Threshold

Segments with ≤ 64 entries skip per-bucket narrowing entirely. When a segment
is small, the cost of key marshalling for `bucketForKey()` exceeds the savings
from transferring fewer entries.

```
if maxEntries ≤ 64 → return ALL_BUCKETS (fetch all entries, skip bucket filtering)
if maxEntries > 64 → compare per-bucket hashes, return mismatched bucket IDs
```

### 4. All-Buckets Fast Path

When all 32 buckets are mismatched (or the small segment threshold triggers),
`GetBucketEntriesCommand` and `getReplicasForBuckets()` detect that
`bucketIds.size() >= bucketCount` and skip the `bucketForKey()` computation
entirely, avoiding unnecessary key marshalling.

---

## Component Map

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        DefaultConflictManager                            │
│                                                                          │
│  ReplicaSpliterator                                                      │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │ constructor: prefetchAllBucketHashes()                             │  │
│  │                                                                    │  │
│  │ tryAdvance():                                                      │  │
│  │   for each segment:                                                │  │
│  │     findMismatchedBuckets() ──► skip / bucketIds / null            │  │
│  │       │                                                            │  │
│  │       ├─ empty set ──► skip segment                                │  │
│  │       ├─ non-empty ──► getReplicasForBuckets(bucketIds)            │  │
│  │       └─ null ────────► getAllReplicasForSegment() (fallback)       │  │
│  └────────────────────────────────────────────────────────────────────┘  │
│                                                                          │
│  prefetchAllBucketHashes()                                               │
│    └─► GetBucketHashesCommand (1 per remote node)                        │
│    └─► SegmentHasher.computeAllBucketHashes() (local)                    │
│                                                                          │
│  getReplicasForBuckets()                                                 │
│    └─► GetBucketEntriesCommand (per segment, to remote write owners)     │
│    └─► local data container iteration (filtered by bucket)               │
│    └─► addToReplicaMap() (group by key with NullCacheEntry sentinels)    │
└─────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────┐     ┌────────────────────────────────────┐
│       SegmentHasher         │     │        SegmentHash (record)        │
│                              │     │  segmentId, hash, entryCount      │
│  computeHash(segmentId)      │     │  matches(other)                   │
│  computeBucketHashes(seg,n)  │     └────────────────────────────────────┘
│  computeAllBucketHashes(...)│
│  deriveSegmentHash(seg,bh)  │     ┌────────────────────────────────────┐
│  bucketForKey(key, count)    │     │        BucketHash (record)         │
│  hashEntry(entry)            │     │  segmentId, bucketId, hash, count │
└────────────────────────────┘     │  matches(other)                    │
                                    └────────────────────────────────────┘
```

---

## Cost Analysis

| Scenario | Before | After |
|---|---|---|
| No conflicts (common case) | Fetch all entries from all segments | 1 batched RPC per remote node (hashes only), zero entry transfer |
| 1 conflicting key in 1 segment (10K entries, 256 segments) | 10K entries transferred | ~312 entries transferred (1/32 of segment) |
| All entries conflicting | Full fetch | Full fetch (same as before, plus small hash overhead) |
| Segment with ≤ 64 entries | Full fetch | Direct entry fetch (skips bucket narrowing overhead) |

The hash comparison overhead is minimal: 32 `BucketHash` records (32 × 20 bytes
= 640 bytes) per segment, compared to potentially thousands of serialized cache
entries per segment in the full-fetch path.

---

## File Inventory

| File | Type | Purpose |
|---|---|---|
| `conflict/impl/SegmentHash.java` | Record | Segment-level hash + entry count |
| `conflict/impl/BucketHash.java` | Record | Bucket-level hash + entry count |
| `conflict/impl/SegmentHasher.java` | Service | Hash computation (segment, bucket, key-to-bucket) |
| `commands/conflict/GetBucketHashesCommand.java` | RPC Command | Request bucket hashes from remote nodes |
| `commands/conflict/GetBucketEntriesCommand.java` | RPC Command | Request entries from specific buckets |
| `conflict/impl/DefaultConflictManager.java` | Modified | Three-level comparison flow, prefetch, selective fetch |
| `commands/CommandsFactory.java` | Modified | Factory method declarations |
| `commands/CommandsFactoryImpl.java` | Modified | Factory method implementations |
| `marshall/.../GlobalContextInitializer.java` | Modified | ProtoStream class registration |
| `commons/.../ProtoStreamTypeIds.java` | Modified | Type ID constants |

## Test Inventory

| File | Type | Tests |
|---|---|---|
| `conflict/impl/SegmentHasherTest.java` | Unit | 7 tests: hash properties (empty, non-zero, XOR, order-independence, value-sensitivity, entry count, matches) |
| `conflict/impl/SegmentHasherBucketTest.java` | Unit | 9 tests: bucket assignment, bucket hashing, deriveSegmentHash equivalence |
| `conflict/impl/SegmentHashConflictManagerTest.java` | Integration | 4 tests: end-to-end segment hash skip behavior |
| `conflict/impl/BucketHashConflictManagerTest.java` | Integration | 4 tests: bucket-level conflict detection and resolution |
| `conflict/impl/ConflictManagerTest.java` | Integration | 10 tests: existing conflict manager tests (regression) |
