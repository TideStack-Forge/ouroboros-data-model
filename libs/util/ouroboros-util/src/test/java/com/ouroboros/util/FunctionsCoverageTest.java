package com.ouroboros.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.AbstractMap;
import java.util.Optional;

import org.junit.Test;

import io.vavr.Function3;
import io.vavr.Function4;
import io.vavr.Function5;
import io.vavr.Function6;
import io.vavr.Function7;
import io.vavr.Function8;
import io.vavr.Tuple;

public class FunctionsCoverageTest {

  @Test
  public void coversPipeThroughAndPartialHelpers() {
    assertEquals(Integer.valueOf(9), Functions.pipe((Integer value) -> value + 1, (Integer value) -> value * 2, (Integer value) -> value + 1).apply(3));
    assertEquals("HELLO!", Functions.through(" hello ", String::trim, String::toUpperCase, value -> value + "!").apply());
    assertEquals("seed", Functions.through("seed").apply());
    assertEquals(Integer.valueOf(4), Functions.through(" 2 ", String::trim, Integer::parseInt, value -> value * 2).apply());

    assertEquals(Integer.valueOf(7), Functions.partial((Integer left, Integer right) -> left + right, 3).apply(4));
    assertEquals(Integer.valueOf(9), Functions.partial((Function3<Integer, Integer, Integer, Integer>) (a, b, c) -> a + b + c, 2, 3).apply(4));
    assertEquals(Integer.valueOf(9), Functions.partial((Function3<Integer, Integer, Integer, Integer>) (a, b, c) -> a + b + c, 2).apply(3, 4));
    assertEquals(Integer.valueOf(10), Functions.partial((Function4<Integer, Integer, Integer, Integer, Integer>) (a, b, c, d) -> a + b + c + d, 1, 2, 3).apply(4));
    assertEquals(Integer.valueOf(10), Functions.partial((Function4<Integer, Integer, Integer, Integer, Integer>) (a, b, c, d) -> a + b + c + d, 1, 2).apply(3, 4));
    assertEquals(Integer.valueOf(10), Functions.partial((Function4<Integer, Integer, Integer, Integer, Integer>) (a, b, c, d) -> a + b + c + d, 1).apply(2, 3, 4));

    assertEquals(Integer.valueOf(7), Functions.partialRight((Integer left, Integer right) -> left + right, 4).apply(3));
    assertEquals(Integer.valueOf(9), Functions.partialRight((Function3<Integer, Integer, Integer, Integer>) (a, b, c) -> a + b + c, 3, 4).apply(2));
    assertEquals(Integer.valueOf(9), Functions.partialRight((Function3<Integer, Integer, Integer, Integer>) (a, b, c) -> a + b + c, 4).apply(2, 3));
    assertEquals(Integer.valueOf(10), Functions.partialRight((Function4<Integer, Integer, Integer, Integer, Integer>) (a, b, c, d) -> a + b + c + d, 2, 3, 4).apply(1));
    assertEquals(Integer.valueOf(10), Functions.partialRight((Function4<Integer, Integer, Integer, Integer, Integer>) (a, b, c, d) -> a + b + c + d, 3, 4).apply(1, 2));
    assertEquals(Integer.valueOf(10), Functions.partialRight((Function4<Integer, Integer, Integer, Integer, Integer>) (a, b, c, d) -> a + b + c + d, 4).apply(1, 2, 3));
  }

