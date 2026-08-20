package com.ouroboros.data.validation;

public interface Rule {
  String getFailMessage(String fieldName);

  Boolean validate(Object value);
}
