package com.ouroboros.common.service.outbox;

import java.time.LocalDateTime;

/** Eligibility for both recovery scans and atomic claims; terminal and paused rows are excluded. */
public final class OutboxLease {
  private OutboxLease() {
  }

  public static boolean isClaimable(String status, Integer retryCount, LocalDateTime nextAttemptAt,
                                     String claimToken, LocalDateTime claimUntil,
                                     int maxRetryCount, LocalDateTime now) {
    if (status == null) {
      return false;
    }
    return switch (status) {
      case "PENDING" -> true;
      case "FAILED" -> (retryCount == null || retryCount <= maxRetryCount)
          && (nextAttemptAt == null || !nextAttemptAt.isAfter(now));
      case "PROCESSING" -> claimToken != null && !claimToken.isBlank()
          && claimUntil != null && !claimUntil.isAfter(now);
      default -> false;
    };
  }
}