  @Test
  public void coversPredicateTupleAndConcatHelpers() {
    assertEquals(Integer.valueOf(42), Functions.f(() -> 42).apply());
    assertEquals(Integer.valueOf(5), Functions.f(String::length).apply("value"));
    assertEquals(Integer.valueOf(7), Functions.f((Integer left, Integer right) -> left + right).apply(3, 4));

    assertTrue(Functions.p(String::trim, String::isEmpty).test("  "));
    assertTrue(Functions.p(String::trim, Integer::parseInt, value -> value > 1).test(" 2 "));
    assertFalse(Functions.p((String value) -> value.length() > 5).test("abc"));

    assertEquals(Integer.valueOf(3), Functions.tupled((String value) -> value.length()).apply(Tuple.of("abc")));
    assertEquals(Integer.valueOf(7), Functions.tupled((Integer a, Integer b) -> a + b).apply(Tuple.of(3, 4)));
    assertEquals(Integer.valueOf(9), Functions.tupled((Function3<Integer, Integer, Integer, Integer>) (a, b, c) -> a + b + c).apply(Tuple.of(2, 3, 4)));
    assertEquals(Integer.valueOf(10), Functions.tupled((Function4<Integer, Integer, Integer, Integer, Integer>) (a, b, c, d) -> a + b + c + d).apply(Tuple.of(1, 2, 3, 4)));
    assertEquals(Integer.valueOf(15), Functions.tupled((Function5<Integer, Integer, Integer, Integer, Integer, Integer>) (a, b, c, d, e) -> a + b + c + d + e).apply(Tuple.of(1, 2, 3, 4, 5)));
    assertEquals(Integer.valueOf(21), Functions.tupled((Function6<Integer, Integer, Integer, Integer, Integer, Integer, Integer>) (a, b, c, d, e, f) -> a + b + c + d + e + f).apply(Tuple.of(1, 2, 3, 4, 5, 6)));
    assertEquals(Integer.valueOf(28), Functions.tupled((Function7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>) (a, b, c, d, e, f, g) -> a + b + c + d + e + f + g).apply(Tuple.of(1, 2, 3, 4, 5, 6, 7)));
    assertEquals(Integer.valueOf(36), Functions.tupled((Function8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>) (a, b, c, d, e, f, g, h) -> a + b + c + d + e + f + g + h).apply(Tuple.of(1, 2, 3, 4, 5, 6, 7, 8)));
    assertEquals(Integer.valueOf(7), Functions.<Integer, Integer, Integer>untupled2(tuple -> tuple._1 + tuple._2).apply(3, 4));
    assertEquals(Integer.valueOf(9), Functions.<Integer, Integer, Integer, Integer>untupled3(tuple -> tuple._1 + tuple._2 + tuple._3).apply(2, 3, 4));
    assertEquals(Integer.valueOf(10), Functions.<Integer, Integer, Integer, Integer, Integer>untupled4(tuple -> tuple._1 + tuple._2 + tuple._3 + tuple._4).apply(1, 2, 3, 4));
    assertEquals(Integer.valueOf(15), Functions.<Integer, Integer, Integer, Integer, Integer, Integer>untupled5(tuple -> tuple._1 + tuple._2 + tuple._3 + tuple._4 + tuple._5).apply(1, 2, 3, 4, 5));
    assertEquals(Integer.valueOf(21), Functions.<Integer, Integer, Integer, Integer, Integer, Integer, Integer>untupled6(tuple -> tuple._1 + tuple._2 + tuple._3 + tuple._4 + tuple._5 + tuple._6).apply(1, 2, 3, 4, 5, 6));
    assertEquals(Integer.valueOf(28), Functions.<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>untupled7(tuple -> tuple._1 + tuple._2 + tuple._3 + tuple._4 + tuple._5 + tuple._6 + tuple._7).apply(1, 2, 3, 4, 5, 6, 7));
    assertEquals(Integer.valueOf(36), Functions.<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>untupled8(tuple -> tuple._1 + tuple._2 + tuple._3 + tuple._4 + tuple._5 + tuple._6 + tuple._7 + tuple._8).apply(1, 2, 3, 4, 5, 6, 7, 8));
    assertEquals(Integer.valueOf(3), Functions.toEntryMapper((String key, Integer value) -> key.length() + value).apply(new AbstractMap.SimpleEntry<>("ab", 1)));

    assertEquals(Optional.of("fallback"), Functions.concat(value -> Optional.empty(), value -> Optional.of(value + "back")).apply("fall"));
    assertEquals(Optional.of("hit"), Functions.concat(value -> Optional.of("hit"), value -> Optional.of("miss")).apply("ignored"));
  }

  @Test
  public void coversComposeChainAndExtendedPredicateHelpers() {
    assertEquals("AB", Functions.compose(String::trim, String::toUpperCase).apply(" ab "));
    assertEquals(Integer.valueOf(4), Functions.compose(String::trim, String::length, value -> value + 1).apply("abc"));
    assertEquals("x4", Functions.compose(String::trim, String::length, value -> value + 1, String::valueOf, value -> "x" + value).apply("abc"));
    assertEquals("x5", Functions.compose(String::trim, String::length, value -> value + 1, value -> value + 1, String::valueOf, value -> "x" + value).apply("abc"));
    assertEquals("x6", Functions.compose(String::trim, String::length, value -> value + 1, value -> value + 1, value -> value + 1, String::valueOf, value -> "x" + value).apply("abc"));
    assertEquals("x7", Functions.compose(String::trim, String::length, value -> value + 1, value -> value + 1, value -> value + 1, value -> value + 1, String::valueOf, value -> "x" + value).apply("abc"));

    assertEquals("HELLO!", Functions.chain((String value) -> value.trim()).partialRight((String value, String suffix) -> value + suffix, "!").next(String::toUpperCase).end().apply(" hello "));
    assertEquals(Integer.valueOf(7), Functions.chain((String value) -> value.trim()).next(String::length).partial(Integer::sum, 2).end().apply("hello"));
    assertEquals(Integer.valueOf(10), Functions.chain((String value) -> value.trim()).next(String::length).partial((Function3<Integer, Integer, Integer, Integer>) (a, b, value) -> a + b + value, 2, 3).end().apply("hello"));
    assertEquals(Integer.valueOf(5), Functions.chain((String value) -> value.trim()).next(String::length).end(" hello ").apply());

    assertTrue(Functions.p(String::trim, String::length, value -> value + 1, value -> value > 3).test("abc"));
    assertTrue(Functions.p(String::trim, String::length, value -> value + 1, value -> value + 1, value -> value > 4).test("abc"));
    assertTrue(Functions.p(String::trim, String::length, value -> value + 1, value -> value + 1, value -> value + 1, value -> value > 5).test("abc"));
    assertTrue(Functions.p(String::trim, String::length, value -> value + 1, value -> value + 1, value -> value + 1, value -> value + 1, value -> value > 6).test("abc"));
    assertTrue(Functions.p(String::trim, String::length, value -> value + 1, value -> value + 1, value -> value + 1, value -> value + 1, value -> value + 1, value -> value > 7).test("abc"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void pipeRejectsEmptyArray() {
    Functions.pipe(String::trim, new java.util.function.Function[0]);
  }

  @Test(expected = IllegalArgumentException.class)
  public void throughRejectsEmptyArray() {
    Functions.through("value", new java.util.function.Function[0]);
  }
}
