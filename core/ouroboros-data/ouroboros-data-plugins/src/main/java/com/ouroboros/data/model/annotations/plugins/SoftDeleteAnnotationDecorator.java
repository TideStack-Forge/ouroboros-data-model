package com.ouroboros.data.model.annotations.plugins;

import java.util.LinkedHashMap;
import java.util.Map;

import com.ouroboros.data.model.DataModelMeta;
import com.ouroboros.data.model.TypedAnnotationDecoratorContext;
import com.ouroboros.data.model.TypedModelAnnotationDecorator;

public class SoftDeleteAnnotationDecorator implements TypedModelAnnotationDecorator<SoftDelete> {
  private static final String ENABLE_SOFT_DELETE = "enableSoftDelete";
  private static final String SOFT_DELETE_CONFIG = "softDeleteConfig";
  private static final String PLUGIN_LOGICAL_DELETE = "LogicalDelete";
  private static final String PLUGIN_LEGACY_SOFT_DELETE = "SoftDelete";
  private static final String OWNER = "@SoftDelete";

  @Override
  public Class<SoftDelete> annotationType() {
    return SoftDelete.class;
  }

  @Override
  public DataModelMeta decorate(
      Class<?> modelClass,
      SoftDelete annotation,
      DataModelMeta modelMeta,
      TypedAnnotationDecoratorContext context
  ) {
    context.claimPluginDescriptor(PLUGIN_LOGICAL_DELETE, OWNER);
    context.claimPluginDescriptor(PLUGIN_LEGACY_SOFT_DELETE, OWNER);
    DataModelMeta decorated = modelMeta.deepCopy();
    decorated.setExtraProp(ENABLE_SOFT_DELETE, true);
    decorated.setExtraProp(SOFT_DELETE_CONFIG, buildConfig(annotation));
    return decorated;
  }

  private Map<String, Object> buildConfig(SoftDelete annotation) {
    Map<String, Object> config = new LinkedHashMap<>();
    putIfNotBlank(config, "isDeletedRawName", annotation.isDeletedRawName());
    putIfNotBlank(config, "deletedByRawName", annotation.deletedByRawName());
    putIfTrue(config, "deletedByDisabled", annotation.deletedByDisabled());
    putIfNotBlank(config, "deletedAtRawName", annotation.deletedAtRawName());
    putIfTrue(config, "deletedAtDisabled", annotation.deletedAtDisabled());
    return config;
  }

  private void putIfNotBlank(Map<String, Object> config, String key, String value) {
    if (!value.isBlank()) {
      config.put(key, value);
    }
  }

  private void putIfTrue(Map<String, Object> config, String key, boolean value) {
    if (value) {
      config.put(key, true);
    }
  }
}
