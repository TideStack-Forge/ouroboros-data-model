package com.ouroboros.data.exception;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 数据校验异常
 * <p>
 * 保留用于向后兼容，推荐使用 {@link RecordValidationException}。
 *
 * @author Song Mingxu
 * @deprecated 使用 {@link RecordValidationException} 替代
 */
@Deprecated
public class ValidationException extends RecordValidationException {
  private static final long serialVersionUID = 1L;

  public ValidationException(Map<String, List<String>> errors) {
    super(errors);
  }

  public ValidationException(String errorMessage) {
    super(errorMessage, Collections.emptyMap());
  }

  /**
   * @deprecated 使用 {@link #getFieldErrors()} 替代
   */
  @Deprecated
  public Map<String, List<String>> getErrors() {
    return getFieldErrors();
  }

  /**
   * @deprecated 使用 {@link #getJoinedErrors(String)} 替代
   */
  @Deprecated
  public Map<String, String> getJoinedErrors() {
    return getJoinedErrors(",");
  }

  /**
   * @deprecated 使用 {@link #getMessage()} 替代
   */
  @Deprecated
  public String getErrorMessage() {
    return getMessage();
  }
}
