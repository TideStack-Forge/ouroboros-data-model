package com.ouroboros.errorcode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ResolvedErrorDescriptor {
  private final Integer code;
  private final String bizCode;
  private final String fallbackMessageTemplate;
  private final Object[] messageArgs;
  private final String originalMessage;
  private final Object errors;

  private ResolvedErrorDescriptor(Builder builder) {
    this.code = builder.code;
    this.bizCode = builder.bizCode;
    this.fallbackMessageTemplate = builder.fallbackMessageTemplate;
    this.messageArgs = cloneMessageArgs(builder.messageArgs);
    this.originalMessage = builder.originalMessage;
    this.errors = copyErrors(builder.errors);
  }

  public static Builder builder() {
    return new Builder();
  }

  public Integer getCode() {
    return code;
  }

  public String getBizCode() {
    return bizCode;
  }

  public String getFallbackMessageTemplate() {
    return fallbackMessageTemplate;
  }

  public Object[] getMessageArgs() {
    return cloneMessageArgs(messageArgs);
  }

  public String getOriginalMessage() {
    return originalMessage;
  }

  public Object getErrors() {
    return errors;
  }

  private static Object[] cloneMessageArgs(Object[] messageArgs) {
    return messageArgs == null ? null : Arrays.copyOf(messageArgs, messageArgs.length);
  }

  private static Object copyErrors(Object errors) {
    if (errors == null) {
      return null;
    }
    if (errors instanceof Map) {
      return Collections.unmodifiableMap(new LinkedHashMap<>((Map<?, ?>) errors));
    }
    if (errors instanceof List) {
      return Collections.unmodifiableList(new ArrayList<>((List<?>) errors));
    }
    if (errors instanceof Set) {
      return Collections.unmodifiableSet(new LinkedHashSet<>((Set<?>) errors));
    }
    return errors;
  }

  public static final class Builder {
    private Integer code;
    private String bizCode;
    private String fallbackMessageTemplate;
    private Object[] messageArgs;
    private String originalMessage;
    private Object errors;

    private Builder() {
    }

    public Builder code(Integer code) {
      this.code = code;
      return this;
    }

    public Builder bizCode(String bizCode) {
      this.bizCode = bizCode;
      return this;
    }

    public Builder fallbackMessageTemplate(String fallbackMessageTemplate) {
      this.fallbackMessageTemplate = fallbackMessageTemplate;
      return this;
    }

    public Builder messageArgs(Object[] messageArgs) {
      this.messageArgs = cloneMessageArgs(messageArgs);
      return this;
    }

    public Builder originalMessage(String originalMessage) {
      this.originalMessage = originalMessage;
      return this;
    }

    public Builder errors(Object errors) {
      this.errors = copyErrors(errors);
      return this;
    }

    public ResolvedErrorDescriptor build() {
      return new ResolvedErrorDescriptor(this);
    }
  }
}
