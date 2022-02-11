package org.infinispan.api.annotations.indexing.model;

/**
 * A point in the geocentric coordinate system.
 * <p>
 * Simplified version for Infinispan of {@link org.hibernate.search.engine.spatial.GeoPoint}
 */
public class Point {

   static Point of(double latitude, double longitude) {
      return new Point(latitude, longitude);
   }

   private double latitude;
   private double longitude;

   public Point(double latitude, double longitude) {
      this.latitude = latitude;
      this.longitude = longitude;
   }

   /**
    * @return the latitude, in degrees
    */
   public double latitude() {
      return latitude;
   }

   /**
    * @return the longitude, in degrees
    */
   public double longitude() {
      return longitude;
   }
}
