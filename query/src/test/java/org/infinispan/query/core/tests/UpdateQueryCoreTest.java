package org.infinispan.query.core.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.infinispan.configuration.cache.IndexStorage.LOCAL_HEAP;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.infinispan.Cache;
import org.infinispan.commons.api.CacheContainerAdmin;
import org.infinispan.commons.api.query.Query;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.query.model.Game;
import org.infinispan.test.SingleCacheManagerTest;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "functional", testName = "query.core.tests.UpdateQueryCoreTest")
public class UpdateQueryCoreTest extends SingleCacheManagerTest {

   private static final String ENTITY = "org.infinispan.query.model.Game";

   private Cache<String, Game> gameCache;

   @Override
   protected EmbeddedCacheManager createCacheManager() {
      ConfigurationBuilder builder = getDefaultStandaloneCacheConfig(false);
      builder.encoding().mediaType(MediaType.APPLICATION_PROTOSTREAM_TYPE)
            .indexing().enable().storage(LOCAL_HEAP)
            .addIndexedEntity(ENTITY);

      cacheManager = TestCacheManagerFactory.createCacheManager(Game.GameSchema.INSTANCE, null);
      gameCache = cacheManager.administration()
            .withFlags(CacheContainerAdmin.AdminFlag.VOLATILE)
            .getOrCreateCache("update-test", builder.build());
      return cacheManager;
   }

    @BeforeMethod
    public void populateCache() {
       gameCache.clear();
       gameCache.put("g1", new Game("Civilization", "The best strategy game", 90, 59.99,
             List.of("strategy", "turn-based", "4x"), date(2021, Calendar.OCTOBER, 5)));
       gameCache.put("g2", new Game("Doom", "First person shooter classic", 85, 39.99,
             List.of("shooter", "action", "fps"), date(1993, Calendar.MARCH, 10)));
       gameCache.put("g3", new Game("Tetris", "Puzzle game with blocks", 95, 19.99,
             List.of("puzzle", "arcade", "classic"), date(1984, Calendar.JUNE, 6)));
    }

    private static Date date(int year, int month, int day) {
       Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
       c.clear();
       c.set(year, month, day);
       return c.getTime();
    }

   @Test
   public void testUpdateSetStringField() {
      Query<Game> update = gameCache.query(
            "update from " + ENTITY + " set name = 'Civilization VI' where name = 'Civilization'");
      int count = update.executeStatement();

      assertThat(count).isEqualTo(1);

      Game result = gameCache.get("g1");
      assertThat(result.getName()).isEqualTo("Civilization VI");
      assertThat(result.getDescription()).isEqualTo("The best strategy game");
   }

   @Test
   public void testUpdateMultipleFields() {
      Query<Game> update = gameCache.query(
            "update from " + ENTITY + " set name = 'DOOM Eternal', set description = 'Rip and tear' where name = 'Doom'");
      int count = update.executeStatement();

      assertThat(count).isEqualTo(1);

      Game result = gameCache.get("g2");
      assertThat(result.getName()).isEqualTo("DOOM Eternal");
      assertThat(result.getDescription()).isEqualTo("Rip and tear");
   }

   @Test
   public void testUpdateDoesNotAffectNonMatching() {
      Query<Game> update = gameCache.query(
            "update from " + ENTITY + " set name = 'changed' where name = 'nonexistent'");
      int count = update.executeStatement();

      assertThat(count).isEqualTo(0);

      assertThat(gameCache.get("g1").getName()).isEqualTo("Civilization");
      assertThat(gameCache.get("g2").getName()).isEqualTo("Doom");
      assertThat(gameCache.get("g3").getName()).isEqualTo("Tetris");
   }

   @Test
   public void testUpdateSetFieldToNull() {
      Query<Game> update = gameCache.query(
            "update from " + ENTITY + " set description = null where name = 'Tetris'");
      int count = update.executeStatement();

      assertThat(count).isEqualTo(1);

      Game result = gameCache.get("g3");
      assertThat(result.getDescription()).isNull();
      assertThat(result.getName()).isEqualTo("Tetris");
   }

