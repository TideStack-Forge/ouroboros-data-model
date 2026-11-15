package com.xinpai.common.jackson;

import com.ouroboros.util.Json;

/**
 * Legacy compatibility bridge for metadata Groovy scripts that still import
 * the historical JSON helper.
 */
public final class JSON {
  private JSON() {
  }

  public static String toJSONString(Object value) {
    return Json.toJsonString(value);
  }
}
