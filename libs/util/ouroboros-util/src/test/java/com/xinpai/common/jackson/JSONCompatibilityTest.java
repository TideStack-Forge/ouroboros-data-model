package com.xinpai.common.jackson;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import groovy.lang.GroovyShell;

class JSONCompatibilityTest {

  @Test
  void groovyScriptsCanImportLegacyJsonCompatibilityClass() {
    var shell = new GroovyShell();
    var result = shell.evaluate(
        "import com.xinpai.common.jackson.JSON\n"
            + "JSON.toJSONString([code: 0, msg: 'ok'])"
    );

    assertEquals("{\"code\":0,\"msg\":\"ok\"}", result);
  }
}
