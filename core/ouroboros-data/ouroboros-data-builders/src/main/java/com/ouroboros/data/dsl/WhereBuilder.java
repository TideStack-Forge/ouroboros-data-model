package com.ouroboros.data.dsl;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.collections4.map.SingletonMap;

import io.vavr.Tuple;
import io.vavr.Tuple3;

/**
 * WIP: Where 链式构造器
 */
public class WhereBuilder {
  private final List<Map<String, Object>> conditions = new ArrayList<>();
  private String logicalOperator = "AND";

  private WhereBuilder(String logicalOperator) {
    this.logicalOperator = logicalOperator;
  }

  public static WhereBuilder create() {
    return new WhereBuilder("AND");
  }

  public static WhereBuilder createOr() {
    return new WhereBuilder("OR");
  }

  public ConditionsGroup and() {
    return new ConditionsGroup("AND");
  }

  public ConditionsGroup not() {
    return new ConditionsGroup("NOT");
  }

  public ConditionsGroup or() {
    return new ConditionsGroup("OR");
  }

  public Map<String, Object> build() {
    return new SingletonMap<>(this.logicalOperator, conditions);
  }

  public class ConditionsGroup {
    private final List<Tuple3<String, String, Object>> conditions = new ArrayList<>();
    private String logicOperator;
    private ConditionsGroup parentGroup;

    private ConditionsGroup(String logicOperator) {
      this.logicOperator = logicOperator;
    }

    /**
     * 添加条件
     *
     * @param field    字段
     * @param operator 操作符
     * @param value    值
     */
    public ConditionsGroup add(String field, String operator, Object value) {
      conditions.add(Tuple.of(field, operator, value));
      return this;
    }

    /**
     * 添加等于条件
     *
     * @param field 字段
     * @param value 值
     */
    public ConditionsGroup eq(String field, Comparable<?> value) {
      conditions.add(Tuple.of(field, "=", value));
      return this;
    }

    /**
     * 添加不等于条件
     *
     * @param field 字段
     * @param value 值
     */
    public ConditionsGroup ne(String field, Comparable<?> value) {
      conditions.add(Tuple.of(field, "!=", value));
      return this;
    }

    /**
     * 添加大于条件
     *
     * @param field 字段
     * @param value 值
     */
    public ConditionsGroup gt(String field, Comparable<?> value) {
      conditions.add(Tuple.of(field, ">", value));
      return this;
    }

    /**
     * 添加大于等于条件
     *
     * @param field 字段
     * @param value 值
     */
    public ConditionsGroup ge(String field, Comparable<?> value) {
      conditions.add(Tuple.of(field, ">=", value));
      return this;
    }

    /**
     * 添加小于条件
     *
     * @param field 字段
     * @param value 值
     */
    public ConditionsGroup lt(String field, Comparable<?> value) {
      conditions.add(Tuple.of(field, "<", value));
      return this;
    }

    /**
     * 添加小于等于条件
     *
     * @param field 字段
     * @param value 值
     */
    public ConditionsGroup le(String field, Comparable<?> value) {
      conditions.add(Tuple.of(field, "<=", value));
      return this;
    }

    /**
     * 添加 Like 条件
     *
     * @param field 字段
     * @param value 值
     */
    public ConditionsGroup like(String field, CharSequence value) {
      conditions.add(Tuple.of(field, "~", value.toString()));
      return this;
    }

    /**
     * 添加 Not like 条件
     *
     * @param field 字段
     * @param value 值
     */
    public ConditionsGroup notLike(String field, CharSequence value) {
      conditions.add(Tuple.of(field, "!~", value.toString()));
      return this;
    }

    /**
     * 添加 startsWith 条件(left like)
     *
     * @param field 字段
     * @param value 值
     */
    public ConditionsGroup startsWith(String field, CharSequence value) {
      conditions.add(Tuple.of(field, "STARTS_WITH", value.toString()));
      return this;
    }

    /**
     * 添加 endsWith 条件(right like)
     *
     * @param field 字段
     * @param value 值
     */
    public ConditionsGroup endsWith(String field, CharSequence value) {
      conditions.add(Tuple.of(field, "ENDS_WITH", value.toString()));
      return this;
    }

    /**
     * 添加 IN 条件
     *
     * @param field 字段
     * @param value 值
     */
    public ConditionsGroup in(String field, Collection<Object> value) {
      conditions.add(Tuple.of(field, "IN", new ArrayList<>(value)));
      return this;
    }

    /**
     * 添加 Not In 条件
     *
     * @param field 字段
     * @param value 值
     */
    public ConditionsGroup notIn(String field, Collection<Object> value) {
      conditions.add(Tuple.of(field, "NOT_IN", new ArrayList<>(value)));
      return this;
    }

    /**
     * 添加BETWEEN条件
     *
     * @param field  字段
     * @param value1 起始值
     * @param value2 结束值
     * @return 条件组
     */
    public ConditionsGroup between(String field, Comparable<?> value1, Comparable<?> value2) {
      conditions.add(Tuple.of(field, "BETWEEN", Arrays.asList(value1, value2)));
      return this;
    }

    /**
     * 添加为空条件
     *
     * @param field 字段
     */
    public ConditionsGroup isNull(String field) {
      conditions.add(Tuple.of(field, "IS_NULL", null));
      return this;
    }

    /**
     * 添加不为空条件
     *
     * @param field 字段
     */
    public ConditionsGroup isNotNull(String field) {
      conditions.add(Tuple.of(field, "IS_NOT_NULL", null));
      return this;
    }

    /**
     * 结束条件组
     *
     * @return 回到WhereBuilder
     */
    public WhereBuilder end() {
      var conditions = this.conditions.stream()
          .map(condition -> {
            var field = condition._1();
            var operator = condition._2();
            var value = condition._3();
            if (value == null) {
              return Arrays.asList(operator, Collections.singletonList(field));
            }
            return Collections.singletonMap(field, Collections.singletonMap(operator, value));
          })
          .collect(Collectors.toList());
      Map<String, Object> group = new SingletonMap<>(this.logicOperator, conditions);
      WhereBuilder.this.conditions.add(group);
      return WhereBuilder.this;
    }
  }
}
