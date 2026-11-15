package com.ouroboros.util.config;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import com.ouroboros.util.Dates;
import com.ouroboros.util.JsonBag;
import com.ouroboros.util.json.JsonBagDeserializer;
import com.ouroboros.util.json.JsonBagSerializer;

public class DefaultObjectMapperConfigurator implements ObjectMapperConfigurator {
  @Override
  public void accept(ObjectMapper objectMapper) {
    configDate(objectMapper);
    configJsonBag(objectMapper);
  }

  void configDate(ObjectMapper objectMapper) {
    var dateSerializer = new StdScalarSerializer<>(Date.class) {
      @Override
      public void serialize(Date value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(Dates.toString(value));
      }
    };
    var dateDeserializer = new JsonDeserializer<Date>() {
      @Override
      public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.hasToken(JsonToken.VALUE_STRING)) {
          return Dates.toDate(p.getText());
        }
        if (p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
          return Dates.toDate(p.getLongValue());
        }
        if (p.hasToken(JsonToken.VALUE_NUMBER_FLOAT)) {
          return Dates.toDate(p.getDoubleValue());
        }
        return null;
      }
    };

    var localDateSerializer = new StdScalarSerializer<>(LocalDate.class) {
      @Override
      public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(Dates.toString(value));
      }
    };

    var localDateDeserializer = new JsonDeserializer<LocalDate>() {
      @Override
      public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.hasToken(JsonToken.VALUE_STRING)) {
          return Dates.toLocalDate(p.getText());
        }
        if (p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
          return Dates.toLocalDate(p.getLongValue());
        }
        if (p.hasToken(JsonToken.VALUE_NUMBER_FLOAT)) {
          return Dates.toLocalDate(p.getDoubleValue());
        }
        return null;
      }
    };

    var localDateTimeSerializer = new StdScalarSerializer<>(LocalDateTime.class) {
      @Override
      public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(Dates.toString(value));
      }
    };

    var localDateTimeDeserializer = new StdScalarDeserializer<LocalDateTime>(LocalDateTime.class) {
      @Override
      public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.hasToken(JsonToken.VALUE_STRING)) {
          return Dates.toLocalDateTime(p.getText());
        }
        if (p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
          return Dates.toLocalDateTime(p.getLongValue());
        }
        if (p.hasToken(JsonToken.VALUE_NUMBER_FLOAT)) {
          return Dates.toLocalDateTime(p.getDoubleValue());
        }
        return null;
      }
    };

    var module = new SimpleModule("DateModule", Version.unknownVersion());
    module.addSerializer(Date.class, dateSerializer);
    module.addDeserializer(Date.class, dateDeserializer);
    module.addSerializer(LocalDate.class, localDateSerializer);
    module.addDeserializer(LocalDate.class, localDateDeserializer);
    module.addSerializer(LocalDateTime.class, localDateTimeSerializer);
    module.addDeserializer(LocalDateTime.class, localDateTimeDeserializer);
    objectMapper.registerModule(module);
  }

  void configJsonBag(ObjectMapper objectMapper) {
    objectMapper.registerModule(new SimpleModule("JsonBag", new Version(1, 0, 0, null, null, null))
        .addDeserializer(JsonBag.class, new JsonBagDeserializer())
        .addSerializer(JsonBag.class, new JsonBagSerializer())
    );
  }
}
