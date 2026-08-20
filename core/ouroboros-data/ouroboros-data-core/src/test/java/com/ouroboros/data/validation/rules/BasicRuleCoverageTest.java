package com.ouroboros.data.validation.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.validation.Rule;

class BasicRuleCoverageTest {

  @Test
  void notNullMatchesOnlyNotNullKeyword() {
    var rule = new NotNull();

    assertTrue(rule.build("notNull").isPresent());
    assertFalse(rule.build("required").isPresent());
    assertFalse(rule.validate(null));
    assertTrue(rule.validate("value"));
    assertEquals("name不能为空", rule.getFailMessage("name"));
  }

  @Test
  void notEmptyHandlesStringsOptionalsAndAliases() {
    var rule = new NotEmpty();

    assertTrue(rule.build("notEmpty").isPresent());
    assertTrue(rule.build("isRequired").isPresent());
    assertTrue(rule.build("required").isPresent());
    assertFalse(rule.build("other").isPresent());

    assertFalse(rule.validate(null));
    assertFalse(rule.validate(""));
    assertTrue(rule.validate("a"));
    assertFalse(rule.validate(Optional.empty()));
    assertTrue(rule.validate(Optional.of("x")));
    assertTrue(rule.validate(1));
    assertEquals("name不能为空值", rule.getFailMessage("name"));
  }

  @Test
  void regexPatternBuildsRuleAndValidates() {
    var builder = new RegexPattern();

    assertFalse(builder.build("other").isPresent());

    var rule = builder.build("regex:^a.+z$").get();
    assertTrue(rule.validate(null));
    assertTrue(rule.validate("abcz"));
    assertFalse(rule.validate("xyz"));
    assertEquals("field正则表达式校验失败", rule.getFailMessage("field"));
  }
}
