package com.ouroboros.data.validation.rules;

import java.util.Optional;

import com.ouroboros.data.validation.Rule;
import com.ouroboros.data.validation.RuleBuilder;

public class NotNull implements Rule, RuleBuilder {

  @Override
  public String getFailMessage(String fieldName) {
    return String.format("%s不能为空", fieldName);
  }

  @Override
  public Optional<Rule> build(String ruleString) {
    if ("notNull".equalsIgnoreCase(ruleString)) {
      return Optional.of(this);
    }
    return Optional.empty();
  }

  @Override
  public Boolean validate(Object value) {
    return value != null;
  }
}
