package com.ouroboros.util;

import static io.vavr.API.Try;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日期相关工具方法
 */
@SuppressWarnings("unused")
public class Dates {
  private static final Logger log = LoggerFactory.getLogger(Dates.class);
  /**
   * Date 转换为 LocalDateTime
   *
   * @param date Date对象
   */
  public static LocalDateTime toLocalDateTime(Date date) {
    if (date instanceof java.sql.Date sqlDate) {
      return sqlDate.toLocalDate().atStartOfDay();
    }
    if (date instanceof java.sql.Time sqlTime) {
      return sqlTime.toLocalTime().atDate(LocalDate.MIN);
    }
    return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
  }

  public static LocalDateTime toLocalDateTime(Instant instant) {
    return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
  }

  /**
   * LocalDate 转换为 LocalDateTime
   *
   * @param date   LocalDate 对象
   * @param hour   时
   * @param minute 分
   * @param second 秒
   */
  public static LocalDateTime toLocalDateTime(LocalDate date, int hour, int minute, int second) {
    return date.atTime(hour, minute, second);
  }

  /**
   * LocalDate 转换为 LocalDateTime，时间取 00:00:00
   *
   * @param date LocalDate 对象
   */
  public static LocalDateTime toLocalDateTime(LocalDate date) {
    return toLocalDateTime(date, 0, 0, 0);
  }

