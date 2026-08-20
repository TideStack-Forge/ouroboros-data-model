package com.ouroboros.data.exception;

/**
 * 字段元数据异常
 * <p>
 * 当字段元数据配置错误或解析失败时抛出。
 *
 * @author Song Mingxu
 */
public class FieldMetadataException extends MetadataException {
  private final String fieldName;
  private final String modelName;

  public FieldMetadataException(String message, String fieldName) {
    super(message);
    this.fieldName = fieldName;
    this.modelName = null;
  }

  public FieldMetadataException(String message, String fieldName, String modelName) {
    super(message);
    this.fieldName = fieldName;
    this.modelName = modelName;
  }

  public FieldMetadataException(String message, String fieldName, String modelName, Throwable cause) {
    super(message, cause);
    this.fieldName = fieldName;
    this.modelName = modelName;
  }

  public String getFieldName() {
    return fieldName;
  }

  public String getModelName() {
    return modelName;
  }
}
