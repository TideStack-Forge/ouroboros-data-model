package com.ouroboros.data.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

import io.vavr.control.Try;

public final class DataConverters {
  private DataConverters() {
  }

  public static Short toShort(Object value) {
    if (value instanceof Short number) {
      return number;
    }
    if (value instanceof Number number) {
      return number.shortValue();
    }
    if (value instanceof CharSequence text) {
      var normalized = normalizeNumber(text);
      return Try.of(() -> Short.parseShort(normalized))
          .orElse(Try.of(() -> toShort(Long.parseLong(normalized))))
          .orElse(Try.of(() -> toShort(Double.parseDouble(normalized))))
          .getOrNull();
    }
    return null;
  }

  public static Integer toInteger(Object value) {
    if (value instanceof Integer number) {
      return number;
    }
    if (value instanceof Number number) {
      return number.intValue();
    }
    if (value instanceof CharSequence text) {
      var normalized = normalizeNumber(text);
      return Try.of(() -> Integer.parseInt(normalized))
          .orElse(Try.of(() -> toInteger(Long.parseLong(normalized))))
          .orElse(Try.of(() -> toInteger(Double.parseDouble(normalized))))
          .getOrNull();
    }
    return null;
  }

  public static Long toLong(Object value) {
    if (value instanceof Long number) {
      return number;
    }
    if (value instanceof Number number) {
      return number.longValue();
    }
    if (value instanceof CharSequence text) {
      var normalized = normalizeNumber(text);
      return Try.of(() -> Long.parseLong(normalized))
          .orElse(Try.of(() -> toLong(Double.parseDouble(normalized))))
          .getOrNull();
    }
    return null;
  }

  public static Float toFloat(Object value) {
    if (value instanceof Float number) {
      return number;
    }
    if (value instanceof Number number) {
      return number.floatValue();
    }
    if (value instanceof CharSequence text) {
      var normalized = normalizeNumber(text);
      return Try.of(() -> Float.parseFloat(normalized))
          .orElse(Try.of(() -> toFloat(Double.parseDouble(normalized))))
          .getOrNull();
    }
    return null;
  }

  public static Double toDouble(Object value) {
    if (value instanceof Double number) {
      return number;
    }
    if (value instanceof Number number) {
      return number.doubleValue();
    }
    if (value instanceof CharSequence text) {
      return Try.of(() -> Double.parseDouble(normalizeNumber(text))).getOrNull();
    }
    return null;
  }

  public static BigInteger toBigInteger(Object value) {
    if (value instanceof BigInteger number) {
      return number;
    }
    if (value instanceof BigDecimal number) {
      return number.toBigInteger();
    }
    if (value instanceof Number number) {
      return BigInteger.valueOf(number.longValue());
    }
    if (value instanceof CharSequence text) {
      var normalized = normalizeNumber(text);
      return Try.of(() -> new BigInteger(normalized))
          .orElse(Try.of(() -> toBigInteger(new BigDecimal(normalized))))
          .getOrNull();
    }
    return null;
  }

  public static BigDecimal toBigDecimal(Object value) {
    if (value instanceof BigDecimal number) {
      return number;
    }
    if (value instanceof BigInteger number) {
      return new BigDecimal(number);
    }
    if (value instanceof Number number) {
      return BigDecimal.valueOf(number.doubleValue());
    }
    if (value instanceof CharSequence text) {
      var normalized = normalizeNumber(text);
      return Try.of(() -> new BigDecimal(normalized))
          .orElse(Try.of(() -> toBigDecimal(new BigInteger(normalized))))
          .getOrNull();
    }
    return null;
  }

  public static String toString(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof String text) {
      return text;
    }
    if (value instanceof CharSequence text) {
      return text.toString();
    }
    if (value instanceof LocalDateTime dateTime) {
      return DataDates.toString(dateTime);
    }
    if (value instanceof LocalDate date) {
      return DataDates.toString(date);
    }
    if (value instanceof Date date) {
      return DataDates.toString(date);
    }
    return value.toString();
  }

  public static Boolean toBoolean(Object value) {
    if (value == null) {
      return false;
    }
    if (value instanceof Boolean bool) {
      return bool;
    }
    if (value instanceof Number number) {
      return number.doubleValue() != 0;
    }
    if (value instanceof CharSequence text) {
      var normalized = text.toString();
      return !normalized.isEmpty()
          && Try.of(() -> Boolean.parseBoolean(normalized))
          .orElse(() -> Try.of(() -> toBoolean(Integer.parseInt(normalized))))
          .getOrElse(true);
    }
    return true;
  }

  private static String normalizeNumber(CharSequence value) {
    return value.toString().replace(",", "");
  }
}
