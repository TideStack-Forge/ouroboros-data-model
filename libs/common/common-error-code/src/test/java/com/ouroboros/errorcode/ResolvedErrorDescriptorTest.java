package com.ouroboros.errorcode;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResolvedErrorDescriptorTest {

  @Test
  void descriptor_keepsErrorsArgsAndOriginalMessage() {
    var descriptor = ResolvedErrorDescriptor.builder()
        .code(422)
        .bizCode("VALIDATION-001")
        .fallbackMessageTemplate("校验失败")
        .originalMessage("name is required")
        .messageArgs(new Object[]{"name"})
        .errors(Collections.singletonMap("name", "必填"))
        .build();

    assertEquals(Integer.valueOf(422), descriptor.getCode());
    assertEquals("VALIDATION-001", descriptor.getBizCode());
    assertEquals("校验失败", descriptor.getFallbackMessageTemplate());
    assertEquals("name is required", descriptor.getOriginalMessage());
    assertArrayEquals(new Object[]{"name"}, descriptor.getMessageArgs());
    assertEquals(Collections.singletonMap("name", "必填"), descriptor.getErrors());
  }

  @Test
  void throwableMapper_isJustALowLevelSpiContract() {
    ThrowableErrorDescriptorMapper mapper = new ThrowableErrorDescriptorMapper() {
      @Override
      public boolean supports(Throwable throwable) {
        return throwable instanceof IllegalArgumentException;
      }

      @Override
      public ResolvedErrorDescriptor map(Throwable throwable) {
        return ResolvedErrorDescriptor.builder()
            .code(400)
            .originalMessage(throwable.getMessage())
            .build();
      }
    };

    assertTrue(mapper.supports(new IllegalArgumentException("bad argument")));
    assertEquals(Integer.valueOf(400), mapper.map(new IllegalArgumentException("bad argument")).getCode());
  }
}
