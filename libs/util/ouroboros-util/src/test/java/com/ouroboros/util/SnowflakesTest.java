package com.ouroboros.util;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class SnowflakesTest {

  @Test
  public void reusesGeneratorForSameNode() {
    var first = Snowflakes.get(1, 1);
    var second = Snowflakes.get(1, 1);

    assertSame(first, second);
    assertNotEquals(first.nextId(), second.nextId());
  }

  @Test
  public void rejectsInvalidNodeIds() {
    assertThrows(IllegalArgumentException.class, () -> Snowflakes.get(-1, 0));
    assertThrows(IllegalArgumentException.class, () -> Snowflakes.get(0, 32));
  }
}
