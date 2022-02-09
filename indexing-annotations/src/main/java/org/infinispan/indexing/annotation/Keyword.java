package org.infinispan.indexing.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorRef;
import org.infinispan.indexing.annotation.option.Aggregable;
import org.infinispan.indexing.annotation.option.Norms;
import org.infinispan.indexing.annotation.option.Options;
import org.infinispan.indexing.annotation.option.Projectable;
import org.infinispan.indexing.annotation.option.Searchable;
import org.infinispan.indexing.annotation.option.Sortable;
import org.infinispan.indexing.annotation.processor.impl.KeywordProcessor;

/**
 * Maps a property to a keyword field in the index, holding a single token (word) of text.
 * <p>
 * On contrary to {@link FullText}, this annotation only creates non-tokenized (single-word) text fields.
 * As a result:
 * <ul>
 *     <li>The field value must be of type String</li>
 *     <li>You cannot assign an analyzer when using this annotation</li>
 *     <li>You can, however, assign a normalizer (which is an analyzer that doesn't perform tokenization)
 *     when using this annotation</li>
 *     <li>This annotation allows to make the field sortable</li>
 * </ul>
 * <p>
 * If you want to index a non-String value, use the {@link Basic} annotation instead.
 * If you want to index a String value, but want the field to be tokenized, use {@link FullText} instead.
 * <p>
 * Simplified version for Infinispan of {@link org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField}
 */
@Documented
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Keyword.List.class)
@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef(type = KeywordProcessor.class, retrieval = BeanRetrieval.CONSTRUCTOR))
public @interface Keyword {

   /**
    * @return The name of the index field.
    */
   String name() default "";

   /**
    * @return A reference to the normalizer to use for this field.
    * Defaults to an empty string, meaning no normalization at all.
    */
   String normalizer() default "";

   /**
    * @return Whether index time scoring information should be stored or not.
    * @see Norms
    */
   Norms norms() default Norms.YES;

   /**
    * @return Whether projections are enabled for this field.
    * @see Basic#projectable()
    * @see Projectable
    */
   Projectable projectable() default Projectable.NO;

   /**
    * @return Whether this field should be sortable.
    * @see Basic#sortable()
    * @see Sortable
    */
   Sortable sortable() default Sortable.NO;

   /**
    * @return Whether this field should be searchable.
    * @see Basic#searchable()
    * @see Searchable
    */
   Searchable searchable() default Searchable.YES;

   /**
    * @return Whether aggregations are enabled for this field.
    * @see Basic#aggregable()
    * @see Aggregable
    */
   Aggregable aggregable() default Aggregable.NO;

   /**
    * @return A value used instead of null values when indexing.
    * @see Basic #indexNullAs()
    */
   String indexNullAs() default Options.DO_NOT_INDEX_NULL;

   @Documented
   @Target({ElementType.METHOD, ElementType.FIELD})
   @Retention(RetentionPolicy.RUNTIME)
   @interface List {
      Keyword[] value();
   }
}
