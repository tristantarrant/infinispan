package org.infinispan.api.annotations.indexing;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorRef;
import org.infinispan.api.annotations.indexing.impl.GeoCoordinatesProcessor;
import org.infinispan.api.annotations.indexing.model.Point;
import org.infinispan.api.annotations.indexing.option.Projectable;
import org.infinispan.api.annotations.indexing.option.Sortable;

/**
 * Defines a {@link Point} binding from a type or a property
 * to a {@link Point} field representing a point on earth.
 * <p>
 * If the longitude and latitude information is hosted on two different properties,
 * {@code @GeoCoordinates} must be used on the entity (class level).
 * The {@link Latitude} and {@link Longitude} annotations must mark the properties.
 * <pre><code>
 * &#064;GeoCoordinates(name="home")
 * public class User {
 *     &#064;Latitude
 *     public Double getHomeLatitude() { ... }
 *     &#064;Longitude
 *     public Double getHomeLongitude() { ... }
 * }
 * </code></pre>
 * <p>
 * Alternatively, {@code @GeoCoordinates} can be used on a property of type {@link Point}:
 * <pre><code>
 * public class User {
 *     &#064;GeoCoordinates
 *     public Point getHome() { ... }
 * }
 * </code></pre>
 * <p>
 * Infinispan version of {@link org.hibernate.search.mapper.pojo.bridge.builtin.annotation.GeoPointBinding}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
@Documented
@Repeatable(GeoCoordinates.List.class)
@TypeMapping(processor = @TypeMappingAnnotationProcessorRef(type = GeoCoordinatesProcessor.class, retrieval = BeanRetrieval.CONSTRUCTOR))
@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef(type = GeoCoordinatesProcessor.class, retrieval = BeanRetrieval.CONSTRUCTOR))
public @interface GeoCoordinates {

   /**
    * The name of the index field holding spatial information.
    * <p>
    * If {@code @Point} is hosted on a property, defaults to the property name.
    * If {@code @Point} is hosted on a class, the name must be provided.
    *
    * @return the field name
    */
   String fieldName() default "";

   /**
    * @return Returns an instance of the {@link Projectable} enum, indicating whether projections are enabled for this
    * field. Defaults to {@code Projectable.NO}.
    */
   Projectable projectable() default Projectable.NO;

   /**
    * @return Returns an instance of the {@link Sortable} enum, indicating whether sorts are enabled for this
    * field. Defaults to {@code Sortable.NO}.
    */
   Sortable sortable() default Sortable.NO;

   /**
    * @return The name of the marker this spatial should look into
    * when looking for the {@link Latitude} and {@link Longitude} markers.
    */
   String marker() default "";

   @Retention(RetentionPolicy.RUNTIME)
   @Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
   @Documented
   @interface List {
      GeoCoordinates[] value();
   }
}
