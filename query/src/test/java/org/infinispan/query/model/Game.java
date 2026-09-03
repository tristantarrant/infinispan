package org.infinispan.query.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.infinispan.api.annotations.indexing.Basic;
import org.infinispan.api.annotations.indexing.Indexed;
import org.infinispan.api.annotations.indexing.Keyword;
import org.infinispan.api.annotations.indexing.Text;
import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoSchema;

@Indexed(index = "play")
public class Game {

    private final String name;

    private final String description;

    private final int rating;

    private final double price;

    private final List<String> categories;

    private final Date releaseDate;

    public Game(String name, String description) {
       this(name, description, 0, 0.0, new ArrayList<>(), null);
    }

    public Game(String name, String description, int rating) {
       this(name, description, rating, 0.0, new ArrayList<>(), null);
    }

    public Game(String name, String description, int rating, double price) {
       this(name, description, rating, price, new ArrayList<>(), null);
    }

    public Game(String name, String description, int rating, double price, List<String> categories) {
       this(name, description, rating, price, categories, null);
    }

    @ProtoFactory
    public Game(String name, String description, int rating, double price, List<String> categories, Date releaseDate) {
       this.name = name;
       this.description = description;
       this.rating = rating;
       this.price = price;
       this.categories = categories;
       this.releaseDate = releaseDate;
    }

   @Keyword(projectable = true, sortable = true)
   @ProtoField(1)
   public String getName() {
      return name;
   }

   @Text
   @ProtoField(2)
   public String getDescription() {
      return description;
   }

   @Basic(projectable = true, sortable = true)
   @ProtoField(number = 3, defaultValue = "0")
   public int getRating() {
      return rating;
   }

   @Basic(projectable = true, sortable = true)
   @ProtoField(number = 4, defaultValue = "0.0")
   public double getPrice() {
      return price;
   }

    @ProtoField(5)
    public List<String> getCategories() {
       return categories;
    }

    @Basic(projectable = true, sortable = true)
    @ProtoField(6)
    public Date getReleaseDate() {
       return releaseDate;
    }

    @Override
    public String toString() {
       return "Game{" +
             "name='" + name + '\'' +
             ", description='" + description + '\'' +
             ", rating=" + rating +
             ", price=" + price +
             ", categories=" + categories +
             ", releaseDate=" + releaseDate +
             '}';
   }

   @ProtoSchema(includeClasses = {Game.class, NonIndexedGame.class, GameKey.class})
   public interface GameSchema extends GeneratedSchema {
      GameSchema INSTANCE = new GameSchemaImpl();
   }
}
