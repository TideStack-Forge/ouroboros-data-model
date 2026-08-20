package com.ouroboros.data.normalize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.validation.DefaultRuleFactory;
import com.ouroboros.data.validation.Rule;
import com.ouroboros.data.validation.RuleFactory;
import com.querydsl.core.types.Ops;

class FactoryCoverageTest {

  @Test
  void ruleFactoryUsesSpiBuildersAndFactories() {
    assertFalse(RuleFactory.getRule(null).isPresent());
    assertFalse(RuleFactory.getRule("unknown").isPresent());

    var requiredRule = RuleFactory.getRule("required");
    assertTrue(requiredRule.isPresent());
    assertFalse(requiredRule.get().validate(""));

    var regexRule = RuleFactory.getRule("regex:^a+$");
    assertTrue(regexRule.isPresent());
    assertTrue(regexRule.get().validate("aaa"));
    assertFalse(regexRule.get().validate("bbb"));

    var defaultRuleFactory = new DefaultRuleFactory();
    assertTrue(defaultRuleFactory.apply("notNull").isPresent());
    assertFalse(defaultRuleFactory.apply("missing-rule").isPresent());
  }

  @Test
  void operatorFactoryUsesRegisteredAliases() {
    assertEquals(Ops.EQ, OperatorFactory.getOperator("==").get());
    assertEquals(Ops.AggOps.COUNT_AGG, OperatorFactory.getOperator("count").get());
    assertEquals(Operators.FIELD, OperatorFactory.getOperator("field").get());
    assertFalse(OperatorFactory.getOperator("missing").isPresent());

    var defaultOperatorFactory = new DefaultOperatorFactory();
    assertEquals(Ops.EQ, defaultOperatorFactory.apply("=").get());
    assertEquals(Ops.AggOps.COUNT_AGG, defaultOperatorFactory.apply("COUNT").get());
    assertEquals(Operators.FIELD, defaultOperatorFactory.apply("FIELD").get());
    assertFalse(defaultOperatorFactory.apply("missing").isPresent());
  }
}
