package com.ouroboros.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;

public class JsonCoverageTest {

  @Test
  public void coversPrettyPrintingTryHelpersAndBeanConversions() {
    var data = new LinkedHashMap<>();
    data.put("a", 1);
    data.put("list", Arrays.asList(2, 3));

    var pretty = Json.toPrettyJsonString(data);
    assertTrue(pretty.contains("\n"));
    assertEquals("{\"a\":1,\"list\":[2,3]}", Json.toJsonString(data));
    assertTrue(Json.tryToJsonString(data).isSuccess());
    assertTrue(Json.tryToPrettyJsonString(data).isSuccess());
    assertEquals(data, Json.toMap(Json.toJsonString(data)));
    assertTrue(Json.tryToMap("{\"a\":1}").isSuccess());

    var list = Json.toList("[1,2,3]");
    assertEquals(Arrays.asList(1, 2, 3), list);
    assertEquals(Arrays.asList("x", "y"), Json.toList(new TypeReference<List<String>>() {}, "[\"x\",\"y\"]"));
    assertTrue(Json.tryToList("[1,2,3]").isSuccess());
    assertTrue(Json.tryToList(new TypeReference<List<Integer>>() {}, "[1,2,3]").isSuccess());

    var bag = Json.toJsonBag("{\"a\":1}");
    assertEquals(Collections.singletonMap("a", 1), bag.getMap());
    assertTrue(Json.tryToJsonBag("[1,2]").isSuccess());

    assertEquals(data, Json.toBean(Map.class, Json.toJsonString(data)));
    assertTrue(Json.tryToBean(Map.class, Json.toJsonString(data)).isSuccess());
    assertTrue(Json.tryToBean(Map.class, new ByteArrayInputStream(Json.toJsonString(data).getBytes(StandardCharsets.UTF_8))).isSuccess());

    var type = new TypeReference<List<Integer>>() {}.getType();
    assertEquals(Arrays.asList(1, 2, 3), Json.toBean(type, "[1,2,3]"));
    assertTrue(Json.tryToBean(type, "[1,2,3]").isSuccess());

    assertFalse(Json.toMap("not-json").containsKey("a"));
    assertEquals(null, Json.toBean(Map.class, "not-json"));
  }
}
