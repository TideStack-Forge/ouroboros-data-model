package com.ouroboros.util.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.ouroboros.util.JsonBag;

/**
 * @author liansz
 */
public class JsonBagSerializer extends JsonSerializer<JsonBag> {

  @Override
  public void serialize(JsonBag jsonBag, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
    jsonGenerator.writeObject(
        jsonBag.isNull() ? null
            : jsonBag.isMap() ? jsonBag.getMap()
            : jsonBag.isList() ? jsonBag.getList()
            : jsonBag.get()
    );
  }
}