   @Test
   public void testSelectStillWorksAfterUpdate() {
      gameCache.query("update from " + ENTITY + " set name = 'Updated' where name = 'Civilization'")
            .executeStatement();

      Query<Game> select = gameCache.query("from " + ENTITY + " where name = 'Updated'");
      List<Game> results = select.execute().list();

      assertThat(results).hasSize(1);
      assertThat(results.get(0).getDescription()).isEqualTo("The best strategy game");
   }

   @Test
   public void testUpdateWithNamedParameter() {
      Query<Game> update = gameCache.query(
            "update from " + ENTITY + " set name = :newName where name = :oldName");
      update.setParameter("newName", "Civilization VII");
      update.setParameter("oldName", "Civilization");
      int count = update.executeStatement();

      assertThat(count).isEqualTo(1);

      Game result = gameCache.get("g1");
      assertThat(result.getName()).isEqualTo("Civilization VII");
      assertThat(result.getDescription()).isEqualTo("The best strategy game");
   }

    @Test
    public void testCacheGetAfterUpdate() {
       gameCache.query("update from " + ENTITY + " set name = 'Civ VI' where name = 'Civilization'")
             .executeStatement();

       Game result = gameCache.get("g1");
       assertThat(result.getName()).isEqualTo("Civ VI");
       assertThat(result.getDescription()).isEqualTo("The best strategy game");
       assertThat(gameCache.size()).isEqualTo(3);
    }

    // --- Expression tests ---

