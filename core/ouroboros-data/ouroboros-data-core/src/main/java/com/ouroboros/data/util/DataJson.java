package com.ouroboros.data.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.vavr.control.Try;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class DataJson {
  static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final DefaultPrettyPrinter PRETTY_PRINTER = new DefaultPrettyPrinter();

  static {
    OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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
}
