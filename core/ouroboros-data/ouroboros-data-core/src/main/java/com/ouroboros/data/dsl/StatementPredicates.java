package com.ouroboros.data.dsl;

import static io.vavr.Predicates.anyOf;
import static io.vavr.Predicates.instanceOf;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.ouroboros.data.normalize.OperatorFactory;
import com.ouroboros.data.util.DataMaps;

@SuppressWarnings(value = {"rawtypes", "unchecked"})
public class StatementPredicates {

  // 是否为基本数据类型
  public static Predicate<? super Object> isBasicType = value -> (value instanceof CharSequence
      || value instanceof Number
      || value instanceof Boolean
      || value instanceof Date
      || value instanceof LocalDate
      || value instanceof LocalDateTime
      || value instanceof LocalTime);
  // 是否为基本数据类型List
  public static Predicate<List> isBasicTypeList = list -> list != null
      && ((List<?>) list).stream().allMatch(isBasicType);
  // 是否为Key类型为String的Map
  public static Predicate<Map> isStringKeyMap = DataMaps::isStringKeyMap;
  // 判断是否为子查询
  public static Predicate<Map> isQueryMap = map -> map.size() > 1 && map.keySet()
      .stream()
      .map(k -> k.toString().toUpperCase())
      .anyMatch(Arrays.asList("FROM")::contains);
  // 是否为SExpression形式
  public static Predicate<List<?>> isSExpressionList = list -> {
    if (list == null) {
      return false;
    }
    if (list.isEmpty()) {
      return true;
    }
    return list.get(0) instanceof String
        && OperatorFactory.getOperator(list.get(0).toString()).isPresent()
        && list.stream().skip(1).allMatch(anyOf(isBasicType, instanceOf(List.class), instanceOf(Map.class)));
  };

}
