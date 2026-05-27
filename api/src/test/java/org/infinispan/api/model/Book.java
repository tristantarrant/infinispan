package org.infinispan.api.model;

import java.io.Serializable;
import java.util.List;

import org.infinispan.api.annotations.indexing.Basic;
import org.infinispan.api.annotations.indexing.Embedded;
import org.infinispan.api.annotations.indexing.Indexed;
import org.infinispan.api.annotations.indexing.Text;

@Indexed
public class Book implements Serializable {

   private String title;
   private String description;
   private Integer publicationYear;
   private Author author;
   private List<Review> reviews;

   public Book(String title, String description, Integer publicationYear, Author author) {
      this.title = title;
      this.description = description;
      this.publicationYear = publicationYear;
      this.author = author;
   }

   public Book(String title, String description, Integer publicationYear, Author author, List<Review> reviews) {
      this.title = title;
      this.description = description;
      this.publicationYear = publicationYear;
      this.author = author;
      this.reviews = reviews;
   }

   @Basic(projectable = true, sortable = true)
   public String getTitle() {
      return title;
   }

   public void setTitle(String title) {
      this.title = title;
   }

   @Text(analyzer = "standard")
   public String getDescription() {
      return description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   @Basic(projectable = true, sortable = true)
   public Integer getPublicationYear() {
      return publicationYear;
   }

   public void setPublicationYear(Integer publicationYear) {
      this.publicationYear = publicationYear;
   }

   @Embedded
   public Author getAuthor() {
      return author;
   }

   public void setAuthor(Author author) {
      this.author = author;
   }

   @Embedded
   public List<Review> getReviews() {
      return reviews;
   }

   public void setReviews(List<Review> reviews) {
      this.reviews = reviews;
   }

   @Override
   public String toString() {
      return "Book{title='" + title + "', publicationYear=" + publicationYear + ", author=" + author + "}";
   }
}