    @Test
    public void testExpressionRatingIncrement() {
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " set rating = rating + 1 where name = 'Civilization'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g1").getRating()).isEqualTo(91);
    }

    @Test
    public void testExpressionRatingDecrement() {
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " set rating = rating - 5 where name = 'Doom'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g2").getRating()).isEqualTo(80);
    }

    @Test
    public void testExpressionRatingMultiply() {
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " set rating = rating * 2 where name = 'Tetris'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g3").getRating()).isEqualTo(190);
    }

    @Test
    public void testExpressionStringConcatenation() {
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " set name = name + ' Remastered' where name = 'Doom'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g2").getName()).isEqualTo("Doom Remastered");
    }

    @Test
    public void testExpressionFunctionUpper() {
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " set name = upper(name) where name = 'Doom'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g2").getName()).isEqualTo("DOOM");
    }

    @Test
    public void testExpressionFunctionConcat() {
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " set name = concat(name, ' - ', description) where name = 'Civilization'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g1").getName()).isEqualTo("Civilization - The best strategy game");
    }

    @Test
    public void testExpressionWithParameter() {
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " set rating = rating + :increment where name = 'Civilization'");
       update.setParameter("increment", 10);
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g1").getRating()).isEqualTo(100);
    }

    @Test
    public void testExpressionMultipleOperations() {
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " set rating = rating + 1, set description = concat(description, ' +1') where name = 'Civilization'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       Game g = gameCache.get("g1");
       assertThat(g.getRating()).isEqualTo(91);
       assertThat(g.getDescription()).isEqualTo("The best strategy game +1");
    }

    @Test
    public void testExpressionArithmeticPrecedence() {
       // rating = 90, set rating = rating + 2 * 3 = 90 + 6 = 96
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " set rating = rating + 2 * 3 where name = 'Civilization'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g1").getRating()).isEqualTo(96);
    }

    @Test
    public void testExpressionParenthesizedExpression() {
       // rating = 90, set rating = (rating + 2) * 3 = 92 * 3 = 276
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " set rating = (rating + 2) * 3 where name = 'Civilization'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g1").getRating()).isEqualTo(276);
    }

    @Test
    public void testExpressionDivision() {
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " set rating = rating / 2 where name = 'Tetris'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g3").getRating()).isEqualTo(47);
    }

    @Test
    public void testExpressionModulo() {
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " set rating = rating % 7 where name = 'Tetris'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g3").getRating()).isEqualTo(4);
    }

    @Test
    public void testExpressionPropertyReferenceCopy() {
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " set description = name where name = 'Doom'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g2").getDescription()).isEqualTo("Doom");
    }

    // --- Double field expression tests ---

    @Test
    public void testExpressionDoubleAddition() {
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " set price = price + 10.5 where name = 'Civilization'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g1").getPrice()).isCloseTo(70.49, within(1e-10));
    }

    @Test
    public void testExpressionDoubleMultiplication() {
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " set price = price * 1.5 where name = 'Doom'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g2").getPrice()).isCloseTo(59.985, within(1e-10));
    }

    @Test
    public void testExpressionRound() {
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " set rating = round(price) where name = 'Civilization'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g1").getRating()).isEqualTo(60);
    }

    @Test
    public void testExpressionFloor() {
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " set price = floor(price) where name = 'Tetris'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g3").getPrice()).isEqualTo(19.0);
    }

    @Test
    public void testExpressionCeil() {
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " set price = ceil(price) where name = 'Doom'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g2").getPrice()).isEqualTo(40.0);
    }

    // --- ADD/REMOVE collection tests ---

    @Test
    public void testAddSingleValue() {
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " add categories = 'rpg' where name = 'Civilization'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g1").getCategories()).containsExactly("strategy", "turn-based", "4x", "rpg");
    }

    @Test
    public void testAddMultipleValues() {
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " add categories = ('rpg', 'sandbox') where name = 'Doom'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g2").getCategories()).containsExactly("shooter", "action", "fps", "rpg", "sandbox");
    }

    @Test
    public void testRemoveSingleValue() {
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " remove categories = 'turn-based' where name = 'Civilization'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g1").getCategories()).containsExactly("strategy", "4x");
    }

    @Test
    public void testRemoveMultipleValues() {
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " remove categories = ('action', 'fps') where name = 'Doom'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g2").getCategories()).containsExactly("shooter");
    }

    @Test
    public void testAddAndRemove() {
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " add categories = 'puzzle', remove categories = 'arcade' where name = 'Tetris'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g3").getCategories()).containsExactly("puzzle", "classic", "puzzle");
    }

    @Test
    public void testAddWithExpression() {
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " add categories = concat('rating-', toLong(rating)) where name = 'Civilization'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g1").getCategories()).contains("rating-90");
    }

    @Test
    public void testAddDoesNotAffectNonMatching() {
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " add categories = 'new' where name = 'nonexistent'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(0);
       assertThat(gameCache.get("g1").getCategories()).containsExactly("strategy", "turn-based", "4x");
       assertThat(gameCache.get("g2").getCategories()).containsExactly("shooter", "action", "fps");
       assertThat(gameCache.get("g3").getCategories()).containsExactly("puzzle", "arcade", "classic");
    }

    @Test
    public void testMixedSetAndAdd() {
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " set rating = 99, add categories = 'updated' where name = 'Civilization'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       Game g = gameCache.get("g1");
       assertThat(g.getRating()).isEqualTo(99);
       assertThat(g.getCategories()).containsExactly("strategy", "turn-based", "4x", "updated");
    }

    @Test
    public void testMixedSetAndRemove() {
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " set rating = 50, remove categories = 'fps' where name = 'Doom'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       Game g = gameCache.get("g2");
       assertThat(g.getRating()).isEqualTo(50);
       assertThat(g.getCategories()).containsExactly("shooter", "action");
    }

    @Test
    public void testMixedAddAndAdd() {
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " add categories = 'rpg', add categories = ('sandbox', 'open-world') where name = 'Tetris'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g3").getCategories())
             .containsExactly("puzzle", "arcade", "classic", "rpg", "sandbox", "open-world");
    }

    @Test
    public void testMixedRemoveAndRemove() {
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " remove categories = 'strategy', remove categories = ('turn-based', '4x') where name = 'Civilization'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g1").getCategories()).isEmpty();
    }

    @Test
    public void testMixedSetAddRemove() {
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " set rating = 100, add categories = 'russian', remove categories = 'arcade' where name = 'Tetris'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       Game g = gameCache.get("g3");
       assertThat(g.getRating()).isEqualTo(100);
       assertThat(g.getCategories()).containsExactly("puzzle", "classic", "russian");
    }

    @Test
    public void testRegexpReplaceDescription() {
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " set description = regexp_replace(description, 'game', 'title') where name = 'Tetris'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g3").getDescription()).isEqualTo("Puzzle title with blocks");
    }

    @Test
    public void testRegexpReplaceMultipleOccurrences() {
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " set description = regexp_replace(description, 's', 'S') where name = 'Doom'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g2").getDescription()).isEqualTo("FirSt perSon Shooter claSSic");
    }

    @Test
    public void testRegexpSubstrNoMatch() {
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " set description = regexp_substr(description, '[0-9]+') where name = 'Doom'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g2").getDescription()).isNull();
    }

    @Test
    public void testRegexpInstr() {
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " set rating = regexp_instr(description, 'shooter') where name = 'Doom'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g2").getRating()).isEqualTo(14);
    }

    // --- Date function tests ---

    @Test
    public void testReleaseDateRoundTrip() {
       Game g = gameCache.get("g2");
       assertThat(g.getReleaseDate()).isEqualTo(date(1993, Calendar.MARCH, 10));
    }

    @Test
    public void testDateAddDays() {
       // Doom release: 1993-03-10, add 30 days → 1993-04-09
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " set releaseDate = adddays(releaseDate, 30) where name = 'Doom'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g2").getReleaseDate()).isEqualTo(date(1993, Calendar.APRIL, 9));
    }

    @Test
    public void testDateAddHours() {
       // Tetris release: 1984-06-06T00:00:00Z, add 90 hours → 1984-06-09T18:00:00Z
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " set releaseDate = addhours(releaseDate, 90) where name = 'Tetris'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       Calendar expected = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
       expected.clear();
       expected.set(1984, Calendar.JUNE, 9, 18, 0, 0);
       assertThat(gameCache.get("g3").getReleaseDate()).isEqualTo(expected.getTime());
    }

    @Test
    public void testDateAddMinutes() {
       // Civilization release: 2021-10-05T00:00:00Z, add 90 minutes → 2021-10-05T01:30:00Z
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " set releaseDate = addminutes(releaseDate, 90) where name = 'Civilization'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       Calendar expected = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
       expected.clear();
       expected.set(2021, Calendar.OCTOBER, 5, 1, 30, 0);
       assertThat(gameCache.get("g1").getReleaseDate()).isEqualTo(expected.getTime());
    }

    @Test
    public void testDateAddMonths() {
       // Tetris release: 1984-06-06, add 6 months → 1984-12-06
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " set releaseDate = addmonths(releaseDate, 6) where name = 'Tetris'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g3").getReleaseDate()).isEqualTo(date(1984, Calendar.DECEMBER, 6));
    }

    @Test
    public void testDateAddYears() {
       // Doom release: 1993-03-10, add 30 years → 2023-03-10 (anniversary)
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " set releaseDate = addyears(releaseDate, 30) where name = 'Doom'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g2").getReleaseDate()).isEqualTo(date(2023, Calendar.MARCH, 10));
    }

    @Test
    public void testDateAddWithUnitString() {
       // Civilization release: 2021-10-05, dateadd 2 weeks → 2021-10-19
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " set releaseDate = dateadd(releaseDate, 2, 'weeks') where name = 'Civilization'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g1").getReleaseDate()).isEqualTo(date(2021, Calendar.OCTOBER, 19));
    }

    @Test
    public void testDateAddNegativeDays() {
       // Doom release: 1993-03-10, subtract 10 days → 1993-03-00? No: 1993-02-28
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " set releaseDate = adddays(releaseDate, -10) where name = 'Doom'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g2").getReleaseDate()).isEqualTo(date(1993, Calendar.FEBRUARY, 28));
    }

    @Test
    public void testDateAddNullField() {
       // Set releaseDate to null first, then adddays should return null
       gameCache.put("g4", new Game("Minecraft", "Block game", 92, 29.99,
             List.of("sandbox", "survival"), null));

       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " set releaseDate = adddays(releaseDate, 5) where name = 'Minecraft'");
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g4").getReleaseDate()).isNull();
    }

    @Test
    public void testDateSetFromParameter() {
       Query<Game> update = gameCache.query(
             "update from " + ENTITY + " set releaseDate = :newDate where name = 'Tetris'");
       update.setParameter("newDate", date(1989, Calendar.DECEMBER, 31).getTime());
       int count = update.executeStatement();

       assertThat(count).isEqualTo(1);
       assertThat(gameCache.get("g3").getReleaseDate()).isEqualTo(date(1989, Calendar.DECEMBER, 31));
    }
}
