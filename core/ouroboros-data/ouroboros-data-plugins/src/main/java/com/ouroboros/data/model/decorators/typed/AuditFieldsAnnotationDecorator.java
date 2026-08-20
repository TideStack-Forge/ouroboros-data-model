package com.ouroboros.data.model.decorators.typed;

import java.util.LinkedHashMap;
import java.util.Map;

import com.ouroboros.data.annotation.AuditFields;
import com.ouroboros.data.model.DataModelMeta;
import com.ouroboros.data.model.TypedAnnotationDecoratorContext;
import com.ouroboros.data.model.TypedModelAnnotationDecorator;

public class AuditFieldsAnnotationDecorator implements TypedModelAnnotationDecorator<AuditFields> {
  private static final String ENABLE_AUDIT_FIELDS = "enableAuditFields";
  private static final String BASIC_AUDIT_CONFIG = "basicAuditConfig";
  private static final String PLUGIN_BASIC_AUDIT = "BasicAudit";
  private static final String OWNER = "@AuditFields";

  @Override
  public Class<AuditFields> annotationType() {
    return AuditFields.class;
  }

  @Override
  public DataModelMeta decorate(
      Class<?> modelClass,
      AuditFields annotation,
      DataModelMeta modelMeta,
      TypedAnnotationDecoratorContext context
  ) {
    context.claimPluginDescriptor(PLUGIN_BASIC_AUDIT, OWNER);
    DataModelMeta decorated = modelMeta.deepCopy();
    decorated.setExtraProp(ENABLE_AUDIT_FIELDS, true);
    decorated.setExtraProp(BASIC_AUDIT_CONFIG, buildConfig(annotation));
    return decorated;
  }

  private Map<String, Object> buildConfig(AuditFields annotation) {
    Map<String, Object> config = new LinkedHashMap<>();
    putIfNotBlank(config, "createdByRawName", annotation.createdByRawName());
    putIfNotBlank(config, "createdAtRawName", annotation.createdAtRawName());
    putIfNotBlank(config, "updatedByRawName", annotation.updatedByRawName());
    putIfNotBlank(config, "updatedAtRawName", annotation.updatedAtRawName());
    return config;
  }

  private void putIfNotBlank(Map<String, Object> config, String key, String value) {
    if (!value.isBlank()) {
      config.put(key, value);
    }
  }
}
