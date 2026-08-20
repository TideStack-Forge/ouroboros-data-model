package com.ouroboros.data.exception;

/**
 * 主键生成异常
 * <p>
 * 当主键生成过程中发生错误时抛出。
 *
 * @author Song Mingxu
 */
public class PrimaryKeyGenerationException extends DataValidationException {
  private final String generatorName;

  public PrimaryKeyGenerationException(String message) {
    super(message);
    this.generatorName = null;
  }

  public PrimaryKeyGenerationException(String message, String generatorName) {
    super(message);
    this.generatorName = generatorName;
  }

  public PrimaryKeyGenerationException(String message, String generatorName, Throwable cause) {
    super(message, cause);
    this.generatorName = generatorName;
  }

  public PrimaryKeyGenerationException(String message, Throwable cause) {
    super(message, cause);
    this.generatorName = null;
  }

  public String getGeneratorName() {
    return generatorName;
  }
}
