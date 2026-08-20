package com.ouroboros.data.query;

import com.ouroboros.data.dsl.ModelFieldPath;
import com.ouroboros.data.model.DataModelField;
import com.querydsl.core.types.Path;

public final class DefaultProjectionFieldSupport {

  private DefaultProjectionFieldSupport() {
  }

  public static boolean isDirectDefaultProjectionField(DataModelField field) {
    return field != null
        && Boolean.TRUE.equals(field.getValueType().isPhysical())
        && isDirectFieldName(field.getName());
  }

  public static boolean isDirectDefaultProjectionPath(Path<?> path) {
    if (path instanceof ModelFieldPath<?> modelFieldPath) {
      return isDirectDefaultProjectionField(modelFieldPath.getModelField());
    }
    return path != null && isDirectFieldName(path.getMetadata().getName());
  }

  public static boolean isDirectFieldName(String fieldName) {
    return fieldName != null && !fieldName.contains(".");
  }
}
