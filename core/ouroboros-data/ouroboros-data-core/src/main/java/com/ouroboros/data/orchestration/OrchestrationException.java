package com.ouroboros.data.orchestration;

/**
 * Orchestration 层异常
 *
 * @author Claude Code
 */
public class OrchestrationException extends RuntimeException {

  public OrchestrationException(String message) {
    super(message);
  }

  public OrchestrationException(String message, Throwable cause) {
    super(message, cause);
  }
}
