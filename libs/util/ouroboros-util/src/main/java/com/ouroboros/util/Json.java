package com.ouroboros.util;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.vavr.control.Try;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ouroboros.util.config.ObjectMapperConfigurator;

@SuppressWarnings("unused")
public class Json {
  static final ObjectMapper objectMapper = new ObjectMapper();
  static final ObjectMapper strictObjectMapper;
  static final DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();

  static {
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    SPI.getSortedServiceStream(ObjectMapperConfigurator.class)
        .forEach(configure -> configure.accept(objectMapper));
    strictObjectMapper = objectMapper.copy();
    strictObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    DefaultPrettyPrinter.Indenter indenter = new DefaultIndenter("  ", DefaultIndenter.SYS_LF);
    prettyPrinter.indentObjectsWith(indenter);
    prettyPrinter.indentArraysWith(indenter);
  }

  /** An independent mapper for typed binding, preserving platform JSON configuration. */
  public static ObjectMapper mapperCopy() {
    return objectMapper.copy();
  }

  /**
   * 将对象转换为 Json 字符串
   *
   * @return Json 字符串
   */
  public static String toJsonString(Object any) {
    return Try.of(() -> objectMapper.writeValueAsString(any)).getOrElse("");
  }

  /**
   * 将对象转换为 Json 字符串
   *
   * @return Json 字符串
   */
  public static Try<String> tryToJsonString(Object any) {
    return Try.of(() -> objectMapper.writeValueAsString(any));
  }

  /**
   * 将对象转换为格式化后的 Json 字符串
   *
   * @return 对象
   */
  public static String toPrettyJsonString(Object any) {
    return Try.of(() -> objectMapper.writer(prettyPrinter).writeValueAsString(any))
        .getOrElse("");
  }

  /**
   * 将对象转换为格式化后的 Json 字符串
   *
   * @return 对象
   */
  public static Try<String> tryToPrettyJsonString(Object any) {
    return Try.of(() -> objectMapper.writer(prettyPrinter).writeValueAsString(any));
  }

  /**
   * 将 Json 字符串转换为Map
   */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> toMap(CharSequence jsonString) {
    return Try.of(() -> objectMapper.readValue(jsonString.toString(), Map.class))
        .getOrElse(Collections::emptyMap);
  }

  /**
   * 将 Json 字符串转换为Map
   */
  public static Try<Map<String, Object>> tryToMap(CharSequence jsonString) {
    return Try.of(() -> objectMapper.readValue(jsonString.toString(), Map.class));
  }

  /**
   * 将 Json 字符串转换为List
   */
  @SuppressWarnings("unchecked")
  public static List<Object> toList(CharSequence jsonString) {
    return Try.of(() -> objectMapper.readValue(jsonString.toString(), List.class))
        .getOrElse(Collections::emptyList);
  }

  /**
   * 将 Json 字符串转换为List
   */
  @SuppressWarnings("unchecked")
  public static <T> List<T> toList(TypeReference<List<T>> typeReference, CharSequence jsonString) {
    return Try.of(() -> objectMapper.readValue(jsonString.toString(), typeReference))
        .getOrElse(Collections::emptyList);
  }

  /**
   * 将 Json 字符串转换为List
   */
  public static Try<List<Object>> tryToList(CharSequence jsonString) {
    return Try.of(() -> (List<Object>) objectMapper.readValue(jsonString.toString(), List.class));
  }

  public static <T> Try<List<T>> tryToList(TypeReference<List<T>> typeReference, CharSequence jsonString) {
    return Try.of(() -> objectMapper.readValue(jsonString.toString(), typeReference));
  }

  /**
   * 将 Json 字符串转换为JsonBag
   */
  public static JsonBag toJsonBag(CharSequence jsonString) {
    return Try.of(() -> objectMapper.readValue(jsonString.toString(), JsonBag.class))
        .getOrElse(JsonBag::empty);
  }

  /**
   * 将 Json 字符串转换为JsonBag
   */
  public static Try<JsonBag> tryToJsonBag(CharSequence jsonString) {
    return Try.of(() -> objectMapper.readValue(jsonString.toString(), JsonBag.class));
  }

  /**
   * 将 Json 字符串转换为指定类型的对象
   */
  public static <T> T toBean(Class<T> clazz, CharSequence jsonString) {
    return Try.of(() -> objectMapper.readValue(jsonString.toString(), clazz))
        .getOrElse((T) null);
  }

  /**
   * 将 Json 字符串转换为指定类型的对象
   */
  public static <T> Try<T> tryToBean(Class<T> clazz, String jsonString) {
    return Try.of(() -> objectMapper.readValue(jsonString, clazz));
  }

  /**
   * 将 Json 字符串转换为指定类型的对象
   */
  public static <T> Try<T> tryToBean(Class<T> clazz, CharSequence jsonString) {
    return Try.of(() -> objectMapper.readValue(jsonString.toString(), clazz));
  }

  /**
   * 将 Json 字符串转换为指定类型的对象，并拒绝未知字段。
   */
  public static <T> Try<T> tryToStrictBean(Class<T> clazz, CharSequence jsonString) {
    return Try.of(() -> strictObjectMapper.readValue(jsonString.toString(), clazz));
  }

  /**
   * 将 Json 字符串转换为指定类型的对象
   *
   * @param typeReference
   * @param jsonString
   */
  public static <T> T toBean(TypeReference<T> typeReference, CharSequence jsonString) {
    return Try.of(() -> objectMapper.readValue(jsonString.toString(), typeReference))
        .getOrElse((T) null);
  }

  /**
   * 将 Json 字符串转换为指定类型的对象
   *
   * @param typeReference
   * @param jsonString
   */
  public static <T> Try<T> tryToBean(TypeReference<T> typeReference, CharSequence jsonString) {
    return Try.of(() -> objectMapper.readValue(jsonString.toString(), typeReference));
  }

  /**
   * 将 Json 字符串转换为指定类型的对象，并拒绝未知字段。
   *
   * @param typeReference
   * @param jsonString
   */
  public static <T> Try<T> tryToStrictBean(TypeReference<T> typeReference, CharSequence jsonString) {
    return Try.of(() -> strictObjectMapper.readValue(jsonString.toString(), typeReference));
  }

  /**
   * 将 inputStream 转换为指定类型的对象
   */
  public static <T> T toBean(Class<T> clazz, InputStream inputStream) {
    return Try.of(() -> objectMapper.readValue(inputStream, clazz))
        .getOrElse((T) null);
  }

  /**
   * 将 inputStream 转换为指定类型的对象
   */
  public static <T> Try<T> tryToBean(Class<T> clazz, InputStream inputStream) {
    return Try.of(() -> objectMapper.readValue(inputStream, clazz));
  }

  /**
   * 将 Json 字符串转换为指定类型的对象
   */
  public static <T> T toBean(Type type, CharSequence jsonString) {
    JavaType javaType = objectMapper.getTypeFactory().constructType(type);
    return (T) Try.of(() -> objectMapper.readValue(jsonString.toString(), javaType))
        .getOrElse((T) null);
  }

  /**
   * 将 Json 字符串转换为指定类型的对象
   */
  public static <T> Try<T> tryToBean(Type type, CharSequence jsonString) {
    JavaType javaType = objectMapper.getTypeFactory().constructType(type);
    return Try.of(() -> objectMapper.readValue(jsonString.toString(), javaType));
  }
}
