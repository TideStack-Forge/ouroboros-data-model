package com.ouroboros.data.model.decorators.typed;

import java.util.LinkedHashMap;
import java.util.Map;

import com.ouroboros.data.annotation.LogicalDelete;
import com.ouroboros.data.model.DataModelMeta;
import com.ouroboros.data.model.TypedAnnotationDecoratorContext;
import com.ouroboros.data.model.TypedModelAnnotationDecorator;

public class LogicalDeleteAnnotationDecorator implements TypedModelAnnotationDecorator<LogicalDelete> {
  private static final String LOGICAL_DELETE_ENABLED_PROP = "enableSoftDelete";
  private static final String LOGICAL_DELETE_CONFIG_PROP = "softDeleteConfig";
  private static final String PLUGIN_LOGICAL_DELETE = "LogicalDelete";
  private static final String PLUGIN_LEGACY_SOFT_DELETE = "SoftDelete";
  private static final String OWNER = "@LogicalDelete";

  @Override
  public Class<LogicalDelete> annotationType() {
    return LogicalDelete.class;
  }

  @Override
  public DataModelMeta decorate(
      Class<?> modelClass,
      LogicalDelete annotation,
      DataModelMeta modelMeta,
      TypedAnnotationDecoratorContext context
  ) {
    context.claimPluginDescriptor(PLUGIN_LOGICAL_DELETE, OWNER);
    context.claimPluginDescriptor(PLUGIN_LEGACY_SOFT_DELETE, OWNER);
    DataModelMeta decorated = modelMeta.deepCopy();
    decorated.setExtraProp(LOGICAL_DELETE_ENABLED_PROP, true);
    decorated.setExtraProp(LOGICAL_DELETE_CONFIG_PROP, buildConfig(annotation));
    return decorated;
  }

  private Map<String, Object> buildConfig(LogicalDelete annotation) {
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
