package com.ouroboros.util.map;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.ouroboros.util.Json;
import com.ouroboros.util.JsonBag;

/**
 * @author liansz
 */
public class TestJsonBag {

  @Test
  public void serializer() {
    {
      var data = Collections.singletonMap("a", "123");
      JsonBag jsonBag = new JsonBag(data);
      assertEquals(Json.toJsonString(data), Json.toJsonString(jsonBag));
    }
    {
      var data = Collections.singletonList("123");
      JsonBag jsonBag = new JsonBag(data);
      assertEquals(Json.toJsonString(data), Json.toJsonString(jsonBag));
    }
    {
      var data = new Model("123");
      JsonBag jsonBag = new JsonBag(data);
      assertEquals(Json.toJsonString(data), Json.toJsonString(jsonBag));
    }
  }

  @Test
  public void deserializer() {
    {
      Map<String, Object> data = Collections.singletonMap("a", "123");
      JsonBag jsonBag = new JsonBag(data);
      assertEquals(data.get("a"), jsonBag.get("a"));
    }
    {
      List<String> data = Collections.singletonList("123");
      JsonBag jsonBag = new JsonBag((List) data);
      assertEquals(data.get(0), jsonBag.get(0));
    }
    {
      Model data = new Model("123");
      JsonBag jsonBag = new JsonBag(data);
      assertEquals(data.getId(), ((Model) jsonBag.get()).getId());
    }
  }
}
