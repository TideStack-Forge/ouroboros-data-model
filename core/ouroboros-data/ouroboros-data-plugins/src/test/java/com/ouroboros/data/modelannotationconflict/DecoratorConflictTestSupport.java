package com.ouroboros.data.modelannotationconflict;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.ouroboros.data.exception.ModelMetadataException;
import com.ouroboros.data.model.DataModelMeta;
import com.ouroboros.data.model.TypedAnnotationDecoratorContext;

public final class DecoratorConflictTestSupport {
  private DecoratorConflictTestSupport() {
  }

  public static DataModelMeta emptyMeta(String modelName) {
    DataModelMeta meta = new DataModelMeta();
    meta.setName(modelName);
    return meta;
  }

  public static TypedAnnotationDecoratorContext context(String modelName) {
    Map<String, String> owners = new HashMap<>();
    return new TypedAnnotationDecoratorContext() {
      @Override
      public String modelName() {
        return modelName;
      }

      @Override
      public String resolveModelName(Class<?> modelClass) {
        return modelClass.getSimpleName();
      }

      @Override
      public void claimPluginDescriptor(String pluginName, String ownerAnnotation) {
        String existingOwner = owners.putIfAbsent(pluginName.toLowerCase(Locale.ROOT), ownerAnnotation);
        if (existingOwner != null && !existingOwner.equals(ownerAnnotation)) {
          throw metadataException(String.format(
              "插件 descriptor 冲突: plugin=%s, owner=%s, conflict=%s",
              pluginName,
              existingOwner,
              ownerAnnotation
          ));
        }
      }

      @Override
      public ModelMetadataException metadataException(String message) {
        return new ModelMetadataException(
            String.format("模型: %s 的 typed annotation 配置错误: %s", modelName(), message),
            modelName()
        );
      }

      @Override
      public ModelMetadataException metadataException(String message, Throwable cause) {
        return new ModelMetadataException(
            String.format("模型: %s 的 typed annotation 配置错误: %s", modelName(), message),
            modelName(),
            cause
        );
      }
    };
  }
}
