package com.ouroboros.data.model;

import java.lang.annotation.Annotation;

/**
 * Decorates metadata for a model-level typed annotation.
 *
 * @param <A> annotation type handled by this decorator
 */
public interface TypedModelAnnotationDecorator<A extends Annotation> {

  /**
   * @return annotation class handled by this decorator
   */
  Class<A> annotationType();

  /**
   * Decorate metadata for the annotation instance.
   *
   * @param modelClass source typed model class
   * @param annotation annotation instance
   * @param modelMeta  current model metadata
   * @param context    decorator context
   * @return decorated model metadata
   */
  DataModelMeta decorate(
      Class<?> modelClass,
      A annotation,
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
   * @param annotation annotation instance
   * @param modelMeta  current model metadata
   * @param context    decorator context
   * @return decorated model metadata
   */
  default DataModelMeta decorateAnnotation(
      Class<?> modelClass,
      Annotation annotation,
      DataModelMeta modelMeta,
      TypedAnnotationDecoratorContext context
  ) {
    return decorate(modelClass, annotationType().cast(annotation), modelMeta, context);
  }
}
