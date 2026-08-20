package com.ouroboros.data.orchestration;

/**
 * JOIN 能力判断结果
 *
 * @since 1.0.0-beta.2
 */
public class JoinCapabilityResult {
  private final boolean joinable;
  private final JoinStrategy strategy;
  private final String reason;

  private JoinCapabilityResult(boolean joinable, JoinStrategy strategy, String reason) {
    this.joinable = joinable;
    this.strategy = strategy;
    this.reason = reason;
  }

  public static JoinCapabilityResult joinable() {
    return new JoinCapabilityResult(true, JoinStrategy.NATIVE, null);
  }

  public static JoinCapabilityResult notJoinable(String reason) {
    return new JoinCapabilityResult(false, JoinStrategy.SEPARATE, reason);
  }

  public boolean isJoinable() {
    return joinable;
  }

  public JoinStrategy getStrategy() {
    return strategy;
  }

  public String getReason() {
    return reason;
  }
}
