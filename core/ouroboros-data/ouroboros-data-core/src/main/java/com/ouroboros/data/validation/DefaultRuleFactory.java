package com.ouroboros.data.validation;

import java.util.Optional;

public class DefaultRuleFactory implements RuleFactory {
  @Override
  public Optional<Rule> apply(String ruleString) {
    return RuleFactory.getRule(ruleString);
  }
}
