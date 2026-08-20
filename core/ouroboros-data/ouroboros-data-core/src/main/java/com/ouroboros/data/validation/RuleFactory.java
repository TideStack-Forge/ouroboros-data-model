package com.ouroboros.data.validation;

import java.util.Optional;
import java.util.function.Function;

import com.ouroboros.data.util.DataServices;

@SuppressWarnings("unused")
public interface RuleFactory extends Function<String, Optional<Rule>> {
  RuleBuilder RULE_BUILDER_CHAIN = DataServices.getSortedServiceStream(RuleBuilder.class)
      .reduce(s -> Optional.empty(), (prev, curr) -> (String ruleString) -> {
        var rule = prev.build(ruleString);
        if (rule.isPresent()) {
          return rule;
        }
        return curr.build(ruleString);
      });

  static Optional<Rule> getRule(String ruleString) {
    return Optional.ofNullable(ruleString)
        .flatMap(RULE_BUILDER_CHAIN::build);
  }
}
