package com.ouroboros.data.util;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import io.vavr.control.Try;

import com.ouroboros.data.exception.InvalidEntityName;
import com.ouroboros.data.exception.StatementCheckFailure;
import com.ouroboros.data.exception.StatementCheckFailure.FailureType;
import com.ouroboros.data.exception.ValuesError;
import com.ouroboros.data.util.DataMaps;

public final class Asserts {

  // 关键词列表，待完善
  private static final List<String> keywords = Collections.emptyList();
  // 合法路径正则
  private static final String namePattern = "^[a-zA-Z_\u4e00-\u9fa5]+[a-zA-Z_\\-\\d\u4e00-\u9fa5]*$";

  // 校验是否为合法的路径
  private static void assertPathName(String pathName) throws IllegalArgumentException {
    if (pathName == null) {
      throw new InvalidNameFormatException();
    }
    var arr = pathName.split("(?i)( as | )");
    if (arr.length > 2) {
      throw new InvalidNameFormatException();
    }
    var namePath = Arrays.stream(StringUtils.defaultIfBlank(arr[0], "").split("\\."))
        .map(String::trim)
        .collect(Collectors.toList());
    if (namePath.size() == 0 || namePath.stream().anyMatch(StringUtils::isBlank)) {
      throw new NamePathContainsBlankException();
    }
    if (namePath.stream().anyMatch(keywords::contains)) {
      throw new NamePathContainsKeywordException();
    }
    if (namePath.stream().anyMatch(path -> !path.matches(namePattern))) {
      throw new InvalidNameFormatException();
    }
  }

  public static void assertValidPathNode(String pathNode) {
    if (StringUtils.isBlank(pathNode)) {
      throw new InvalidNameFormatException();
    }
    if (keywords.contains(pathNode)) {
      throw new NamePathContainsKeywordException();
    }
    if (!pathNode.matches(namePattern)) {
      throw new InvalidNameFormatException();
    }
  }

  /**
   * 合法的表名断言
   *
   * @param entityName 合法的表名不能为空，必须为字母开头，可用字母、数字、下划线以及中文，允许以 `.` 分割表示完整路径
   * @throws InvalidEntityName
   */
  public static void assertValidEntityName(String entityName) throws InvalidEntityName {
    try {
      assertPathName(entityName);
    } catch (NamePathContainsBlankException e) {
      throw new InvalidEntityName("entity name path contains blank", Optional.of(entityName));
    } catch (NamePathContainsKeywordException e) {
      throw new InvalidEntityName("entity name path contains keyword", Optional.of(entityName));
    } catch (InvalidNameFormatException e) {
      throw new InvalidEntityName("entity name format is invalid", Optional.of(entityName));
    }
  }

  /**
   * 合法的字段名断言
   *
   * @param fieldName 合法的字段名不能为空，必须为字母开头，可用字母、数字、下划线以及中文，允许以 `.` 分割表示完整路径
   * @throws StatementCheckFailure
   */
  public static void assertValidFieldName(String fieldName) throws StatementCheckFailure {
    try {
      assertPathName(fieldName);
    } catch (NamePathContainsBlankException e) {
      throw new StatementCheckFailure(fieldName, FailureType.INVALID_FIELD, "Field name can not be blank");
    } catch (NamePathContainsKeywordException e) {
      throw new StatementCheckFailure(fieldName, FailureType.INVALID_FIELD, "Field name can not be keyword");
    } catch (InvalidNameFormatException e) {
      throw new StatementCheckFailure(fieldName, FailureType.INVALID_FIELD, "Field name is invalid");
    }
  }

  /**
   * 合法值Map断言
   *
   * @param values 验证对象
   * @throws ValuesError
   */
  public static void assertValidValueMap(Map<?, ?> values) throws ValuesError {

    if (MapUtils.isEmpty(values)) {
      throw new ValuesError("values is empty", Optional.empty());
    }

    if (!DataMaps.isStringKeyMap(values)) {
      throw new ValuesError("values is not a string key map", Optional.empty());
    }

    // 检查字段名
    var invalidFields = values.keySet()
        .stream()
        .map(f -> Try.run(() -> assertValidFieldName((String) f)))
        .filter(Try::isFailure)
        .map(f -> (StatementCheckFailure) f.getCause())
        .collect(Collectors.toMap(
            StatementCheckFailure::getFieldName,
            v -> v,
            (vf1, vf2) -> vf1,
            LinkedHashMap::new));

    if (MapUtils.isNotEmpty(invalidFields)) {
      throw new ValuesError("Invalid field names", Optional.of(invalidFields));
    }
  }

  /**
   * List中的Try全部为success断言
   *
   * @param tryList
   * @throws Throwable
   */
  public static void assertAllSuccess(List<? extends Try<?>> tryList) throws Throwable {
    assertAllSuccess(tryList.stream());
  }

  /**
   * Stream中的Try全部为success断言
   *
   * @param tryStream
   * @throws Throwable
   */
  public static void assertAllSuccess(Stream<? extends Try<?>> tryStream) throws Throwable {
    var firstFailure = tryStream
        .filter(Try::isFailure)
        .findFirst();
    if (firstFailure.isPresent()) {
      throw firstFailure.get().getCause();
    }
  }

  static class InvalidNameFormatException extends IllegalArgumentException {
    InvalidNameFormatException() {
      super();
    }
  }

  static class NamePathContainsBlankException extends IllegalArgumentException {
    NamePathContainsBlankException() {
      super();
    }
  }

  static class NamePathContainsKeywordException extends IllegalArgumentException {
    NamePathContainsKeywordException() {
      super();
    }
  }
}
