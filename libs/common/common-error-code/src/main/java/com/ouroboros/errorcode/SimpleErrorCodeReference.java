package com.ouroboros.errorcode;

import java.util.Objects;

public final class SimpleErrorCodeReference implements ErrorCodeReference {
  private final Integer code;
  private final String bizCode;
  private final String fallbackMessageTemplate;

  public SimpleErrorCodeReference(Integer code, String bizCode, String fallbackMessageTemplate) {
    this.code = code;
    this.bizCode = bizCode;
    this.fallbackMessageTemplate = fallbackMessageTemplate;
  }

  @Override
  public Integer getCode() {
    return code;
  }

  @Override
  public String getBizCode() {
    return bizCode;
  }

  @Override
  public String getFallbackMessageTemplate() {
    return fallbackMessageTemplate;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof SimpleErrorCodeReference)) {
      return false;
    }
    var that = (SimpleErrorCodeReference) obj;
    return Objects.equals(code, that.code)
        && Objects.equals(bizCode, that.bizCode)
        && Objects.equals(fallbackMessageTemplate, that.fallbackMessageTemplate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, bizCode, fallbackMessageTemplate);
  }

  @Override
  public String toString() {
    return "SimpleErrorCodeReference{"
        + "code=" + code
        + ", bizCode='" + bizCode + '\''
        + ", fallbackMessageTemplate='" + fallbackMessageTemplate + '\''
        + '}';
  }
}
