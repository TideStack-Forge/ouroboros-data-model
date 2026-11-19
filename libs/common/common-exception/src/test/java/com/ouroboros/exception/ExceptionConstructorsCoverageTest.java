package com.ouroboros.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ExceptionConstructorsCoverageTest {

  @Test
  void businessExceptionCoversDefaultConstructors() {
    var cause = new IllegalStateException("bad request");

    var withMessage = new BusinessException("业务错误");
    var withMessageAndCause = new BusinessException("业务错误", cause);
    var withCodeAndCause = new BusinessException(4001, "业务错误", cause);

    assertEquals(Integer.valueOf(-4000), withMessage.getErrorCode());
    assertEquals("业务错误", withMessage.getMessage());

    assertEquals(Integer.valueOf(-4000), withMessageAndCause.getErrorCode());
    assertSame(cause, withMessageAndCause.getCause());

    assertEquals(Integer.valueOf(4001), withCodeAndCause.getErrorCode());
    assertSame(cause, withCodeAndCause.getCause());
  }

  @Test
  void internalExceptionCoversDefaultConstructors() {
    var cause = new IllegalArgumentException("system broken");

    var withMessage = new InternalException("内部错误");
    var withMessageAndCause = new InternalException("内部错误", cause);
    var withCodeAndCause = new InternalException(5001, "内部错误", cause);
    var fromCause = new InternalException(cause);

    assertEquals(Integer.valueOf(-5000), withMessage.getErrorCode());
    assertEquals("内部错误", withMessage.getMessage());

    assertEquals(Integer.valueOf(-5000), withMessageAndCause.getErrorCode());
    assertSame(cause, withMessageAndCause.getCause());

    assertEquals(Integer.valueOf(5001), withCodeAndCause.getErrorCode());
    assertSame(cause, withCodeAndCause.getCause());

    assertEquals(Integer.valueOf(-5000), fromCause.getErrorCode());
    assertEquals("system broken", fromCause.getMessage());
    assertSame(cause, fromCause.getCause());
  }

  @Test
  void platformExceptionCoversDefaultConstructors() {
    var cause = new UnsupportedOperationException("platform broken");

    var withMessage = new PlatformException("平台错误");
    var withMessageAndCause = new PlatformException("平台错误", cause);
    var withCodeAndCause = new PlatformException(6001, "平台错误", cause);

    assertEquals(Integer.valueOf(-6000), withMessage.getErrorCode());
    assertEquals("平台错误", withMessage.getMessage());

    assertEquals(Integer.valueOf(-6000), withMessageAndCause.getErrorCode());
    assertSame(cause, withMessageAndCause.getCause());

    assertEquals(Integer.valueOf(6001), withCodeAndCause.getErrorCode());
    assertSame(cause, withCodeAndCause.getCause());
  }

  @Test
  void validationExceptionCapturesFieldErrors() {
    Map<String, String> errors = new LinkedHashMap<String, String>();
    errors.put("name", "required");
    errors.put("age", "invalid");

    var withMessage = new ValidationException("校验失败");
    var withErrors = new ValidationException(errors);

    assertEquals(Integer.valueOf(422), withMessage.getErrorCode());
    assertEquals("校验失败", withMessage.getMessage());

    assertEquals(Integer.valueOf(422), withErrors.getErrorCode());
    assertTrue(withErrors.getErrors().entrySet().containsAll(errors.entrySet()));
    assertEquals("", withErrors.getMessage());
  }

  @Test
  void ouroborosExceptionDefensiveCopiesMessageArgs() {
    var args = new Object[]{"A100", "OPEN"};
    var ex = new PlatformException(new com.ouroboros.errorcode.SimpleErrorCodeReference(6002, "PLATFORM-002", "{0}:{1}"), args);

    args[0] = "CHANGED";
    var returned = ex.getMessageArgs();
    returned[1] = "MODIFIED";

    assertEquals("A100:OPEN", ex.getMessage());
    assertEquals("A100", ex.getMessageArgs()[0]);
    assertEquals("OPEN", ex.getMessageArgs()[1]);
  }
}
