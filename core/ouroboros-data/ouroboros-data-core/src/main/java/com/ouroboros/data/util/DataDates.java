package com.ouroboros.data.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import io.vavr.control.Try;

public final class DataDates {
  private DataDates() {
  }

  public static LocalDateTime toLocalDateTime(Date date) {
    if (date instanceof java.sql.Date sqlDate) {
      return sqlDate.toLocalDate().atStartOfDay();
    }
    if (date instanceof java.sql.Time sqlTime) {
      return sqlTime.toLocalTime().atDate(LocalDate.MIN);
    }
    return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
  }

  public static LocalDateTime toLocalDateTime(Number timestamp) {
    var longValue = timestamp.longValue();
    if (longValue < 10000000000L) {
      longValue *= 1000;
      if (timestamp instanceof Double doubleValue) {
        longValue += (long) (doubleValue % 1 * 1000);
      }
    }
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(longValue), ZoneId.systemDefault());
  }

  public static LocalDateTime toLocalDateTime(CharSequence dateString) {
    var value = dateString.toString();
    if (value.matches("^\\d{10,13}(\\.\\d+)?$")) {
      return toLocalDateTime(Double.parseDouble(value));
    }
    return Try.of(() -> LocalDateTime.parse(value))
        .orElse(() -> Try.of(() -> value.length() <= 10 ? toLocalDate(value).atStartOfDay() : LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME)))
        .orElse(() -> Try.of(() -> LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))))
        .getOrElse(LocalDateTime.MIN);
  }

  public static LocalDate toLocalDate(Date date) {
    if (date instanceof java.sql.Date sqlDate) {
      return sqlDate.toLocalDate();
    }
    if (date instanceof java.sql.Time) {
      return LocalDate.MIN;
    }
    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
  }

  public static LocalDate toLocalDate(Number timestamp) {
    return toLocalDateTime(timestamp).toLocalDate();
  }

  public static LocalDate toLocalDate(CharSequence dateString) {
    var value = dateString.toString();
    if (value.matches("^\\d{10,13}(\\.\\d{3})?$")) {
      return toLocalDate(Double.parseDouble(value));
    }
    return value.length() > 10 ? toLocalDateTime(value).toLocalDate() : LocalDate.parse(value);
  }

  public static LocalTime toLocalTime(Date date) {
    if (date instanceof java.sql.Date) {
      return LocalTime.MIN;
    }
    if (date instanceof java.sql.Time sqlTime) {
      return sqlTime.toLocalTime();
    }
    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
  }

  public static LocalTime toLocalTime(Number timestamp) {
    return toLocalDateTime(timestamp).toLocalTime();
  }

  public static LocalTime toLocalTime(CharSequence dateString) {
    var value = dateString.toString();
    if (value.matches("^\\d{10,13}(\\.\\d{3})?$")) {
      return toLocalTime(Double.parseDouble(value));
    }
    return value.length() > 10 ? toLocalDateTime(value).toLocalTime() : LocalTime.parse(value);
  }

  public static String toString(LocalDateTime date) {
    return date.toString();
  }

  public static String toString(LocalDate date) {
    return date.toString();
  }

  public static String toString(Date date) {
    return toString(toLocalDateTime(date));
  }
}
