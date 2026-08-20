package com.ouroboros.data.normalize.normalizers;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.query.QueryExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.exception.NormalizeException;
import com.ouroboros.data.normalize.ClauseNormalizeContext;
import com.ouroboros.data.normalize.ClauseNormalizer;

/**
 * SELECT子句规范化器（上下文感知版本）
 */
public class DefaultSelectNormalizer implements ClauseNormalizer {

  @Override
  public boolean supports(String clauseType) {
    return "QUERY".equalsIgnoreCase(clauseType) || "SELECT".equalsIgnoreCase(clauseType);
  }

  @Override
  public QueryStatement.QueryStatementBuilder normalize(Map<String, ?> clauseData,
                                                        QueryStatement.QueryStatementBuilder builder,
                                                        ClauseNormalizeContext context) {
    Object select = Keyword.SELECT.findIn(clauseData);

    SExpression<?> selectExpr;

    if (select == null) {
      // 默认选择所有列
      selectExpr = SExpression.create(Operators.COLUMNS, "*");
    } else if (select instanceof SExpression<?> sExpr) {
      selectExpr = sExpr;
    } else if (select instanceof CharSequence selectStr) {
      // 字符串形式：解析逗号分隔的字段列表，支持 "field as alias" 语法
      selectExpr = buildColumns(parseStringSelect(selectStr.toString()), context);
    } else if (select instanceof List<?> selectList) {
      if (selectList.isEmpty() || selectList.size() == 1 && isWildcard(selectList.get(0))) {
        selectExpr = SExpression.create(Operators.COLUMNS, "*");
      } else {
        List<SExpression<?>> normalizedSelect = new ArrayList<>(selectList.size());
        boolean containsCanonicalEntry = false;
        for (int index = 0; index < selectList.size(); index++) {
          Object item = selectList.get(index);
          containsCanonicalEntry |= item instanceof SExpression<?>;
          normalizedSelect.add(normalizeSelectItem(item, context, "SELECT[" + index + "]"));
        }
        if (containsCanonicalEntry) {
          return builder.select(normalizedSelect);
        }
        selectExpr = SExpression.create(Operators.COLUMNS, normalizedSelect);
      }
    } else if (select instanceof Map<?, ?> selectMap) {
      // Map形式：{alias: field}
      selectExpr = buildColumns(selectMap.entrySet().stream()
          .map(entry -> Collections.singletonMap(entry.getKey(), entry.getValue()))
          .collect(Collectors.toList()), context);
    } else {
      throw new NormalizeException("不支持的 SELECT 子句类型: " + select.getClass().getName());
    }

    return builder.select(selectExpr);
  }

  /**
   * 解析字符串形式的SELECT子句
   * 支持：field1, field2, field3 as alias3
   */
  private List<Object> parseStringSelect(String selectStr) {
    return Arrays.stream(selectStr.split(","))
        .map(String::trim)
        .map(field -> {
          // 支持两种 SQL 标准别名语法：
          // 1. "field AS alias"（显式 AS 关键字，不区分大小写）
          // 2. "field alias"（空格分隔，SQL 标准允许省略 AS）
          // 正则 "(?i)( as | )" 同时匹配两种形式
          String[] fieldArr = field.split("(?i)( as | )");
          if (fieldArr.length == 1) {
            // 没有别名
            return field;
          } else if (fieldArr.length == 2) {
            // 有别名：{alias: field}
            return Collections.singletonMap(fieldArr[1].trim(), fieldArr[0].trim());
          }
          return null;
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private SExpression<?> buildColumns(List<?> rawItems, ClauseNormalizeContext context) {
    if (rawItems.isEmpty()) {
      return SExpression.create(Operators.COLUMNS, "*");
    }
    if (rawItems.size() == 1 && isWildcard(rawItems.get(0))) {
      return SExpression.create(Operators.COLUMNS, "*");
    }

    List<SExpression<?>> fields = new ArrayList<>();
    for (int i = 0; i < rawItems.size(); i++) {
      fields.add(normalizeSelectItem(rawItems.get(i), context, "SELECT[" + i + "]"));
    }
    return SExpression.create(Operators.COLUMNS, fields);
  }

  private SExpression<?> normalizeSelectItem(Object rawItem,
                                             ClauseNormalizeContext context,
                                             String expressionPath) {
    if (rawItem instanceof QueryExpression<?> expression) {
      Object rawValue = expression.toRawValue();
      if (rawValue == rawItem) {
        throw new NormalizeException("QueryExpression 不能返回自身作为 raw value: "
            + rawItem.getClass().getName());
      }
      return normalizeSelectItem(rawValue, context, expressionPath);
    }

    if (rawItem instanceof SExpression<?> sExpr) {
      return sExpr;
    }

    if (rawItem instanceof CharSequence text) {
      return buildFieldExpression(text.toString(), context, expressionPath);
    }

    if (rawItem instanceof Map<?, ?> aliasMap) {
      if (aliasMap.size() != 1) {
        throw new NormalizeException("查询字段别名映射必须只包含 1 个键值对: " + aliasMap);
      }
      Map.Entry<?, ?> entry = aliasMap.entrySet().iterator().next();
      String alias = String.valueOf(entry.getKey()).trim();
      if (alias.isEmpty()) {
        throw new NormalizeException("查询字段别名不能为空: " + aliasMap);
      }
      return SExpression.alias(
          normalizeSelectExpression(entry.getValue(), context, expressionPath + "." + alias),
          alias);
    }

    return normalizeSelectExpression(rawItem, context, expressionPath);
  }

  private SExpression<?> normalizeSelectExpression(Object rawExpression,
                                                   ClauseNormalizeContext context,
                                                   String expressionPath) {
    if (rawExpression instanceof CharSequence text) {
      return buildFieldExpression(text.toString(), context, expressionPath);
    }

    return context.normalizeExpression(rawExpression, expressionPath).getOrElseThrow(
        cause -> new NormalizeException("查询字段解析失败: " + expressionPath, cause));
  }

  private SExpression<?> buildFieldExpression(String rawField,
                                              ClauseNormalizeContext context,
                                              String expressionPath) {
    String field = rawField.trim();
    if (field.isEmpty()) {
      throw new NormalizeException("查询字段不能为空: " + expressionPath);
    }
    if ("*".equals(field)) {
      return SExpression.create(Operators.COLUMNS, "*");
    }
    if (isIntegerLiteral(field)) {
      return SExpression.constant(parseIntegerLiteral(field));
    }
    if (isDecimalLiteral(field)) {
      return SExpression.constant(new BigDecimal(field));
    }

    List<SExpression<?>> segments = Arrays.stream(field.split("\\."))
        .map(String::trim)
        .filter(segment -> !segment.isEmpty())
        .map(SExpression::constant)
        .collect(Collectors.toList());
    if (segments.isEmpty()) {
      throw new NormalizeException("查询字段不能为空: " + expressionPath);
    }

    return context.buildSExpression(Operators.FIELD, segments, expressionPath).getOrElseThrow(
        cause -> new NormalizeException("查询字段解析失败: " + expressionPath, cause));
  }

  private boolean isWildcard(Object item) {
    return item instanceof CharSequence text && "*".equals(text.toString().trim());
  }

  private boolean isIntegerLiteral(String field) {
    return field.matches("[+-]?\\d+");
  }

  private Object parseIntegerLiteral(String field) {
    try {
      return Integer.valueOf(field);
    } catch (NumberFormatException exception) {
      return Long.valueOf(field);
    }
  }

  private boolean isDecimalLiteral(String field) {
    return field.matches("[+-]?\\d+\\.\\d+");
  }
}
