package com.ouroboros.util;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

import org.junit.Test;

public class DatesCoverageTest {

  @Test
  public void convertsBetweenDateTypesWithoutLosingExpectedPrecision() {
    var dateTime = LocalDateTime.of(2024, 2, 15, 10, 20, 30, 123000000);
    var date = dateTime.toLocalDate();
    var time = dateTime.toLocalTime();
    var dateObject = Dates.toDate(dateTime);

    assertEquals(dateTime.withNano(123000000), Dates.toLocalDateTime(dateObject));
    assertEquals(date, Dates.toLocalDate(dateObject));
    assertEquals(time, Dates.toLocalTime(dateObject));

    assertEquals(dateTime.withNano(0), Dates.toLocalDateTime(Dates.getUnixTimestamp(dateTime)));
    assertEquals(dateTime.withNano(500000000), Dates.toLocalDateTime(Double.valueOf(Dates.getUnixTimestamp(dateTime) + 0.5)));
    assertEquals(date, Dates.toLocalDate(Dates.getUnixTimestamp(dateTime)));
    assertEquals(time.withNano(0), Dates.toLocalTime(Dates.getUnixTimestamp(dateTime)));

    assertEquals(dateTime, Dates.toLocalDateTime("2024-02-15T10:20:30.123"));
    assertEquals(date, Dates.toLocalDate("2024-02-15"));
    assertEquals(LocalTime.of(10, 20, 30), Dates.toLocalTime("10:20:30"));
    assertEquals(dateTime.withNano(0), Dates.toLocalDateTime("2024-02-15 10:20:30", "yyyy-MM-dd HH:mm:ss"));
    assertEquals("2024-02-15T10:20:30.123", Dates.toString(dateTime));
    assertEquals("2024-02-15", Dates.toString(date));
    assertEquals("2024-02-15 10:20:30", Dates.toString(dateTime, "yyyy-MM-dd HH:mm:ss"));
    assertEquals("2024-02-15", Dates.toString(date, "yyyy-MM-dd"));
    assertEquals("2024-02-15 10:20:30", Dates.toString(dateObject, "yyyy-MM-dd HH:mm:ss"));
  }

  @Test
  public void adjustsBoundariesAndRelativeDatesDeterministically() {
    var date = LocalDate.of(2024, 2, 15);
    var dateTime = date.atTime(10, 20, 30);

    assertEquals(LocalDate.of(2024, 1, 1), Dates.getYearStartDay(date));
    assertEquals(LocalDate.of(2024, 12, 31), Dates.getYearEndDay(dateTime));
    assertEquals(LocalDateTime.of(2024, 1, 1, 0, 0), Dates.getYearStart(date));
    assertEquals(LocalDateTime.of(2024, 12, 31, 23, 59, 59, 999000000), Dates.getYearEnd(dateTime));

    assertEquals(LocalDate.of(2024, 2, 1), Dates.getMonthStartDay(date));
    assertEquals(LocalDate.of(2024, 2, 29), Dates.getMonthEndDay(date));
    assertEquals(LocalDateTime.of(2024, 2, 1, 0, 0), Dates.getMonthStart(dateTime));
    assertEquals(LocalDateTime.of(2024, 2, 29, 23, 59, 59, 999000000), Dates.getMonthEnd(date));

    assertEquals(LocalDate.of(2024, 2, 12), Dates.getWeekStartDay(date));
    assertEquals(LocalDate.of(2024, 2, 18), Dates.getWeekEndDay(date));
    assertEquals(LocalDateTime.of(2024, 2, 12, 0, 0), Dates.getWeekStart(dateTime));
    assertEquals(LocalDateTime.of(2024, 2, 18, 23, 59, 59, 999999999), Dates.getWeekEnd(date));

    assertEquals(LocalDateTime.of(2024, 2, 15, 0, 0), Dates.getDayStart(dateTime));
    assertEquals(LocalDateTime.of(2024, 2, 15, 23, 59, 59, 999999999), Dates.getDayEnd(date));

    assertEquals(LocalDate.of(2024, 2, 12), Dates.getThisWeekday(date, 1));
    assertEquals(LocalDate.of(2024, 2, 19), Dates.getNextWeekday(date, 1));
    assertEquals(LocalDate.of(2024, 2, 12), Dates.getPrevWeekday(date, 1));
    assertEquals(LocalDateTime.of(2024, 1, 1, 0, 0), Dates.getLastMonthStart(date));
    assertEquals(LocalDateTime.of(2024, 1, 31, 23, 59, 59, 999000000), Dates.getLastMonthEnd(dateTime));

    assertEquals(LocalDateTime.of(2024, 2, 15, 11, 22, 33), Dates.addTime(dateTime, 1, 2, 3));
    assertEquals(LocalDateTime.of(2024, 2, 15, 11, 22, 33), Dates.setTime(dateTime, 11, 22, 33));
    assertEquals(Dates.toDate(LocalDateTime.of(2024, 2, 17, 10, 20, 30)), Dates.addDays(Dates.toDate(dateTime), 2));
  }
}
