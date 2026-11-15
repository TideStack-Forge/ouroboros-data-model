package com.ouroboros.data.util;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.vavr.control.Try;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class DataJson {
  static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final DefaultPrettyPrinter PRETTY_PRINTER = new DefaultPrettyPrinter();

  static {
    OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    OBJECT_MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    OBJECT_MAPPER.registerModule(new JavaTimeModule());
    var module = new SimpleModule();
    module.addDeserializer(LocalDateTime.class, new TimestampCompatibleLocalDateTimeDeserializer());
    var integerDeserializer = new BooleanCompatibleIntegerDeserializer();
    module.addDeserializer(Integer.class, integerDeserializer);
    module.addDeserializer(Integer.TYPE, integerDeserializer);
    OBJECT_MAPPER.registerModule(module);
    var indenter = new DefaultIndenter("  ", DefaultIndenter.SYS_LF);
    PRETTY_PRINTER.indentObjectsWith(indenter);
    PRETTY_PRINTER.indentArraysWith(indenter);
  }

  private DataJson() {
  }

  public static String toJsonString(Object value) {
    return Try.of(() -> OBJECT_MAPPER.writeValueAsString(value)).getOrElse("");
  }

  public static String toPrettyJsonString(Object value) {
    return Try.of(() -> OBJECT_MAPPER.writer(PRETTY_PRINTER).writeValueAsString(value)).getOrElse("");
  }

  public static Map<String, Object> toMap(CharSequence jsonString) {
    return tryToMap(jsonString).getOrElse(Collections::emptyMap);
  }

  public static Try<Map<String, Object>> tryToMap(CharSequence jsonString) {
    return Try.of(() -> OBJECT_MAPPER.readValue(jsonString.toString(), OBJECT_MAPPER.getTypeFactory()
        .constructMapType(Map.class, String.class, Object.class)));
  }

  public static List<Object> toList(CharSequence jsonString) {
    return tryToList(jsonString).getOrElse(Collections::emptyList);
  }

  public static Try<List<Object>> tryToList(CharSequence jsonString) {
    return Try.of(() -> OBJECT_MAPPER.readValue(jsonString.toString(), OBJECT_MAPPER.getTypeFactory()
        .constructCollectionType(List.class, Object.class)));
  }

  public static DataJsonBag toJsonBag(CharSequence jsonString) {
    return Try.of(() -> DataJsonBag.of(OBJECT_MAPPER.readValue(jsonString.toString(), Object.class)))
        .getOrElse(DataJsonBag::empty);
  }

  public static <T> T toBean(Class<T> type, CharSequence jsonString) {
    return tryToBean(type, jsonString).getOrNull();
  }

  public static <T> Try<T> tryToBean(Class<T> type, CharSequence jsonString) {
    return Try.of(() -> OBJECT_MAPPER.readValue(jsonString.toString(), type));
  }

  private static class BooleanCompatibleIntegerDeserializer extends JsonDeserializer<Integer> {

    @Override
    public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
      JsonToken token = parser.currentToken();
      if (token == JsonToken.VALUE_TRUE) {
        return 1;
      }
      if (token == JsonToken.VALUE_FALSE) {
        return 0;
      }
      if (token == JsonToken.VALUE_NULL) {
        return null;
      }
      if (token == JsonToken.VALUE_NUMBER_INT) {
        return parser.getIntValue();
      }
      if (token == JsonToken.VALUE_STRING) {
        String value = parser.getText();
        return value == null || value.isBlank() ? null : Integer.valueOf(value.trim());
      }
      return (Integer) context.handleUnexpectedToken(Integer.class, parser);
    }
  }

  private static class TimestampCompatibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
      JsonToken token = parser.currentToken();
      if (token == JsonToken.VALUE_NULL) {
        return null;
      }
      if (token == JsonToken.VALUE_NUMBER_INT) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(parser.getLongValue()), ZoneId.systemDefault());
      }
      if (token == JsonToken.VALUE_STRING) {
        String value = parser.getText();
        if (value == null || value.isBlank()) {
          return null;
        }
        String normalizedValue = value.trim().replace(' ', 'T');
        try {
          return LocalDateTime.parse(normalizedValue);
        } catch (DateTimeParseException e) {
          return OffsetDateTime.parse(normalizedValue)
              .atZoneSameInstant(ZoneId.systemDefault())
              .toLocalDateTime();
        }
      }
      if (token == JsonToken.START_ARRAY) {
        List<Integer> parts = new ArrayList<>();
        while (parser.nextToken() != JsonToken.END_ARRAY) {
          parts.add(parser.getIntValue());
        }
        int second = parts.size() > 5 ? parts.get(5) : 0;
        int nanoOfSecond = parts.size() > 6 ? parts.get(6) : 0;
        return LocalDateTime.of(parts.get(0), parts.get(1), parts.get(2), parts.get(3), parts.get(4), second, nanoOfSecond);
      }
      return (LocalDateTime) context.handleUnexpectedToken(LocalDateTime.class, parser);
    }
  }
}