  /**
   * 将时间戳转换为 LocalDateTime
   *
   * @param timestamp 兼容四种格式，13位（长整型、双精度）、10位长整型、双精度10位整数代表秒数+小数代表毫秒数
   * @return LocalDateTime
   */
  public static LocalDateTime toLocalDateTime(Number timestamp) {
    var longValue = timestamp.longValue();
    // 如果是秒数，则转换为毫秒数
    if (longValue < 10000000000L) {
      longValue *= 1000;
      if (timestamp instanceof Double doubleValue) {
        var decimalMillis = (long) (doubleValue % 1 * 1000);
        longValue += decimalMillis;
      }
    }
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(longValue), ZoneId.systemDefault());
  }

  /**
   * 将日期字符串转换为 LocalDateTime
   *
   * @param dateString 兼容三种种规范：
   *                   <ul>
   *                   <li>时间戳，兼容四种格式，13位（长整型、双精度）、10位长整型、双精度10位整数代表秒数+小数代表毫秒数</li>
   *                   <li>ISO-8601，例：'2025-12-03T10:15:30', '2025-12-03T10:15:30+01:00'</li>
   *                   <li>yyyy-MM-dd HH:mm:ss</li>
   *                   </ul>
   * @return 若解析失败则返回 LocalDateTime.MIN
   */
  public static LocalDateTime toLocalDateTime(CharSequence dateString) {
    // 如果日期字符串是时间戳，则直接转换为 LocalDateTime
    if (dateString.toString().matches("^\\d{10,13}(\\.\\d+)?$")) {
      var timestamp = Double.parseDouble(dateString.toString());
      return toLocalDateTime(timestamp);
    }
    return Try(() -> LocalDateTime.parse(dateString))
        .orElse(() -> Try(() -> dateString.length() <= 10 ? toLocalDate(dateString.toString()).atStartOfDay() : LocalDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME)))
        .orElse(() -> Try(() -> LocalDateTime.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))))
        .getOrElse(() -> LocalDateTime.MIN);
  }

  /**
   * 将指定格式的日期字符串转换为 LocalDateTime
   *
   * @param dateString 日期字符串
   * @param formats    日期字符串格式，可以传递多个格式，依次尝试转换
   */
  public static LocalDateTime toLocalDateTime(CharSequence dateString, CharSequence... formats) {
    for (CharSequence format : formats) {
      try {
        return LocalDateTime.parse(dateString, DateTimeFormatter.ofPattern(format.toString()));
      } catch (Throwable e) {
        log.warn("Failed to parse date string '{}' with format '{}': {}", dateString, format, e.getMessage());
      }
    }
    return toLocalDateTime(dateString);
  }


  /**
   * Date 类型转换为 LocalDate
   *
   * @param date Date 对象
   */
  public static LocalDate toLocalDate(Date date) {
    if (date instanceof java.sql.Date sqlDate) {
      return sqlDate.toLocalDate();
    }
    if (date instanceof java.sql.Time sqlTime) {
      return LocalDate.MIN;
    }
    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
  }

  /**
   * LocalDateTime 类型转换为 LocalDate
   *
   * @param date LocalDateTime 对象
   */
  public static LocalDate toLocalDate(LocalDateTime date) {
    return date.toLocalDate();
  }

  /**
   * 时间戳转换为 LocalDate
   *
   * @param timestamp 兼容四种格式，13位（长整型、双精度）、10位长整型、双精度10位整数代表秒数+小数代表毫秒数
   * @return LocalDate
   */
  public static LocalDate toLocalDate(Number timestamp) {
    return toLocalDateTime(timestamp).toLocalDate();
  }

  /**
   * 字符串转换为 LocalDate
   *
   * @param dateString 时间戳字符串或ISO-8601规范字符串，例子：2025-12-03T10:15:30
   */
  public static LocalDate toLocalDate(CharSequence dateString) {
    if (dateString.toString().matches("^\\d{10,13}(\\.\\d{3})?$")) {
      var timestamp = Double.parseDouble(dateString.toString());
      return toLocalDate(timestamp);
    }
    return dateString.length() > 10 ? toLocalDateTime(dateString).toLocalDate() : LocalDate.parse(dateString);
  }

  /**
   * 将指定格式的日期字符串转换为 LocalDate
   *
   * @param dateString 日期字符串
   * @param formats    日期字符串格式，可以传递多个格式，依次尝试转换
   */
  public static LocalDate toLocalDate(CharSequence dateString, CharSequence... formats) {
    for (CharSequence format : formats) {
      try {
        return LocalDate.parse(dateString, DateTimeFormatter.ofPattern(format.toString()));
      } catch (Throwable e) {
        log.warn("Failed to parse date string '{}' with format '{}': {}", dateString, format, e.getMessage());
      }
    }
    return toLocalDate(dateString);
  }

  public static LocalTime toLocalTime(Date date) {
    if (date instanceof java.sql.Date sqlDate) {
      return LocalTime.MIN;
    }
    if (date instanceof java.sql.Time sqlTime) {
      return sqlTime.toLocalTime();
    }
    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
  }

  public static LocalTime toLocalTime(LocalDateTime date) {
    return date.toLocalTime();
  }

  public static LocalTime toLocalTime(Number timestamp) {
    return toLocalDateTime(timestamp).toLocalTime();
  }

  public static LocalTime toLocalTime(CharSequence dateString) {
    if (dateString.toString().matches("^\\d{10,13}(\\.\\d{3})?$")) {
      var timestamp = Double.parseDouble(dateString.toString());
      return toLocalTime(timestamp);
    }
    return dateString.length() > 10 ? toLocalDateTime(dateString).toLocalTime() : LocalTime.parse(dateString);
  }

  public static LocalTime toLocalTime(CharSequence dateString, CharSequence... formats) {
    for (CharSequence format : formats) {
      try {
        return LocalTime.parse(dateString, DateTimeFormatter.ofPattern(format.toString()));
      } catch (Throwable e) {
        log.warn("Failed to parse time string '{}' with format '{}': {}", dateString, format, e.getMessage());
      }
    }
    return toLocalTime(dateString);
  }

  /**
   * LocalDateTime 转换为 Date 对象
   *
   * @param date LocalDateTime 对象
   */
  public static Date toDate(LocalDateTime date) {
    return Date.from(date.atZone(ZoneId.systemDefault()).toInstant());
  }

  /**
   * LocalDate 转换为 Date 对象
   *
   * @param date LocalDate 对象
   */
  public static Date toDate(LocalDate date) {
    return toDate(date.atStartOfDay());
  }

  /**
   * 时间戳转换为 Date
   *
   * @param timestamp 兼容四种格式，13位（长整型、双精度）、10位长整型、双精度10位整数代表秒数+小数代表毫秒数
   * @return Date
   */
  public static Date toDate(Number timestamp) {
    return toDate(toLocalDateTime(timestamp));
  }

  /**
   * 日期字符串转换为 Date 对象
   *
   * @param dateString 时间戳或IOS-8601或简单格式的日期字符串
   */
  public static Date toDate(CharSequence dateString) {
    return toDate(toLocalDateTime(dateString));
  }

  /**
   * 将指定格式的日期字符串转换为 Date
   *
   * @param dateString 日期字符串
   * @param formats    格式
   */
  public static Date toDate(CharSequence dateString, CharSequence... formats) {
    return toDate(toLocalDateTime(dateString, formats));
  }

  /**
   * 返回 ISO-8601 格式日期时间字符串
   *
   * @param date LocalDateTime 对象
   * @return yyyy-MM-ddTHH:mm:ss 格式
   */
  public static String toString(LocalDateTime date) {
    return date.toString();
  }

  /**
   * 返回 ISO-8601 格式日期字符串
   *
   * @param date LocalDate 对象
   * @return yyyy-MM-dd
   */
  public static String toString(LocalDate date) {
    return date.toString();
  }

  /**
   * 返回 ISO-8601 格式日期字符串
   *
   * @param date Date 对象
   * @return yyyy-MM-ddTHH:mm:ss 格式
   */
  public static String toString(Date date) {
    return toString(toLocalDateTime(date));
  }

  /**
   * 按照指定格式将日期时间转换为字符串
   *
   * @param format 格式
   */
  public static String toString(LocalDateTime date, String format) {
    return date.format(DateTimeFormatter.ofPattern(format));
  }

  /**
   * 按照指定格式将日期时间转换为字符串
   *
   * @param format 格式
   */
  public static String toString(LocalDate date, String format) {
    return date.format(DateTimeFormatter.ofPattern(format));
  }

  /**
   * 按照指定格式将日期时间转换为字符串
   *
   * @param format 格式
   */
  public static String toString(Date date, String format) {
    return toLocalDateTime(date).format(DateTimeFormatter.ofPattern(format));
  }

  /**
   * 返回增加时间后的新日期时间
   *
   * @param date    日期时间
   * @param hours   时
   * @param minutes 分
   * @param seconds 秒
   */
  public static LocalDateTime addTime(LocalDateTime date, int hours, int minutes, int seconds) {
    return date.plusHours(hours).plusMinutes(minutes).plusSeconds(seconds);
  }

  /**
   * 返回增加时间后的新日期时间
   *
   * @param date    日期时间
   * @param hours   时
   * @param minutes 分
   * @param seconds 秒
   */
  public static Date addTime(Date date, int hours, int minutes, int seconds) {
    return new Date(date.getTime() + ((long) hours * 60 * 60 * 1000) + ((long) minutes * 60 * 1000) + (seconds * 1000L));
  }

  /**
   * 返回某日期指定时间的对象
   *
   * @param date    时间日期
   * @param hours   时
   * @param minutes 分
   * @param seconds 秒
   */
  public static LocalDateTime setTime(LocalDateTime date, int hours, int minutes, int seconds) {
    return date.withHour(hours).withMinute(minutes).withSecond(seconds);
  }

  /**
   * 返回增加时间后的新日期时间
   *
   * @param date    日期时间
   * @param hours   时
   * @param minutes 分
   * @param seconds 秒
   */
  public static Date setTime(Date date, int hours, int minutes, int seconds) {
    return toDate(toLocalDate(date).atTime(hours, minutes, seconds));
  }

  public static Date addDays(Date date, int days) {
    return new Date(date.getTime() + ((long) days * 24 * 60 * 60 * 1000));
  }

  /**
   * 返回当前时间戳（13位，即毫秒级）
   */
  public static Long getTimestamp() {
    return System.currentTimeMillis();
  }

  /**
   * 获取时间戳（13位，即毫秒级）
   *
   * @param date 日期时间
   */
  public static Long getTimestamp(Date date) {
    return date.getTime();
  }

  /**
   * 获取时间戳（13位，即毫秒级）
   *
   * @param date 日期时间
   */
  public static Long getTimestamp(LocalDateTime date) {
    return date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
  }

  /**
   * 获取时间戳（13位，即毫秒级）
   *
   * @param date 日期时间
   */
  public static Long getTimestamp(LocalDate date) {
    return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
  }

  /**
   * 获取当前UNIX时间戳（10位，即秒级）
   */
  public static Long getUnixTimestamp() {
    return System.currentTimeMillis() / 1000;
  }

  /**
   * 获取UNIX时间戳（10位，即秒级）
   *
   * @param date 日期时间
   */
  public static Long getUnixTimestamp(Date date) {
    return date.getTime() / 1000;
  }

  /**
   * 获取UNIX时间戳（10位，即秒级）
   *
   * @param date 日期时间
   */
  public static Long getUnixTimestamp(LocalDateTime date) {
    return date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000;
  }

  /**
   * 获取UNIX时间戳（10位，即秒级）
   *
   * @param date 日期时间
   */
  public static Long getUnixTimestamp(LocalDate date) {
    return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000;
  }

  /**
   * 获取指定日期当年第一天的日期
   *
   * @param date 日期时间
   */
  public static LocalDate getYearStartDay(LocalDate date) {
    return LocalDate.of(date.getYear(), 1, 1);
  }

  /**
   * 获取指定日期当年第一天的日期
   *
   * @param date 日期时间
   */
  public static LocalDate getYearStartDay(LocalDateTime date) {
    return LocalDate.of(date.getYear(), 1, 1);
  }

  /**
   * 获取指定日期当年的最后一天的日期
   *
   * @param date 日期时间
   */
  public static LocalDate getYearEndDay(LocalDate date) {
    return LocalDate.of(date.getYear(), 12, 31);
  }

  /**
   * 获取指定日期当年的最后一天的日期
   *
   * @param date 日期时间
   */
  public static LocalDate getYearEndDay(LocalDateTime date) {
    return LocalDate.of(date.getYear(), 12, 31);
  }

  /**
   * 获取指定日期当年第一天0时的日期时间
   *
   * @param date 日期时间
   */
  public static LocalDateTime getYearStart(LocalDateTime date) {
    return LocalDateTime.of(date.getYear(), 1, 1, 0, 0, 0);
  }

  /**
   * 获取指定日期当年第一天0时的日期时间
   *
   * @param date 日期时间
   */
  public static LocalDateTime getYearStart(LocalDate date) {
    return LocalDateTime.of(date.getYear(), 1, 1, 0, 0, 0);
  }

  /**
   * 获取指定日期当年最后一天最后一刻的日期时间
   *
   * @param date 日期时间
   */
  public static LocalDateTime getYearEnd(LocalDateTime date) {
    return LocalDateTime.of(date.getYear(), 12, 31, 23, 59, 59, 999000000);
  }

  /**
   * 获取指定日期当年最后一天最后一刻的日期时间
   *
   * @param date 日期时间
   */
  public static LocalDateTime getYearEnd(LocalDate date) {
    return LocalDateTime.of(date.getYear(), 12, 31, 23, 59, 59, 999000000);
  }

  /**
   * 获取指定日期当月第一天的日期
   *
   * @param date 日期时间
   */
  public static LocalDate getMonthStartDay(LocalDate date) {
    return LocalDate.of(date.getYear(), date.getMonthValue(), 1);
  }

  /**
   * 获取指定日期当月最后一天的日期
   *
   * @param date 日期时间
   */
  public static LocalDate getMonthEndDay(LocalDate date) {
    return LocalDate.of(date.getYear(), date.getMonthValue(), date.lengthOfMonth());
  }

  /**
   * 获取指定日期当月第一天0时的日期时间
   *
   * @param date 日期时间
   */
  public static LocalDateTime getMonthStart(LocalDateTime date) {
    return LocalDateTime.of(date.getYear(), date.getMonth(), 1, 0, 0, 0);
  }

  /**
   * 获取指定日期当月第一天0时的日期时间
   *
   * @param date 日期时间
   */
  public static LocalDateTime getMonthStart(LocalDate date) {
    return LocalDateTime.of(date.getYear(), date.getMonth(), 1, 0, 0, 0);
  }

  /**
   * 获取指定日期当月最后一天最后一刻的日期时间
   *
   * @param date 日期时间
   */
  public static LocalDateTime getMonthEnd(LocalDateTime date) {
    return LocalDateTime.of(date.getYear(), date.getMonth(), date.toLocalDate().lengthOfMonth(), 23, 59, 59, 999000000);
  }

  /**
   * 获取指定日期当月最后一天最后一刻的日期时间
   *
   * @param date 日期时间
   */
  public static LocalDateTime getMonthEnd(LocalDate date) {
    return LocalDateTime.of(date.getYear(), date.getMonth(), date.lengthOfMonth(), 23, 59, 59, 999000000);
  }

  /**
   * 获取指定日期当周周一的日期
   *
   * @param date 日期时间
   */
  public static LocalDate getWeekStartDay(LocalDate date) {
    return date.with(DayOfWeek.MONDAY);
  }

  /**
   * 获取指定日期当周周日的日期
   *
   * @param date 日期时间
   */
  public static LocalDate getWeekEndDay(LocalDate date) {
    return date.with(DayOfWeek.SUNDAY);
  }

  /**
   * 获取指定日期当周周一的日期时间
   *
   * @param date 日期时间
   */
  public static LocalDateTime getWeekStart(LocalDateTime date) {
    return date.with(DayOfWeek.MONDAY).with(LocalTime.MIN);
  }

  /**
   * 获取指定日期当周周一的日期时间
   *
   * @param date 日期时间
   */
  public static LocalDateTime getWeekStart(LocalDate date) {
    return date.with(DayOfWeek.MONDAY).atTime(LocalTime.MIN);
  }

  /**
   * 获取指定日期当周周日的日期时间
   *
   * @param date 日期时间
   */
  public static LocalDateTime getWeekEnd(LocalDateTime date) {
    return date.with(DayOfWeek.SUNDAY).with(LocalTime.MAX);
  }

  /**
   * 获取指定日期当周周日的日期时间
   *
   * @param date 日期时间
   */
  public static LocalDateTime getWeekEnd(LocalDate date) {
    return date.with(DayOfWeek.SUNDAY).atTime(LocalTime.MAX);
  }

  /**
   * 获取指定日期的开始时间
   *
   * @param date 日期时间
   */
  public static LocalDateTime getDayStart(LocalDateTime date) {
    return LocalDateTime.of(date.toLocalDate(), LocalTime.MIN);
  }

  /**
   * 获取指定日期的开始时间
   *
   * @param date 日期时间
   */
  public static LocalDateTime getDayStart(LocalDate date) {
    return LocalDateTime.of(date, LocalTime.MIN);
  }

  /**
   * 获取指定日期的结束时间
   *
   * @param date 日期时间
   */
  public static LocalDateTime getDayEnd(LocalDateTime date) {
    return LocalDateTime.of(date.toLocalDate(), LocalTime.MAX);
  }

  /**
   * 获取指定日期的结束时间
   *
   * @param date 日期时间
   */
  public static LocalDateTime getDayEnd(LocalDate date) {
    return LocalDateTime.of(date, LocalTime.MAX);
  }

  /**
   * 获取今年第一天开始时间
   */
  public static LocalDateTime getThisYearStart() {
    return LocalDate.now().withDayOfYear(1).atStartOfDay();
  }

  /**
   * 获取今年最后一天最后一刻
   */
  public static LocalDateTime getThisYearEnd() {
    var today = today();
    return LocalDate.of(today.getYear(), 12, 31).atTime(LocalTime.MAX);
  }

  /**
   * 获取当前月第一天的开始日期时间
   */
  public static LocalDateTime getThisMonthStart() {
    var today = today();
    return today.withDayOfMonth(1).atTime(LocalTime.MIN);
  }

  /**
   * 获取当前月最后一天最后一刻
   */
  public static LocalDateTime getThisMonthEnd() {
    var today = today();
    return today.withDayOfMonth(today.lengthOfMonth()).atTime(LocalTime.MAX);
  }

  /**
   * 获取今天最开始的日期时间
   */
  public static LocalDateTime getTodayStart() {
    return today().atTime(LocalTime.MIN);
  }

  /**
   * 获取今天最后一刻
   */
  public static LocalDateTime getTodayEnd() {
    return today().atTime(LocalTime.MAX);
  }

  /**
   * 获取去年第一天开始时间
   */
  public static LocalDateTime getLastYearStart() {
    var date = getThisYearStart();
    return date.withYear(date.getYear() - 1);
  }

  /**
   * 获取去年最后一天的最后一刻
   */
  public static LocalDateTime getLastYearEnd() {
    var date = getThisYearEnd();
    return date.withYear(date.getYear() - 1);
  }

  /**
   * 获取指定日期上个月的第一天的开始时间
   *
   * @param date 日期
   */
  public static LocalDateTime getLastMonthStart(LocalDate date) {
    return getMonthStart(date.minusMonths(1));
  }

  /**
   * 获取指定日期上个月的第一天的开始时间
   *
   * @param date 日期
   */
  public static LocalDateTime getLastMonthStart(LocalDateTime date) {
    return getMonthStart(date.minusMonths(1));
  }

  /**
   * 获取指定日期上个月的最后一天的最后一刻
   *
   * @param date 日期
   */
  public static LocalDateTime getLastMonthEnd(LocalDate date) {
    return getMonthEnd(date.minusMonths(1));
  }

  /**
   * 获取指定日期上个月的最后一天的最后一刻
   *
   * @param date 日期
   */
  public static LocalDateTime getLastMonthEnd(LocalDateTime date) {
    return getMonthEnd(date.minusMonths(1));
  }

  /**
   * 获取指定日期当周指定天
   *
   * @param date    日期
   * @param weekday 1-7依次代表周一至周日
   */
  public static LocalDate getThisWeekday(LocalDate date, Integer weekday) {
    return date.with(DayOfWeek.of(weekday));
  }

  /**
   * 获取指定日期当周指定星期几
   *
   * @param date    日期
   * @param weekday 1-7依次代表周一至周日
   */
  public static LocalDate getThisWeekday(LocalDateTime date, Integer weekday) {
    return date.with(DayOfWeek.of(weekday)).toLocalDate();
  }

  /**
   * 获取指定日期的下一个星期几（当周或下周）
   *
   * @param date    日期
   * @param weekday 1-7依次代表周一至周日
   */
  public static LocalDate getNextWeekday(LocalDate date, Integer weekday) {
    var day = date.getDayOfWeek().getValue();
    if (day < weekday) {
      return date.plusDays(weekday - day);
    } else if (day > weekday) {
      return date.plusDays(weekday - day + 7);
    } else {
      return date;
    }
  }

  /**
   * 获取指定日期上个星期几（当周或上周）
   *
   * @param date    日期
   * @param weekday 1-7 依次代表 周一至周日
   */
  public static LocalDate getPrevWeekday(LocalDate date, Integer weekday) {
    var day = date.getDayOfWeek().getValue();
    if (day < weekday) {
      return date.plusDays(weekday - day - 7);
    } else if (day > weekday) {
      return date.plusDays(weekday - day);
    } else {
      return date;
    }
  }

  /**
   * 获取下一个周几（本周或下周）
   *
   * @param weekday 1-7 依次代表 周一至周日
   */
  public static LocalDate getNextWeekday(Integer weekday) {
    return getNextWeekday(LocalDate.now(), weekday);
  }

  /**
   * 获取上一个周几（本周或上周）
   *
   * @param weekday 1-7 依次代表 周一至周日
   */
  public static LocalDate getPrevWeekday(Integer weekday) {
    return getPrevWeekday(LocalDate.now(), weekday);
  }

  /**
   * 获取当前日期
   */
  public static LocalDate today() {
    return LocalDate.now();
  }

  /**
   * 获取当前日期时间
   */
  public static LocalDateTime now() {
    return LocalDateTime.now();
  }

  /**
   * 获取当前日期时间
   * <br><i>推荐使用now方法</i>
   */
  @Deprecated
  public static Date getCurrentDate() {
    return new Date();
  }
}
