package com.ouroboros.util.map;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import com.ouroboros.util.Maps;

/**
 * @author liansz
 */
public class TestMaps {

  @Test
  public void serializer() {
    // 主键序列化
    Pack packPrimary = new Pack(ModelValue.primary("123"));
    Map<String, Object> packPrimaryMap = Maps.fromBean(packPrimary);
    assertEquals(packPrimaryMap.get("a"), "123");

    // 模型序列化
    Pack packModel = new Pack(ModelValue.model(new Model("123")));
    Map<String, Object> packModelMap = Maps.fromBean(packModel);
    assertEquals(((Model) packModelMap.get("a")).getId(), "123");
  }

  @Test
  public void deserializer() {
    // 主键反序列化
    Map<String, Object> packPrimaryMap = Collections.singletonMap("a", "1123");
    Pack packPrimary = Maps.toBean(Pack.class, packPrimaryMap);
    assertEquals(packPrimary.getA().get(), "1123");


    // 模型反序列化
    Map<String, Object> packModelMap = Collections.singletonMap("a", Collections.singletonMap("id", "1123"));
    Pack packModel = Maps.toBean(Pack.class, packModelMap);
    assertEquals(((Model) packModel.getA().get()).getId(), "1123");
  }
}
