package com.ouroboros.errorcode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleErrorCodeReferenceTest {

  @Test
  void simpleReference_exposesCodeBizCodeAndFallbackTemplate() {
    var ref = new SimpleErrorCodeReference(40301, "AUTH-001", "登录已过期");

    assertEquals(Integer.valueOf(40301), ref.getCode());
    assertEquals("AUTH-001", ref.getBizCode());
    assertEquals("登录已过期", ref.getFallbackMessageTemplate());
  }
}
