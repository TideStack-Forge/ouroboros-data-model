package com.ouroboros.util;

@SuppressWarnings("unused")
public class Strings {

  private Strings() {
  }

  public static boolean isBlank(CharSequence value) {
    if (value == null) {
      return true;
    }
    for (int i = 0; i < value.length(); i++) {
      if (!Character.isWhitespace(value.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  public static String upperFirst(String value) {
    if (value == null || value.isEmpty()) {
      return value;
    }
    char first = value.charAt(0);
    char upperFirst = Character.toUpperCase(first);
    if (first == upperFirst) {
      return value;
    }
    return upperFirst + value.substring(1);
  }

  public static String toSnakeCase(String value) {
    return toDelimitedCase(value, '_');
  }

  public static String toKebabCase(String value) {
    return toDelimitedCase(value, '-');
  }

  private static String toDelimitedCase(String value, char delimiter) {
    if (value == null || value.isEmpty()) {
      return value;
    }

    StringBuilder result = new StringBuilder(value.length() + 8);
    for (int i = 0; i < value.length(); i++) {
      char current = value.charAt(i);
      if (Character.isUpperCase(current)) {
        boolean insertDelimiter = shouldInsertDelimiter(value, i);
        if (insertDelimiter) {
          appendDelimiter(result, delimiter);
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
    char previous = value.charAt(index - 1);
    if (Character.isLowerCase(previous) || Character.isDigit(previous)) {
      return true;
    }
    return index + 1 < value.length() && Character.isLowerCase(value.charAt(index + 1));
  }

  private static void appendDelimiter(StringBuilder result, char delimiter) {
    if (result.length() > 0 && result.charAt(result.length() - 1) != delimiter) {
      result.append(delimiter);
    }
  }
}
