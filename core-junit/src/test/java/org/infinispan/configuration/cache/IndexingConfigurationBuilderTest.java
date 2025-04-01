package org.infinispan.configuration.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.infinispan.commons.configuration.Combine;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class IndexingConfigurationBuilderTest {
   @Test
   public void testIndexingEntitiesMerge() {
      ConfigurationBuilder one = new ConfigurationBuilder();
      one.indexing().enable().addIndexedEntities("a", "b");
      ConfigurationBuilder two = new ConfigurationBuilder();
      two.indexing().enable().addIndexedEntities("c", "d");
      two.indexing().read(one.indexing().create(), new Combine(Combine.RepeatedAttributes.MERGE, Combine.Attributes.MERGE));
      IndexingConfiguration cfg = two.indexing().create();
      assertThat(cfg.indexedEntityTypes()).containsAll(List.of("a", "b", "c", "d"));
   }

   @Test
   public void testIndexingEntitiesOverride() {
      ConfigurationBuilder one = new ConfigurationBuilder();
      one.indexing().enable().addIndexedEntities("a", "b");
      ConfigurationBuilder two = new ConfigurationBuilder();
      two.indexing().enable().addIndexedEntities("c", "d");
      two.indexing().read(one.indexing().create(), new Combine(Combine.RepeatedAttributes.OVERRIDE, Combine.Attributes.MERGE));
      IndexingConfiguration cfg = two.indexing().create();
      assertThat(cfg.indexedEntityTypes()).contains("a");
      assertThat(cfg.indexedEntityTypes()).contains("b");
      assertThat(cfg.indexedEntityTypes()).doesNotContain("c");
      assertThat(cfg.indexedEntityTypes()).doesNotContain("d");
   }
}
