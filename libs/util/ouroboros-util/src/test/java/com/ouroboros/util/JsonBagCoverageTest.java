package com.ouroboros.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

public class JsonBagCoverageTest {

  @Test
  public void coversStatesAccessorsMutatorsAndEquality() {
    var empty = JsonBag.empty();
    assertTrue(empty.isNull());
    assertFalse(empty.isList());
    assertFalse(empty.isMap());
    assertFalse(empty.isValue());

    var listBag = JsonBag.of(new ArrayList<>(Arrays.asList("a", "b")));
    assertTrue(listBag.isList());
    assertEquals("a", listBag.get(0));
    assertEquals("b", listBag.get("1"));
    assertEquals("a", listBag.put(0, "x"));
    assertEquals("x", listBag.get(0));
    assertEquals("x", listBag.remove(0));
    assertEquals("b", listBag.get(0));

    var map = new LinkedHashMap<>();
    map.put("0", "zero");
    map.put("name", "Neo");
    var mapBag = JsonBag.of(map);
    assertTrue(mapBag.isMap());
    assertEquals("zero", mapBag.get(0));
    assertEquals("Neo", mapBag.get("name"));
    assertEquals("Neo", mapBag.put("name", "Trinity"));
    assertEquals("Trinity", mapBag.remove("name"));

    var valueBag = JsonBag.of("value");
    assertTrue(valueBag.isValue());
    assertEquals("value", valueBag.get());
    assertEquals(valueBag, "value");
    assertEquals(valueBag, JsonBag.of("value"));
    assertEquals(listBag, Collections.singletonList("b"));
    assertEquals(mapBag, Collections.singletonMap("0", "zero"));
    assertEquals(JsonBag.of(null), JsonBag.empty());
  }
  @Test
  public void plainAndPlatformMappersRoundTripTheSameJsonValues() throws Exception {
    var plain = new com.fasterxml.jackson.databind.ObjectMapper();
    for (String json : Arrays.asList("{\"nested\":[1,true,null]}", "[\"a\",2]", "\"value\"", "42", "1.25", "true")) {
      JsonBag bag = plain.readValue(json, JsonBag.class);
      assertEquals(plain.readTree(json), plain.readTree(plain.writeValueAsString(bag)));
      assertEquals(Json.tryToJsonBag(json).get(), bag);
    }
    assertEquals(Map.of("template", "notice"), plain.convertValue(Map.of("template", "notice"), JsonBag.class).getMap());
  }
}
