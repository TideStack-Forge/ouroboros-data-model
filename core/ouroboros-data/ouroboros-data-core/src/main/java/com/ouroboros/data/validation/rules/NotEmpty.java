package com.ouroboros.data.validation.rules;

import java.util.Optional;

import com.ouroboros.data.validation.Rule;
import com.ouroboros.data.validation.RuleBuilder;

public class NotEmpty implements Rule, RuleBuilder {
  @Override
  public String getFailMessage(String fieldName) {
    return String.format("%s不能为空值", fieldName);
  }

  @Override
  public Optional<Rule> build(String ruleString) {
    if ("notEmpty".equalsIgnoreCase(ruleString)
        || "isRequired".equalsIgnoreCase(ruleString)
        || "required".equalsIgnoreCase(ruleString)) {
      return Optional.of(this);
    }
    return Optional.empty();
  }

  @Override
  public Boolean validate(Object value) {
    if (value == null) {
      return false;
    }
    if (value instanceof String str) {
      return !str.isEmpty();
    }
    if (value instanceof Optional<?> opt) {
      return opt.isPresent();
    }
    return true;
  }
}
