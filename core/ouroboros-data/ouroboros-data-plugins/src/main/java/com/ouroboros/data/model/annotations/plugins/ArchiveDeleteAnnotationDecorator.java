package com.ouroboros.data.model.annotations.plugins;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.ouroboros.data.model.DataModelMeta;
import com.ouroboros.data.model.PluginDescriptor;
import com.ouroboros.data.model.TypedAnnotationDecoratorContext;
import com.ouroboros.data.model.TypedModelAnnotationDecorator;

public class ArchiveDeleteAnnotationDecorator implements TypedModelAnnotationDecorator<ArchiveDelete> {
  private static final String PLUGIN_ARCHIVE_DELETE = "ArchiveDelete";
  private static final String OWNER = "@ArchiveDelete";

  @Override
  public Class<ArchiveDelete> annotationType() {
    return ArchiveDelete.class;
  }

  @Override
  public DataModelMeta decorate(
      Class<?> modelClass,
      ArchiveDelete annotation,
      DataModelMeta modelMeta,
      TypedAnnotationDecoratorContext context
  ) {
    context.claimPluginDescriptor(PLUGIN_ARCHIVE_DELETE, OWNER);
    DataModelMeta decorated = modelMeta.deepCopy();
    decorated.removePluginDescriptor(PLUGIN_ARCHIVE_DELETE);
    decorated.addPluginDescriptor(new PluginDescriptor(PLUGIN_ARCHIVE_DELETE, buildConfig(context, annotation)));
    return decorated;
  }

  private Map<String, Object> buildConfig(TypedAnnotationDecoratorContext context, ArchiveDelete annotation) {
    String archiveModel = resolveArchiveModel(context, annotation);
    Map<String, Object> config = new LinkedHashMap<>();
    config.put("archiveModel", archiveModel);
    return config;
  }

  private String resolveArchiveModel(TypedAnnotationDecoratorContext context, ArchiveDelete annotation) {
    boolean hasClassTarget = !Void.class.equals(annotation.archiveModel());
    boolean hasStringTarget = StringUtils.isNotBlank(annotation.archiveModelName());
    if (hasClassTarget == hasStringTarget) {
      throw context.metadataException(OWNER + " 必须且只能指定 archiveModel 或 archiveModelName");
    }
    return hasStringTarget
        ? annotation.archiveModelName()
        : context.resolveModelName(annotation.archiveModel());
  }
}
