package com.ouroboros.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * 验证异常类，继承自业务异常类
 * 用于处理数据验证失败时抛出的异常
 * 
 * @deprecated Since 1.0.0-beta.2, for removal in 1.0.0-beta.4. 
 *             Use {@link com.ouroboros.data.exception.RecordValidationException} instead for better validation error handling.
 */
@Deprecated
public class ValidationException extends BusinessException {
  // 定义错误代码常量，HTTP状态码422表示无法处理的实体
  private final static int ERROR_CODE = 422;
  // 存储字段验证错误信息的Map，键为字段名，值为错误信息
  private final Map<String, String> errors = new HashMap<>();
  /**
   * 构造方法1：创建带有错误消息的验证异常
   * @param message 错误描述信息
   */
  public ValidationException(String message) {
    super(ERROR_CODE, message);
  }

  /**
   * 构造方法2：创建包含字段验证错误的验证异常
   * @param errors 包含字段名和对应错误信息的Map
   */
  public ValidationException(Map<String, String> errors) {
    super(ERROR_CODE, "");
    this.errors.putAll(errors);
  }

  public Map<String, String> getErrors() {
    return errors;
  }
}
