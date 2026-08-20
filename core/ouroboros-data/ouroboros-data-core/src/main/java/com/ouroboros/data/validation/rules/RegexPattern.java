package com.ouroboros.data.validation.rules;

import java.util.Optional;

import com.ouroboros.data.validation.Rule;
import com.ouroboros.data.validation.RuleBuilder;

public class RegexPattern implements RuleBuilder {

  @Override
  public Optional<Rule> build(String ruleString) {
    if (ruleString.toLowerCase().startsWith("regex:")) {
      return Optional.of(new RegexPatternRule(ruleString.substring(6)));
    }
    return Optional.empty();
  }

  public class RegexPatternRule implements Rule {

    private String pattern;

    public RegexPatternRule(String pattern) {
      this.pattern = pattern;
    }

    @Override
    public String getFailMessage(String fieldName) {
      return String.format("%s正则表达式校验失败", fieldName);
    }

    @Override
    public Boolean validate(Object value) {
      if (value == null) {
        return true;
      }
      return value.toString().matches(pattern);
    }
  }
}
