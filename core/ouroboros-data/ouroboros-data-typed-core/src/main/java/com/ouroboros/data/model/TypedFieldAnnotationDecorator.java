package com.ouroboros.data.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * Decorates metadata for a field-level typed annotation.
 *
 * @param <A> annotation type handled by this decorator
 */
public interface TypedFieldAnnotationDecorator<A extends Annotation> {

  /**
   * @return annotation class handled by this decorator
   */
  Class<A> annotationType();

  /**
   * Decorate field metadata for the annotation instance.
   *
   * @param modelClass source typed model class
   * @param field      source Java field
   * @param annotation annotation instance
   * @param fieldMeta  current field metadata
   * @param modelMeta  current model metadata
   * @param context    decorator context
   * @return decorated field metadata
   */
  DataModelFieldMeta decorate(
      Class<?> modelClass,
      Field field,
      A annotation,
      DataModelFieldMeta fieldMeta,
      DataModelMeta modelMeta,
      TypedAnnotationDecoratorContext context
  );

  /**
   * @return lower values run earlier
   */
  default int getOrder() {
    return 0;
  }

  /**
   * Type-safe bridge for the runtime pipeline.
   *
   * @param modelClass source typed model class
   * @param field      source Java field
   * @param annotation annotation instance
   * @param fieldMeta  current field metadata
   * @param modelMeta  current model metadata
   * @param context    decorator context
   * @return decorated field metadata
   */
  default DataModelFieldMeta decorateAnnotation(
      Class<?> modelClass,
      Field field,
      Annotation annotation,
      DataModelFieldMeta fieldMeta,
      DataModelMeta modelMeta,
      TypedAnnotationDecoratorContext context
  ) {
    return decorate(modelClass, field, annotationType().cast(annotation), fieldMeta, modelMeta, context);
  }
}
