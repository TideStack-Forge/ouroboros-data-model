package com.ouroboros.data.normalize.expressionnormalizers;

import static com.ouroboros.data.dsl.StatementPredicates.isBasicType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.exception.NormalizeException;
import com.ouroboros.data.normalize.ExpressionNormalizeContext;
import com.ouroboros.data.normalize.OperatorAliasResolver;
import com.ouroboros.data.normalize.RawExpressionNormalizer;
import com.querydsl.core.types.Operator;

/**
 * List表达式规范化器（上下文感知版本）
 * <p>
 * 对应旧代码的 ListRawExpressionNormalizer，支持S表达式语法：[operator, arg1, arg2, ...]
 */
public class ListExpressionNormalizer implements RawExpressionNormalizer {

  private static final Set<String> EXPRESSION_CLAUSE_TYPES =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList("WHERE", "HAVING", "QUERY", "EXPRESSION")));

  @Override
  public boolean supports(String clauseType, Class<?> rawExpressionType) {
    return EXPRESSION_CLAUSE_TYPES.contains(clauseType.toUpperCase())
        && List.class.isAssignableFrom(rawExpressionType);
  }

  @Override
  public SExpression<?> normalize(Object rawExpression, ExpressionNormalizeContext context) {
    if (!(rawExpression instanceof List<?> exprList)) {
      return null; // 不处理非List类型，交给下一个normalizer
    }

    // 空列表视为错误
    if (exprList.isEmpty()) {
      throw new NormalizeException("表达式列表不能为空 在 " + context.getExpressionPath());
    }

    Object first = exprList.get(0);
    if (first == null) {
      throw new NormalizeException("表达式列表的第一个元素（操作符）不能为null 在 " + context.getExpressionPath());
    }

    if (exprList.size() == 1 && first instanceof CharSequence) {
      return buildSExpression(Operators.FIELD, Collections.singletonList(first.toString()), context);
    }

    Operator operator;
    List<?> params;
    if (first instanceof List<?> || first instanceof Map<?, ?>) {
      operator = Operators.AND;
      params = exprList;
    } else {
      operator = determineOperator(first, exprList, context);
      params = exprList.subList(1, exprList.size());
    }

    // 构建表达式
    return buildSExpression(operator, params, context);
  }

  /**
   * 确定操作符
   */
  private Operator determineOperator(Object first, List<?> exprList, ExpressionNormalizeContext context) {
    // 情况1：第一个元素是Operator对象
    if (first instanceof Operator op) {
      return op;
    }

    // 情况2：第一个元素是字符串（操作符名称）
    if (first instanceof CharSequence str) {
      String operatorName = str.toString();
      return getOperatorByName(operatorName, context);
    }

    // 情况3：第一个元素是List或Map - 视为AND操作（多个条件并列）
    if (first instanceof List<?> || first instanceof Map<?, ?>) {
      // 这种情况下，整个列表的所有元素都是条件，用AND组合
      // 不需要确定操作符，直接返回AND
      return Operators.AND;
    }

    // 其他情况视为错误
    throw new NormalizeException("无法识别的表达式列表格式，第一个元素必须是操作符或条件: "
        + first.getClass().getName() + " 在 " + context.getExpressionPath());
  }

  /**
   * 根据名称获取操作符
   */
  private Operator getOperatorByName(String operatorName, ExpressionNormalizeContext context) {
    return OperatorAliasResolver.resolveOperator(operatorName);
  }

  /**
   * 构建S表达式
   */
  private SExpression<?> buildSExpression(Operator operator, List<?> params, ExpressionNormalizeContext context) {
    if (operator == Operators.CONSTANT) {
      return buildConstantExpression(params, context);
    }

    return buildDefaultExpression(operator, params, context);
  }

  /**
   * 构建常量表达式
   */
  private SExpression<?> buildConstantExpression(List<?> params, ExpressionNormalizeContext context) {
    if (params.size() != 1) {
      throw new NormalizeException("常量表达式需要恰好1个参数，但得到 " + params.size() + " 个");
    }
    return context.getClauseContext().buildSExpression(
        Operators.CONSTANT,
        Collections.singletonList(SExpression.constant(params.get(0))),
        context.getExpressionPath()).get();
  }

  /**
   * 构建默认表达式
   */
  private SExpression<?> buildDefaultExpression(Operator operator, List<?> params, ExpressionNormalizeContext context) {
    if (operator == Operators.EXISTS
        && params.size() == 1
        && params.get(0) instanceof Map<?, ?> subQueryMap
        && looksLikeQueryMap(subQueryMap)) {
      return SExpression.create(operator, normalizeQueryParam(subQueryMap, context));
    }

    List<SExpression<?>> normalizedParams = new ArrayList<>();
    for (int i = 0; i < params.size(); i++) {
      Object param = params.get(i);
      String paramPath = context.getExpressionPath() + "[" + (i + 1) + "]";
      normalizedParams.add(normalizeParam(param, paramPath, context));
    }

    return context.getClauseContext().buildSExpression(operator, normalizedParams, context.getExpressionPath()).get();
  }

  private SExpression<?> normalizeParam(Object param, String paramPath, ExpressionNormalizeContext context) {
    if (param instanceof SExpression<?> sExpr) {
      return sExpr;
    }
    if (isBasicType.test(param)) {
      return SExpression.constant(param);
    }
    try {
      return context.getClauseContext().normalizeExpression(param, paramPath).get();
    } catch (Exception e) {
      throw new NormalizeException("规范化操作符参数失败 在 " + paramPath + ": " + e.getMessage(), e);
    }
  }

  private QueryStatement normalizeQueryParam(Map<?, ?> rawMap, ExpressionNormalizeContext context) {
    try {
      return context.getQueryContext().normalizeQuery(castToStringKeyMap(rawMap)).get();
    } catch (Exception e) {
      throw new NormalizeException("规范化 EXISTS 子查询失败 在 " + context.getExpressionPath() + ": " + e.getMessage(), e);
    }
  }

  private boolean looksLikeQueryMap(Map<?, ?> map) {
    for (Object key : map.keySet()) {
      if (key instanceof String keyName) {
        Keyword keyword = Keyword.fromString(keyName);
        if (keyword == Keyword.SELECT || keyword == Keyword.FROM || keyword == Keyword.WHERE
            || keyword == Keyword.GROUP || keyword == Keyword.HAVING || keyword == Keyword.ORDER
            || keyword == Keyword.LIMIT || keyword == Keyword.OFFSET || keyword == Keyword.JOIN
            || keyword == Keyword.UNION || keyword == Keyword.UNION_ALL || keyword == Keyword.WITH
            || keyword == Keyword.DISTINCT) {
          return true;
        }
      }
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  private Map<String, ?> castToStringKeyMap(Map<?, ?> map) {
    for (Object key : map.keySet()) {
      if (!(key instanceof String)) {
        throw new NormalizeException("EXISTS 子查询的 key 必须是 String，实际: "
            + (key == null ? "null" : key.getClass().getName()));
      }
    }
    return (Map<String, ?>) map;
  }
}
