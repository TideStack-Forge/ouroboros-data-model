package com.ouroboros.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StringsCoverageTest {

  @Test
  public void handlesBlankAndUpperFirst() {
    assertTrue(Strings.isBlank(null));
    assertTrue(Strings.isBlank(" \t\n"));
    assertFalse(Strings.isBlank("x"));
    assertEquals("UserName", Strings.upperFirst("userName"));
    assertEquals("URLValue", Strings.upperFirst("URLValue"));
  }

  @Test
  public void convertsCommonNamingStyles() {
    assertNull(Strings.toSnakeCase(null));
    assertEquals("", Strings.toKebabCase(""));
    assertEquals("user_name", Strings.toSnakeCase("userName"));
    assertEquals("url_value", Strings.toSnakeCase("URLValue"));
    assertEquals("ui_design", Strings.toSnakeCase("UIDesign"));
    assertEquals("user_id", Strings.toSnakeCase("userID"));
    assertEquals("user_id_value", Strings.toSnakeCase("userIDValue"));
    assertEquals("abc123_name", Strings.toSnakeCase("abc123Name"));
    assertEquals("user_name", Strings.toKebabCase("user_name"));
    assertEquals("url-value", Strings.toKebabCase("URLValue"));
    assertEquals("user-id", Strings.toKebabCase("userID"));
    assertEquals("user-id-value", Strings.toKebabCase("userIDValue"));
    assertEquals("abc123-name", Strings.toKebabCase("abc123Name"));
    assertEquals("ui-design", Strings.toKebabCase("UIDesign"));
    assertEquals("additional-datasource", Strings.toKebabCase("additionalDatasource"));
  }
}
