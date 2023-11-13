package org.infinispan.distribution.ch.impl;

import java.util.Objects;

import org.infinispan.commons.hash.Hash;
import org.infinispan.commons.hash.MurmurHash3;
import org.infinispan.commons.util.Util;
import org.infinispan.configuration.cache.HashConfiguration;
import org.infinispan.distribution.ch.KeyPartitioner;

/**
 * Key partitioner that computes a key's segment based on a hash function.
 *
 * @author Dan Berindei
 * @since 8.2
 */
public class HashFunctionPartitioner implements KeyPartitioner {
   protected Hash hashFunction;
   protected int numSegments;
   private int segmentSize;
   private int mask;


   public HashFunctionPartitioner() {}

   // Should only be used by tests
   public HashFunctionPartitioner(int numSegments) {
      init(numSegments);
   }

   public static HashFunctionPartitioner instance(int numSegments) {
      HashFunctionPartitioner partitioner = new HashFunctionPartitioner();
      partitioner.init(numSegments);
      return partitioner;
   }

   @Override
   public void init(HashConfiguration configuration) {
      Objects.requireNonNull(configuration);
      init(configuration.numSegments());
   }

   @Override
   public void init(KeyPartitioner other) {
      if (other instanceof HashFunctionPartitioner o) {
         if (o.numSegments > 0) { // The other HFP has been initialized, so we can use it
            init(o.numSegments);
         }
      }
   }

   protected void init(int numSegments) {
      if (numSegments <= 0) {
         throw new IllegalArgumentException("numSegments must be strictly positive");
      }
      this.hashFunction = getHash();
      this.numSegments = numSegments;
      int maxBits = Math.min(bitsToUse(), 31);
      this.mask = (1 << maxBits) - 1;

      this.segmentSize = Util.getSegmentSize(maxBits, numSegments);
   }

   protected int bitsToUse() {
      return hashFunction.maxHashBits();
   }

   @Override
   public int getSegment(Object key) {
      int hash = getHashForKey(key);
      return getSegmentForHash(hash);
   }

   public int getHashForKey(Object key) {
      return hashFunction.hash(key) & mask;
   }

   public int getSegmentForHash(int hash) {
      // The result must always be positive, so we make sure the dividend is positive first
      return (hash & Integer.MAX_VALUE) / segmentSize;
   }

   public Hash getHash() {
      return MurmurHash3.getInstance();
   }

   @Override
   public boolean equals(Object o) {
      if (this == o)
         return true;
      if (o == null || getClass() != o.getClass())
         return false;
      HashFunctionPartitioner that = (HashFunctionPartitioner) o;
      if (numSegments != that.numSegments)
         return false;
      return Objects.equals(hashFunction, that.hashFunction);
   }

   @Override
   public int hashCode() {
      int result = hashFunction != null ? hashFunction.hashCode() : 0;
      result = 31 * result + numSegments;
      return result;
   }

   @Override
   public String toString() {
      return "HashFunctionPartitioner{" +
            "hashFunction=" + hashFunction +
            ", ns=" + numSegments +
            '}';
   }
}
