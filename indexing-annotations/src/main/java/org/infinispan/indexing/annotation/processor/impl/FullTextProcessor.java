package org.infinispan.indexing.annotation.processor.impl;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingFullTextFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.infinispan.indexing.annotation.FullText;
import org.infinispan.indexing.annotation.option.Options;

public class FullTextProcessor implements PropertyMappingAnnotationProcessor<FullText> {

   @Override
   public void process(PropertyMappingStep mapping, FullText annotation, PropertyMappingAnnotationProcessorContext context) {
      String name = annotation.name();
      PropertyMappingFullTextFieldOptionsStep fullTextField = (name.isEmpty()) ?
            mapping.fullTextField() : mapping.fullTextField(name);

      fullTextField.analyzer(annotation.analyzer());

      String searchAnalyzer = annotation.searchAnalyzer();
      if (!searchAnalyzer.isEmpty()) {
         fullTextField.searchAnalyzer(searchAnalyzer);
      }

      fullTextField.norms(Options.norms(annotation.norms()));
      fullTextField.termVector(Options.termVector(annotation.termVector()));
      fullTextField.projectable(Options.projectable(annotation.projectable()));
      fullTextField.searchable(Options.searchable(annotation.searchable()));
   }
}
