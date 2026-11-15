package com.ouroboros.util.json;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import com.ouroboros.util.JsonBag;

/**
 * JsonBag 的反序列化器，与 {@link JsonBagSerializer} 对称。
 *
 * <p>此前 JsonBag 只声明了 {@code @JsonSerialize}，凡是没有显式注册 JsonBag 模块的
 * ObjectMapper 都无法把 JSON 还原成 JsonBag。典型受害者是数据层的
 * {@code DataJson.OBJECT_MAPPER}：{@code DataMaps.tryToBean} 回读带 JsonBag 字段的
 * 模型（业务日志、RPC 策略、通知）时会抛
 * 「Cannot construct instance of JsonBag (no Creators ...)」。</p>
 *
 * <p>改用类级 {@code @JsonDeserialize} 后，任意 ObjectMapper 都能处理，不再依赖调用方注册。</p>
 *
 * @author liansz
 */
public class JsonBagDeserializer extends JsonDeserializer<JsonBag> {

  @Override
  public JsonBag deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
    var token = p.currentToken();
    if (token == JsonToken.START_OBJECT) {
      return JsonBag.of(p.readValueAs(Map.class));
    }
    if (token == JsonToken.START_ARRAY) {
      return JsonBag.of(p.readValueAs(List.class));
    }
    if (token == JsonToken.VALUE_STRING) {
      return JsonBag.of(p.readValueAs(String.class));
    }
    if (token == JsonToken.VALUE_NUMBER_INT) {
      return JsonBag.of(p.readValueAs(Integer.class));
    }
    if (token == JsonToken.VALUE_NUMBER_FLOAT) {
      return JsonBag.of(p.readValueAs(Double.class));
    }
    if (token == JsonToken.VALUE_TRUE) {
      return JsonBag.of(true);
    }
    if (token == JsonToken.VALUE_FALSE) {
      return JsonBag.of(false);
    }
    return JsonBag.of(null);
  }
}
