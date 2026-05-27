package org.infinispan.api.model;

import java.io.Serializable;

import org.infinispan.api.annotations.indexing.Basic;
import org.infinispan.api.annotations.indexing.Text;

public class Review implements Serializable {

   private String reviewer;
   private String content;
   private Integer rating;

   public Review(String reviewer, String content, Integer rating) {
      this.reviewer = reviewer;
      this.content = content;
      this.rating = rating;
   }

   @Basic(projectable = true, sortable = true)
   public String getReviewer() {
      return reviewer;
   }

   public void setReviewer(String reviewer) {
      this.reviewer = reviewer;
   }

   @Text
   public String getContent() {
      return content;
   }

   public void setContent(String content) {
      this.content = content;
   }

   @Basic(projectable = true, sortable = true)
   public Integer getRating() {
      return rating;
   }

   public void setRating(Integer rating) {
      this.rating = rating;
   }

   @Override
   public String toString() {
      return "Review{reviewer='" + reviewer + "', rating=" + rating + "}";
   }
}
