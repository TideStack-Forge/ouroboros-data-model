package com.ouroboros.exception;

import com.ouroboros.errorcode.SimpleErrorCodeReference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class OuroborosExceptionTest {

  @Test
  void businessException_keepsLegacyCodePath() {
    var ex = new BusinessException(41001, "库存不足");

    assertEquals(Integer.valueOf(41001), ex.getErrorCode());
    assertEquals("库存不足", ex.getMessage());
    assertNull(ex.getErrorCodeReference());
    assertNull(ex.getMessageArgs());
  }

  @Test
  void businessException_supportsReferenceAndArgs() {
    var ref = new SimpleErrorCodeReference(41002, "ORDER-002", "订单 {0} 状态不正确");
    var args = new Object[]{"A1001"};
    var ex = new BusinessException(ref, args);

    args[0] = "CHANGED";

    assertEquals(Integer.valueOf(41002), ex.getErrorCode());
    assertEquals(ref, ex.getErrorCodeReference());
    assertEquals("订单 A1001 状态不正确", ex.getMessage());
    assertArrayEquals(new Object[]{"A1001"}, ex.getMessageArgs());
  }

  @Test
  void platformException_supportsReferenceCauseAndArgs() {
    var ref = new SimpleErrorCodeReference(46001, "SCRIPT-001", "脚本 {0} 执行失败");
    var cause = new IllegalStateException("boom");
    var ex = new PlatformException(ref, cause, "cleanupJob");

    assertEquals(Integer.valueOf(46001), ex.getErrorCode());
    assertEquals(ref, ex.getErrorCodeReference());
    assertEquals("脚本 cleanupJob 执行失败", ex.getMessage());
    assertArrayEquals(new Object[]{"cleanupJob"}, ex.getMessageArgs());
    assertSame(cause, ex.getCause());
  }

  @Test
  void internalException_supportsReferenceCauseAndArgs() {
    var ref = new SimpleErrorCodeReference(55001, "SYSTEM-001", "内部任务 {0} 执行失败");
    var cause = new IllegalArgumentException("db unavailable");
    var ex = new InternalException(ref, cause, "syncIndex");

    assertEquals(Integer.valueOf(55001), ex.getErrorCode());
    assertEquals(ref, ex.getErrorCodeReference());
    assertEquals("内部任务 syncIndex 执行失败", ex.getMessage());
    assertArrayEquals(new Object[]{"syncIndex"}, ex.getMessageArgs());
    assertSame(cause, ex.getCause());
  }
}
