package com.ouroboros.data.model;

import com.ouroboros.data.exception.ModelMetadataException;

/**
 * Environment available while typed annotations decorate metadata.
 */
public interface TypedAnnotationDecoratorContext {

  /**
   * @return source model full name
   */
  String modelName();

  /**
   * Resolve a scanned typed model class to its model name.
   *
   * @param modelClass typed model class
   * @return model full name
   */
  String resolveModelName(Class<?> modelClass);

  /**
   * Claim ownership for a runtime plugin descriptor name.
   *
   * @param pluginName      runtime plugin descriptor name
   * @param ownerAnnotation annotation or decorator claiming the descriptor
   */
  void claimPluginDescriptor(String pluginName, String ownerAnnotation);

  /**
   * Build a model metadata exception scoped to the source model.
   *
   * @param message failure detail
   * @return metadata exception
   */
  ModelMetadataException metadataException(String message);

  /**
   * Build a model metadata exception scoped to the source model.
   *
   * @param message failure detail
   * @param cause   root cause
   * @return metadata exception
   */
  ModelMetadataException metadataException(String message, Throwable cause);
}
