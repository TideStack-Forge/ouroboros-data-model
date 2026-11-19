package com.ouroboros.common.service.outbox;

import java.time.LocalDateTime;

/** Retry and diagnostic policy shared by approval facts and business subscription deliveries. */
public final class OutboxBackoff {

  /** 错误信息截断长度上限（字符）。 */
  private static final int MAX_ERROR_LENGTH = 2000;

  /** 指数退避分钟数上限。 */
  private static final long MAX_BACKOFF_MINUTES = 60L;

  private OutboxBackoff() {
  }

  /** 下次重试时间：2^retry 分钟（指数退避，上限 60 分钟）。 */
  public static LocalDateTime nextAttemptAt(int retryCount) {
    return nextAttemptAt(retryCount, LocalDateTime.now());
  }

  public static LocalDateTime nextAttemptAt(int retryCount, LocalDateTime now) {
    return now.plusMinutes(
        Math.min((long) Math.pow(2, retryCount), MAX_BACKOFF_MINUTES));
  }

  /** 错误信息截断：超过 2000 字截断（null 原样返回）。 */
  public static String truncateError(String error) {
    return error == null ? null
        : error.length() > MAX_ERROR_LENGTH ? error.substring(0, MAX_ERROR_LENGTH) : error;
  }

  /** 死信判定：下次重试计数超过最大重试次数 → true。 */
  public static boolean isDead(int nextRetryCount, int maxRetryCount) {
    return nextRetryCount > maxRetryCount;
  }
}
