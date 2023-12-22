package org.infinispan.embedded.impl;

import java.util.Objects;

import org.infinispan.api.common.CacheEntryVersion;
import org.infinispan.container.versioning.EntryVersion;

public class EmbeddedCacheEntryVersion implements CacheEntryVersion {
   private final EntryVersion version;

   public EmbeddedCacheEntryVersion(EntryVersion version) {
      this.version = version;
   }

   public EntryVersion unwrap() {
      return version;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof EmbeddedCacheEntryVersion that)) return false;
      return Objects.equals(version, that.version);
   }

   @Override
   public int hashCode() {
      return Objects.hashCode(version);
   }
}
