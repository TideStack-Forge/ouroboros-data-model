package com.ouroboros.data.exception;

/**
 * 元数据异常基类
 * <p>
 * 用于处理数据模型元数据相关的错误。
 * 包括模型元数据解析、字段元数据配置、模型配置等错误。
 *
 * @author Song Mingxu
 */
public class MetadataException extends DataModelException {
  public MetadataException() {
    super();
  }

  public MetadataException(String message) {
    super(message);
  }

  public MetadataException(String message, Throwable cause) {
    super(message, cause);
  }

  public MetadataException(Throwable cause) {
    super(cause);
  }

  protected MetadataException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
