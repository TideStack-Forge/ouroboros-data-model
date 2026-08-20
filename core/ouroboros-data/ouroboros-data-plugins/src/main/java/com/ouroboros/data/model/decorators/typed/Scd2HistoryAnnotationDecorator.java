package com.ouroboros.data.model.decorators.typed;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.ouroboros.data.annotation.Scd2History;
import com.ouroboros.data.model.DataModelMeta;
import com.ouroboros.data.model.TypedAnnotationDecoratorContext;
import com.ouroboros.data.model.TypedModelAnnotationDecorator;

public class Scd2HistoryAnnotationDecorator implements TypedModelAnnotationDecorator<Scd2History> {
  private static final String ENABLE_SCD2_HISTORY = "enableScd2History";
  private static final String SCD2_HISTORY_CONFIG = "scd2HistoryConfig";
  private static final String PLUGIN_SCD2_HISTORY = "Scd2History";
  private static final String OWNER = "@Scd2History";

  @Override
  public Class<Scd2History> annotationType() {
    return Scd2History.class;
  }

  @Override
  public DataModelMeta decorate(
      Class<?> modelClass,
      Scd2History annotation,
      DataModelMeta modelMeta,
      TypedAnnotationDecoratorContext context
  ) {
    context.claimPluginDescriptor(PLUGIN_SCD2_HISTORY, OWNER);
    DataModelMeta decorated = modelMeta.deepCopy();
    decorated.setExtraProp(ENABLE_SCD2_HISTORY, true);
    decorated.setExtraProp(SCD2_HISTORY_CONFIG, buildConfig(context, annotation));
    return decorated;
  }

  private Map<String, Object> buildConfig(TypedAnnotationDecoratorContext context, Scd2History annotation) {
    String historyModelFullName = resolveHistoryModelFullName(context, annotation);
    Map<String, Object> config = new LinkedHashMap<>();
    config.put("historyModelFullName", historyModelFullName);
    return config;
  }

  private String resolveHistoryModelFullName(TypedAnnotationDecoratorContext context, Scd2History annotation) {
    boolean hasClassTarget = !Void.class.equals(annotation.historyModel());
    boolean hasStringTarget = StringUtils.isNotBlank(annotation.historyModelName());
    if (hasClassTarget == hasStringTarget) {
      throw context.metadataException(OWNER + " 必须且只能指定 historyModel 或 historyModelName");
    }
    return hasStringTarget
        ? annotation.historyModelName()
        : context.resolveModelName(annotation.historyModel());
  }
}
