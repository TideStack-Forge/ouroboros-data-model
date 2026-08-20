package com.ouroboros.data.model.serializer.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.ouroboros.data.model.ModelValue;

/**
 * @author liansz
 */
public class ModelValueJsonSerializer extends JsonSerializer<ModelValue> {

  @Override
  public void serialize(ModelValue value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    gen.writeObject(value.get());
  }
}
