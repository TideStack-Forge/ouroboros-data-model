package com.ouroboros.data.model.decorators.typed;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.ouroboros.data.annotation.DataPlugin;
import com.ouroboros.data.model.DataModelMeta;
import com.ouroboros.data.model.PluginDescriptor;
import com.ouroboros.data.model.TypedAnnotationDecoratorContext;
import com.ouroboros.data.model.TypedModelAnnotationDecorator;

public class DataPluginAnnotationDecorator implements TypedModelAnnotationDecorator<DataPlugin> {
  private static final String OWNER = "@DataPlugin";

  @Override
  public Class<DataPlugin> annotationType() {
    return DataPlugin.class;
  }

  @Override
  public int getOrder() {
    return 1000;
  }

  @Override
  public DataModelMeta decorate(
      Class<?> modelClass,
      DataPlugin annotation,
      DataModelMeta modelMeta,
      TypedAnnotationDecoratorContext context
  ) {
    if (StringUtils.isBlank(annotation.name())) {
      throw context.metadataException("@DataPlugin.name 不能为空");
    }
    context.claimPluginDescriptor(annotation.name(), OWNER);
    DataModelMeta decorated = modelMeta.deepCopy();
    decorated.removePluginDescriptor(annotation.name());
    decorated.addPluginDescriptor(new PluginDescriptor(annotation.name(), buildConfig(annotation.config(), context)));
    return decorated;
  }

  private Map<String, Object> buildConfig(String[] configValues, TypedAnnotationDecoratorContext context) {
    if (configValues.length % 2 != 0) {
      throw context.metadataException("@DataPlugin.config 必须按 key/value 成对配置");
    }
    Map<String, Object> config = new LinkedHashMap<>();
    for (int i = 0; i < configValues.length; i += 2) {
      config.put(configValues[i], configValues[i + 1]);
    }
    return config;
  }
}
