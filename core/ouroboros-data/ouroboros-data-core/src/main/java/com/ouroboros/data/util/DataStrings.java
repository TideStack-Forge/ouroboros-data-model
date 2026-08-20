package com.ouroboros.data.util;

public final class DataStrings {
  private DataStrings() {
  }

  public static String toSnakeCase(String value) {
    if (value == null || value.isEmpty()) {
      return value;
    }
    var result = new StringBuilder(value.length() + 8);
    for (int i = 0; i < value.length(); i++) {
      var current = value.charAt(i);
      if (Character.isUpperCase(current)) {
        if (shouldInsertDelimiter(value, i) && result.length() > 0 && result.charAt(result.length() - 1) != '_') {
          result.append('_');
        }
        result.append(Character.toLowerCase(current));
      } else {
        result.append(current);
      }
    }
    return result.toString();
  }

  private static boolean shouldInsertDelimiter(String value, int index) {
    if (index == 0) {
      return false;
    }
    var previous = value.charAt(index - 1);
    return Character.isLowerCase(previous)
        || Character.isDigit(previous)
        || index + 1 < value.length() && Character.isLowerCase(value.charAt(index + 1));
  }
}
