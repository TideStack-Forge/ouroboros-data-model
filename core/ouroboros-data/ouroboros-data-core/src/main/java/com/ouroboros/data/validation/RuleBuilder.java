package com.ouroboros.data.validation;

import java.util.Optional;

public interface RuleBuilder {
  Optional<Rule> build(String ruleString);
}
