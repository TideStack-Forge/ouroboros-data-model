package com.ouroboros.data.exception;

/**
 * 模型元数据异常
 * <p>
 * 当数据模型元数据配置错误或解析失败时抛出。
 *
 * @author Song Mingxu
 */
public class ModelMetadataException extends MetadataException {
  private final String modelName;

  public ModelMetadataException(String message) {
    super(message);
    this.modelName = null;
  }

  public ModelMetadataException(String message, String modelName) {
    super(message);
    this.modelName = modelName;
  }

  public ModelMetadataException(String message, String modelName, Throwable cause) {
    super(message, cause);
    this.modelName = modelName;
  }

  public String getModelName() {
    return modelName;
  }
}
