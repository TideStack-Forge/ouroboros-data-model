package com.ouroboros.util;

import static io.vavr.API.Try;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@SuppressWarnings("unused")
public class Converter {

  public static Byte toByte(Object value) {
    if (value instanceof Byte bv) {
      return bv;
    }
    if (value instanceof Number nv) {
      return nv.byteValue();
    }
    if (value instanceof CharSequence cv) {
      var str = cv.toString().replace(",", "");
      return Try(() -> Byte.parseByte(str))
          .orElse(Try(() -> toByte(Long.parseLong(str))))
          .orElse(Try(() -> toByte(Double.parseDouble(str))))
          .getOrNull();
    }
    return null;
  }

  public static Short toShort(Object value) {
    if (value instanceof Short sv) {
      return sv;
    }
    if (value instanceof Number nv) {
      return nv.shortValue();
    }
    if (value instanceof CharSequence cv) {
      var str = cv.toString().replace(",", "");
      return Try(() -> Short.parseShort(str))
          .orElse(Try(() -> toShort(Long.parseLong(str))))
          .orElse(Try(() -> toShort(Double.parseDouble(str))))
          .getOrNull();
    }
    return null;
  }

  public static Integer toInteger(Object value) {
    if (value instanceof Integer iv) {
      return iv;
    }
    if (value instanceof Number nv) {
      return nv.intValue();
    }
    if (value instanceof CharSequence cv) {
      var str = cv.toString().replace(",", "");
      return Try(() -> Integer.parseInt(str))
          .orElse(Try(() -> toInteger(Long.parseLong(str))))
          .orElse(Try(() -> toInteger(Double.parseDouble(str))))
          .getOrNull();
    }
    return null;
  }

  public static Long toLong(Object value) {
    if (value instanceof Long lv) {
      return lv;
    }
    if (value instanceof Number nv) {
      return nv.longValue();
    }
    if (value instanceof CharSequence cv) {
      var str = cv.toString().replace(",", "");
      return Try(() -> Long.parseLong(str))
          .orElse(Try(() -> toLong(Double.parseDouble(str))))
          .getOrNull();
    }
    return null;
  }

  public static Float toFloat(Object value) {
    if (value instanceof Float fv) {
      return fv;
    }
    if (value instanceof Number nv) {
      return nv.floatValue();
    }
    if (value instanceof CharSequence cv) {
      var str = cv.toString().replace(",", "");
      return Try(() -> Float.parseFloat(str))
          .orElse(Try(() -> toFloat(Double.parseDouble(str))))
          .getOrNull();
    }
    return null;
  }

  public static Double toDouble(Object value) {
    if (value instanceof Double dv) {
      return dv;
    }
    if (value instanceof Number nv) {
      return nv.doubleValue();
    }
    if (value instanceof CharSequence cv) {
      var str = cv.toString().replace(",", "");
      return Try(() -> Double.parseDouble(str))
          .getOrNull();
    }
    return null;
  }

  public static BigInteger toBigInteger(Object value) {
    if (value instanceof BigInteger v) {
      return v;
    }
    if (value instanceof BigDecimal bv) {
      return bv.toBigInteger();
    }
    if (value instanceof Number nv) {
      return BigInteger.valueOf(nv.longValue());
    }
    if (value instanceof CharSequence cv) {
      var str = cv.toString().replace(",", "");
      return Try(() -> new BigInteger(str))
          .orElse(Try(() -> toBigInteger(new BigDecimal(str))))
          .getOrNull();
    }
    return null;
  }

  public static BigDecimal toBigDecimal(Object value) {
    if (value instanceof BigDecimal v) {
      return v;
    }
    if (value instanceof BigInteger bv) {
      return new BigDecimal(bv);
    }
    if (value instanceof Number nv) {
      return BigDecimal.valueOf(nv.doubleValue());
    }
    if (value instanceof CharSequence cv) {
      var str = cv.toString().replace(",", "");
      return Try(() -> new BigDecimal(str))
          .orElse(Try(() -> toBigDecimal(new BigInteger(str))))
          .getOrNull();
    }
    return null;
  }

  public static String toString(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof String v) {
      return v;
    }
    if (value instanceof CharSequence v) {
      return v.toString();
    }
    if (value instanceof LocalDateTime v) {
      return Dates.toString(v);
    }
    if (value instanceof LocalDate v) {
      return Dates.toString(v);
    }
    if (value instanceof Date v) {
      return Dates.toString(v);
    }
    return value.toString();
  }

  public static Boolean toBoolean(Object value) {
    if (value == null) {
      return false;
    }
    if (value instanceof Boolean v) {
      return v;
    }
    if (value instanceof Float v) {
      return v != 0;
    }
    if (value instanceof Double v) {
      return v != 0;
    }
    if (value instanceof Short v) {
      return v != 0;
    }
    if (value instanceof Integer v) {
      return v != 0;
    }
    if (value instanceof Long v) {
      return v != 0;
    }
    if (value instanceof BigDecimal v) {
      return v.floatValue() != 0;
    }
    if (value instanceof CharSequence v) {
      var str = v.toString();
      return !v.toString().isEmpty()
          && Try(() -> Boolean.parseBoolean(str))
          .orElse(() -> Try(() -> toBoolean(Integer.parseInt(str))))
          .getOrElse(true);
    }
    return true;
  }
}
