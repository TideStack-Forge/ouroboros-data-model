package com.ouroboros.data.model.serializer.json;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.ouroboros.data.model.ModelValue;

/**
 * @author liansz
 */
public class ModelValueJsonDeserializer extends JsonDeserializer<ModelValue<?, ?>> implements ContextualDeserializer {

  private JavaType valueType;

  @Override
  public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
    JavaType wrapperType = property.getType();
    ModelValueJsonDeserializer deserializer = new ModelValueJsonDeserializer();
    deserializer.valueType = wrapperType;
    return deserializer;
  }

  @Override
  public ModelValue<?, ?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    List<JavaType> typeParameters = valueType.getBindings().getTypeParameters();

    if (!isObject(p)) {
      return ModelValue.primary(p.readValueAs(typeParameters.get(0).getRawClass()));
    } else {
      return ModelValue.model(p.readValueAs(typeParameters.get(1).getRawClass()));
    }
  }


  private boolean isObject(JsonParser p) {
    return p.getCurrentToken() == JsonToken.START_OBJECT;
  }
}
