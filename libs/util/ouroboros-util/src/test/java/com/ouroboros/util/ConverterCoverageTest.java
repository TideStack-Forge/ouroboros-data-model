package com.ouroboros.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.Test;

public class ConverterCoverageTest {

  @Test
  public void convertsNumericStringsAndNumbers() {
    assertEquals(Byte.valueOf((byte) 12), Converter.toByte("12"));
    assertEquals(Short.valueOf((short) 1234), Converter.toShort("1,234"));
    assertEquals(Integer.valueOf(42), Converter.toInteger(42L));
    assertEquals(Long.valueOf(42L), Converter.toLong("42"));
    assertEquals(Float.valueOf(12.5f), Converter.toFloat("12.5"));
    assertEquals(Double.valueOf(12.5d), Converter.toDouble("12.5"));
    assertEquals(new BigInteger("12345"), Converter.toBigInteger("12,345"));
    assertEquals(new BigDecimal("12345"), Converter.toBigDecimal("12,345"));
  }

  @Test
  public void convertsBooleansAndNullsPredictably() {
    assertTrue(Converter.toBoolean(1));
    assertFalse(Converter.toBoolean(0));
    assertTrue(Converter.toBoolean("true"));
    assertFalse(Converter.toBoolean("0"));
    assertFalse(Converter.toBoolean(null));
    assertNull(Converter.toInteger(new Object()));
    assertNull(Converter.toBigDecimal(new Object()));
  }
}
