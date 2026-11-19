package com.ouroboros.errorcode;

public interface ErrorCodeReference {

  Integer getCode();

  default String getBizCode() {
    return null;
  }

  default String getFallbackMessageTemplate() {
    return null;
  }
}
