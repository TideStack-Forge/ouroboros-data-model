package com.ouroboros.data.exception;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 记录校验异常
 * <p>
 * 当整条记录的多个字段校验失败时抛出。
 * 包含字段级别的详细错误信息。
 *
 * @author Song Mingxu
 */
public class RecordValidationException extends DataValidationException {
  private static final long serialVersionUID = 1L;
  private final Map<String, List<String>> fieldErrors;

  public RecordValidationException(Map<String, List<String>> fieldErrors) {
    super(formatMessage(fieldErrors));
    this.fieldErrors = Collections.unmodifiableMap(fieldErrors);
  }

  public RecordValidationException(String message, Map<String, List<String>> fieldErrors) {
    super(message);
    this.fieldErrors = Collections.unmodifiableMap(fieldErrors);
  }

  private static String formatMessage(Map<String, List<String>> fieldErrors) {
    if (fieldErrors.isEmpty()) {
      return "Record validation failed";
    }
    return String.format("Record validation failed: %d field(s) have errors", fieldErrors.size());
  }

  /**
   * 获取所有字段的错误信息
   *
   * @return 字段名 -> 错误列表的映射
   */
  public Map<String, List<String>> getFieldErrors() {
    return fieldErrors;
  }

  /**
   * 获取合并后的字段错误（每个字段一个字符串）
   *
   * @param separator 错误消息之间的分隔符
   * @return 字段名 -> 合并错误消息的映射
   */
  public Map<String, String> getJoinedErrors(String separator) {
    return fieldErrors.entrySet().stream()
        .filter(entry -> !entry.getValue().isEmpty())
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> String.join(separator, entry.getValue())
        ));
  }

  /**
   * 获取每个字段的第一个错误
   *
   * @return 字段名 -> 第一个错误消息的映射
   */
  public Map<String, String> getFirstErrors() {
    return fieldErrors.entrySet().stream()
        .filter(entry -> !entry.getValue().isEmpty())
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> entry.getValue().get(0)
        ));
  }
}
