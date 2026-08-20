package com.ouroboros.data.normalize.expressionnormalizers;

import static com.ouroboros.data.dsl.StatementPredicates.isBasicType;
import static com.ouroboros.data.dsl.StatementPredicates.isBasicTypeList;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.ExtOps;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.query.QueryCondition;
import com.ouroboros.data.dsl.query.QueryExpression;
import com.ouroboros.data.exception.NormalizeException;
import com.ouroboros.data.normalize.ExpressionNormalizeContext;
import com.ouroboros.data.normalize.OperatorAliasResolver;
import com.ouroboros.data.normalize.RawExpressionNormalizer;
import com.querydsl.core.types.Operator;

/**
 * Map表达式规范化器（上下文感知版本）
 * <p>
 * 对应旧代码的 MapRawExpressionNormalizer，保持原有的逻辑严密性，增加上下文感知能力
 */
public class MapExpressionNormalizer implements RawExpressionNormalizer {

  private static final Set<String> EXPRESSION_CLAUSE_TYPES =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList("WHERE", "HAVING", "QUERY", "EXPRESSION")));

  @Override
  public boolean supports(String clauseType, Class<?> rawExpressionType) {
    return EXPRESSION_CLAUSE_TYPES.contains(clauseType.toUpperCase())
        && Map.class.isAssignableFrom(rawExpressionType);
  }

  @Override
  public SExpression<?> normalize(Object rawExpression, ExpressionNormalizeContext context) {
    if (!(rawExpression instanceof Map<?, ?>)) {
      return null; // 不处理非Map类型，交给下一个normalizer
    }

    Map<?, ?> mapExpr = (Map<?, ?>) rawExpression;

    // 空Map返回空布尔表达式
    if (mapExpr.isEmpty()) {
      return SExpression.empty(Boolean.class);
    }

    // 处理每个键值对
    List<SExpression<?>> conditions = new ArrayList<>();
    for (Map.Entry<?, ?> entry : mapExpr.entrySet()) {
      String key = entry.getKey().toString();
      Object value = entry.getValue();

      SExpression<?> condition = buildEntryExpression(key, value, context);
      if (condition != null && !condition.isEmpty()) {
        conditions.add(condition);
      }
    }

    // 如果没有有效条件，返回空表达式
    if (conditions.isEmpty()) {
      return SExpression.empty(Boolean.class);
    }

    // 单个条件直接返回
    if (conditions.size() == 1) {
      return conditions.get(0);
    }

    // 多个条件用AND组合
    return context.getClauseContext().buildSExpression(Operators.AND, conditions, context.getExpressionPath()).get();
  }

  /**
   * 构建单个键值对的表达式
   */
  private SExpression<?> buildEntryExpression(String key, Object value, ExpressionNormalizeContext context) {
    // 检查是否为逻辑组合操作符
    if (isLogicOperator(key)) {
      return buildLogicCombination(key, value, context);
    }

    // 检查是否为$开头的操作符（但不是逻辑操作符）
    if (key.startsWith("$") && !isLogicOperator(key)) {
      // 尝试解析为特殊操作符（如 $constant）
      return buildSpecialOperatorExpression(key, value, context);
    }

    // 普通字段表达式
    return buildFieldExpression(key, value, context);
  }

  /**
   * 检查是否为逻辑操作符
   * <p>
   * $ 前缀在此处去除后再传给 OperatorAliasResolver（$ 前缀收口设计）
   */
  private boolean isLogicOperator(String key) {
    String stripped = key.startsWith("$") ? key.substring(1) : key;
    return OperatorAliasResolver.tryResolveOperator(stripped)
        .filter(Operators::isLogicCombinationOperator)
        .isPresent();
  }

  /**
   * 构建逻辑组合表达式
   */
  private SExpression<?> buildLogicCombination(String operatorKey, Object value, ExpressionNormalizeContext context) {
    // 获取操作符
    Operator operator = getLogicOperator(operatorKey);

    // 处理参数
    Stream<?> rawConditions;
    if (value instanceof Collection<?> paramList) {
      rawConditions = paramList.stream();
    } else if (value instanceof Map<?, ?> map) {
      // 如果value是Map，将每个entry转为单独的Map
      rawConditions = map.entrySet().stream()
          .map(entry -> Collections.singletonMap(entry.getKey(), entry.getValue()));
    } else {
      rawConditions = Stream.of(value);
    }

    // 递归规范化每个条件
    List<SExpression<?>> conditions = rawConditions
        .map(rawCond -> {
          try {
            return context.getClauseContext().normalizeExpression(rawCond, context.getExpressionPath() + "." + operatorKey).get();
          } catch (Exception e) {
            throw new NormalizeException("规范化逻辑组合条件失败: " + e.getMessage(), e);
          }
        })
        .collect(Collectors.toList());

    // 应用逻辑组合优化
    return context.getClauseContext().buildSExpression(operator, conditions, context.getExpressionPath()).get();
  }

  /**
   * 获取逻辑操作符
   * <p>
   * $ 前缀在此处去除后再传给 OperatorAliasResolver
   */
  private Operator getLogicOperator(String operatorKey) {
    String stripped = operatorKey.startsWith("$") ? operatorKey.substring(1) : operatorKey;
    return OperatorAliasResolver.resolveOperator(stripped);
  }

  /**
   * 构建字段表达式
   */
  private SExpression<?> buildFieldExpression(String fieldName, Object value, ExpressionNormalizeContext context) {
    List<SExpression<?>> fieldParams = Arrays.stream(fieldName.split("\\."))
        .map(String::trim)
        .map(SExpression::constant)
        .collect(Collectors.toList());
    SExpression<?> field = context.getClauseContext().buildSExpression(
        Operators.FIELD,
        fieldParams,
        context.getExpressionPath()).get();

    // 处理null值
    if (value == null) {
      return context.getClauseContext().buildSExpression(Operators.IS_NULL, Arrays.asList(field), context.getExpressionPath()).get();
    }

    if (value instanceof QueryExpression<?> || value instanceof QueryCondition) {
      SExpression<?> rightValue = normalizeOperatorValue(value, context.getExpressionPath() + "." + fieldName, context);
      return context.getClauseContext().buildSExpression(Operators.EQ, Arrays.asList(field, rightValue), context.getExpressionPath()).get();
    }

    // 处理基本类型 - 视为等于比较
    if (isBasicType.test(value)) {
      return context.getClauseContext().buildSExpression(Operators.EQ, Arrays.asList(field, SExpression.constant(value)), context.getExpressionPath()).get();
    }

    // 处理基本类型列表 - 视为IN操作
    if (value instanceof List<?> listValue && isBasicTypeList.test(listValue)) {
      return context.getClauseContext().buildSExpression(Operators.IN, Arrays.asList(field, SExpression.constant(listValue)), context.getExpressionPath()).get();
    }

    // 处理Map - 需要验证是操作符Map还是嵌套条件Map
    if (value instanceof Map<?, ?> map) {
      if (isMixedFieldAndNonLogicOperatorMap(map)) {
        throw new NormalizeException("字段 " + fieldName + " 的 Map 值不能混用非逻辑操作符 key 与字段/逻辑 key");
      }

      // 验证是否为操作符Map
      if (!isOperatorMap(map)) {
        // 这是嵌套条件Map，不是操作符Map
        return handleNestedConditionMap(fieldName, map, context);
      }
      return buildComplexFieldExpression(field, map, context);
    }

    throw new NormalizeException("无法识别的字段值类型: " + value.getClass().getName()
        + " 在字段 " + fieldName + " 的 " + context.getExpressionPath());
  }

  /**
   * 构建复杂的字段表达式（包含操作符的Map）
   */
  private SExpression<?> buildComplexFieldExpression(SExpression<?> field, Map<?, ?> operatorMap, ExpressionNormalizeContext context) {
    List<SExpression<?>> conditions = new ArrayList<>();

    for (Map.Entry<?, ?> entry : operatorMap.entrySet()) {
      String opKey = entry.getKey().toString();
      Object opValue = entry.getValue();

      // 规范化操作符名称：支持两种形式
      // 1. 无前缀形式（旧系统）：GT, IN, CONTAINS 等
      // 2. $ 前缀形式（新系统）：$gt, $in, $contains 等
      String normalizedOpKey = normalizeOperatorKey(opKey);

      // 构建操作符表达式
      SExpression<?> opExpr = buildOperatorExpression(field, normalizedOpKey, opValue, context);
      conditions.add(opExpr);
    }

    // 单个条件直接返回
    if (conditions.size() == 1) {
      return conditions.get(0);
    }

    // 多个条件用AND组合
    return context.getClauseContext().buildSExpression(Operators.AND, conditions, context.getExpressionPath()).get();
  }

  /**
   * 规范化操作符key：去掉$前缀（如果有）
   *
   * <p>$ 前缀仅作为 Map 输入的消歧策略，在此处去除后传给 OperatorAliasResolver。
   *
   * @param opKey 原始操作符key（可能带$前缀）
   * @return 规范化后的操作符key（不带$前缀）
   */
  private String normalizeOperatorKey(String opKey) {
    String stripped = opKey.startsWith("$") ? opKey.substring(1) : opKey;
    return stripped.toLowerCase();
  }

  /**
   * 构建操作符表达式
   */
  private SExpression<?> buildOperatorExpression(SExpression<?> field, String operatorKey, Object value, ExpressionNormalizeContext context) {
    // 获取QueryDSL操作符
    Operator operator = getComparisonOperator(operatorKey);

    // 根据操作符类型处理值
    if (operatorKey.equals("between")) {
      // BETWEEN需要两个参数
      if (!(value instanceof List<?> list) || list.size() != 2) {
        throw new NormalizeException("between操作符需要包含两个元素的列表");
      }
      return context.getClauseContext().buildSExpression(operator, Arrays.asList(
          field,
          normalizeOperatorValue(list.get(0), context.getExpressionPath() + "." + operatorKey + "[0]", context),
          normalizeOperatorValue(list.get(1), context.getExpressionPath() + "." + operatorKey + "[1]", context)
      ), context.getExpressionPath()).get();
    } else if (operatorKey.equals("in") || operatorKey.equals("notin") || operatorKey.equals("not_in")) {
      SExpression<?> rightValue = normalizeCollectionOperatorValue(
          value,
          context.getExpressionPath() + "." + operatorKey,
          context);
      return context.getClauseContext().buildSExpression(operator, Arrays.asList(field, rightValue), context.getExpressionPath()).get();
    } else if (operatorKey.equals("isnull") || operatorKey.equals("is_null")
        || operatorKey.equals("isnotnull") || operatorKey.equals("is_not_null")) {
      return context.getClauseContext().buildSExpression(operator, Arrays.asList(field), context.getExpressionPath()).get();
    } else if (Operators.isAggregationOperator(operator)) {
      SExpression<?> aggregateExpr = context.getClauseContext().buildSExpression(
          operator,
          Collections.singletonList(field),
          context.getExpressionPath()).get();
      return buildAggregateComparisonExpression(aggregateExpr, operatorKey, value, context);
    } else if (operator == ExtOps.REL_ANY || operator == ExtOps.REL_ALL || operator == ExtOps.REL_NONE) {
      SExpression<?> condition;
      try {
        condition = context.getClauseContext().normalizeExpression(
            value,
            context.getExpressionPath() + "." + operatorKey).get();
      } catch (Exception e) {
        throw new NormalizeException("规范化关联条件失败 在 "
            + context.getExpressionPath() + "." + operatorKey + ": " + e.getMessage(), e);
      }
      return context.getClauseContext().buildSExpression(operator, Arrays.asList(field, condition), context.getExpressionPath()).get();
    } else {
      // 一般的比较操作符
      SExpression<?> rightValue = normalizeOperatorValue(
          value,
          context.getExpressionPath() + "." + operatorKey,
          context);
      return context.getClauseContext().buildSExpression(operator, Arrays.asList(field, rightValue), context.getExpressionPath()).get();
    }
  }

  private SExpression<?> buildAggregateComparisonExpression(
      SExpression<?> aggregateExpr, String operatorKey, Object value, ExpressionNormalizeContext context) {
    String aggregatePath = context.getExpressionPath() + "." + operatorKey;

    if (value instanceof Map<?, ?> map) {
      if (!isOperatorMap(map)) {
        throw new NormalizeException(operatorKey + " 聚合比较只支持操作符 Map");
      }

      List<SExpression<?>> comparisons = new ArrayList<>();
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        String comparisonKey = normalizeOperatorKey(entry.getKey().toString());
        comparisons.add(buildAggregateComparisonOperatorExpression(
            aggregateExpr, comparisonKey, entry.getValue(), aggregatePath, context));
      }

      if (comparisons.size() == 1) {
        return comparisons.get(0);
      }
      return context.getClauseContext().buildSExpression(Operators.AND, comparisons, aggregatePath).get();
    }

    SExpression<?> rightValue = normalizeOperatorValue(value, aggregatePath, context);
    return context.getClauseContext().buildSExpression(
        Operators.EQ,
        Arrays.asList(aggregateExpr, rightValue),
        aggregatePath).get();
  }

  private SExpression<?> buildAggregateComparisonOperatorExpression(
      SExpression<?> aggregateExpr,
      String comparisonKey,
      Object value,
      String aggregatePath,
      ExpressionNormalizeContext context) {
    Operator comparisonOperator = getComparisonOperator(comparisonKey);
    if (Operators.isAggregationOperator(comparisonOperator)) {
      throw new NormalizeException("聚合比较不支持嵌套聚合操作符: " + comparisonKey);
    }

    String comparisonPath = aggregatePath + "." + comparisonKey;
    if (comparisonKey.equals("between")) {
      if (!(value instanceof List<?> list) || list.size() != 2) {
        throw new NormalizeException("between操作符需要包含两个元素的列表");
      }
      return context.getClauseContext().buildSExpression(comparisonOperator, Arrays.asList(
          aggregateExpr,
          normalizeOperatorValue(list.get(0), comparisonPath + "[0]", context),
          normalizeOperatorValue(list.get(1), comparisonPath + "[1]", context)
      ), comparisonPath).get();
    }

    if (comparisonKey.equals("in") || comparisonKey.equals("notin") || comparisonKey.equals("not_in")) {
      SExpression<?> rightValue = normalizeCollectionOperatorValue(value, comparisonPath, context);
      return context.getClauseContext().buildSExpression(
          comparisonOperator,
          Arrays.asList(aggregateExpr, rightValue),
          comparisonPath).get();
    }

    if (comparisonKey.equals("isnull") || comparisonKey.equals("is_null")
        || comparisonKey.equals("isnotnull") || comparisonKey.equals("is_not_null")) {
      return context.getClauseContext().buildSExpression(
          comparisonOperator,
          Collections.singletonList(aggregateExpr),
          comparisonPath).get();
    }

    SExpression<?> rightValue = normalizeOperatorValue(value, comparisonPath, context);
    return context.getClauseContext().buildSExpression(
        comparisonOperator,
        Arrays.asList(aggregateExpr, rightValue),
        comparisonPath).get();
  }

  private SExpression<?> normalizeOperatorValue(Object value, String expressionPath,
                                                ExpressionNormalizeContext context) {
    if (value instanceof SExpression<?>) {
      return (SExpression<?>) value;
    }

    if (value instanceof QueryExpression<?> || value instanceof QueryCondition) {
      try {
        return context.getClauseContext().normalizeExpression(value, expressionPath).get();
      } catch (Exception e) {
        throw new NormalizeException("规范化操作符右值失败 在 " + expressionPath + ": " + e.getMessage(), e);
      }
    }

    if (isBasicType.test(value)) {
      return SExpression.constant(value);
    }

    if (value instanceof List<?>) {
      try {
        return context.getClauseContext().normalizeExpression(value, expressionPath).get();
      } catch (Exception e) {
        throw new NormalizeException("规范化操作符右值失败 在 " + expressionPath + ": " + e.getMessage(), e);
      }
    }

    if (value instanceof Map<?, ?> map) {
      if (!isExplicitExpressionMap(map)) {
        throw new NormalizeException("Map 形式的操作符右值必须使用显式表达式语法（如 {$constant: ...}）");
      }
      try {
        return context.getClauseContext().normalizeExpression(value, expressionPath).get();
      } catch (Exception e) {
        throw new NormalizeException("规范化操作符右值失败 在 " + expressionPath + ": " + e.getMessage(), e);
      }
    }

    throw new NormalizeException("不支持的操作符值类型: " + value.getClass().getName());
  }

  private SExpression<?> normalizeCollectionOperatorValue(Object value, String expressionPath,
                                                          ExpressionNormalizeContext context) {
    if (value instanceof List<?> listValue && isBasicTypeList.test(listValue)) {
      return context.getClauseContext().buildSExpression(
          Operators.CONSTANT,
          Collections.singletonList(SExpression.constant(listValue)),
          expressionPath).get();
    }

    return normalizeOperatorValue(value, expressionPath, context);
  }

  private boolean isExplicitExpressionMap(Map<?, ?> map) {
    if (map.size() != 1) {
      return false;
    }
    Object key = map.keySet().iterator().next();
    return key != null && key.toString().startsWith("$");
  }

  /**
   * 获取比较操作符
   * <p>
   * operatorKey 已经过 normalizeOperatorKey 处理，不含 $ 前缀
   */
  private Operator getComparisonOperator(String operatorKey) {
    return OperatorAliasResolver.resolveOperator(operatorKey);
  }

  /**
   * 处理嵌套条件Map（非操作符Map）
   *
   * <p>当遇到 {"fieldName": {"key": "value"}} 这样的结构时，
   * 需要判断这是关联查询还是普通的嵌套Map值。
   *
   * <p>示例输入：{"user": {"name": "张三"}}
   *
   * @param fieldName 字段名（如 "user"）
   * @param nestedMap 嵌套的Map（如 {"name": "张三"}）
   * @param context   表达式规范化上下文
   * @return 规范化后的表达式
   */
  private SExpression<?> handleNestedConditionMap(String fieldName, Map<?, ?> nestedMap,
                                                  ExpressionNormalizeContext context) {
    // 统一递归处理：对 nestedMap 中每个 entry 构建表达式，FIELD 添加 fieldName 前缀
    List<SExpression<?>> conditions = new ArrayList<>();
    for (Map.Entry<?, ?> entry : nestedMap.entrySet()) {
      String nestedKey = entry.getKey().toString();
      Object nestedValue = entry.getValue();
      SExpression<?> innerExpr = buildNestedMapEntryExpression(nestedKey, nestedValue, context);
      SExpression<?> prefixed = addPathPrefixToFields(innerExpr, fieldName, context);
      if (prefixed != null && !prefixed.isEmpty()) {
        conditions.add(prefixed);
      }
    }

    if (conditions.isEmpty()) {
      return SExpression.empty(Boolean.class);
    }

    if (conditions.size() == 1) {
      return conditions.get(0);
    }

    return context.getClauseContext().buildSExpression(Operators.AND, conditions, context.getExpressionPath()).get();
  }

  private SExpression<?> buildNestedMapEntryExpression(String key, Object value,
                                                       ExpressionNormalizeContext context) {
    if (isLogicOperator(key)) {
      return buildLogicCombination(key, value, context);
    }

    // 嵌套 Map 在 operatorMap 门控失败后，剩余 key 一律视为字段路径的一部分。
    // 这样既保留逻辑操作符能力，也能兼容 "$unknown" / 混合 key 的字段回退语义。
    return buildFieldExpression(key, value, context);
  }

  /**
   * 检查Map是否为操作符Map
   *
   * <p>操作符Map的特征：所有key都是已知的操作符（如 $gt, $in）
   * <p>嵌套条件Map的特征：key是字段名（如 name, age）
   *
   * <p>示例：
   * <ul>
   *   <li>{"$gt": 18, "$lt": 65} - 操作符Map ✓</li>
   *   <li>{"name": "张三", "age": 30} - 嵌套条件Map ✗</li>
   *   <li>{"$gt": 18} - 操作符Map ✓</li>
   *   <li>{"user": {"name": "张三"}} - 嵌套条件Map ✗</li>
   * </ul>
   *
   * @param map 待检查的Map
   * @return true 如果是操作符Map
   */
  private boolean isOperatorMap(Map<?, ?> map) {
    if (map.isEmpty()) {
      return false;
    }

    // 仅“全部为非逻辑操作符 key”的 Map 才视为字段操作符 Map。
    // 逻辑操作符（AND/OR/NOT）在嵌套条件 Map 中允许与字段 key 并存。
    return map.keySet().stream()
        .allMatch(key -> isNonLogicOperatorKey(key.toString()));
  }

  private boolean isMixedFieldAndNonLogicOperatorMap(Map<?, ?> map) {
    boolean hasNonLogicOperatorKey = false;
    boolean hasFieldOrLogicKey = false;

    for (Object key : map.keySet()) {
      String keyString = key.toString();
      if (isNonLogicOperatorKey(keyString)) {
        hasNonLogicOperatorKey = true;
      } else {
        hasFieldOrLogicKey = true;
      }

      if (hasNonLogicOperatorKey && hasFieldOrLogicKey) {
        return true;
      }
    }

    return false;
  }

  private boolean isNonLogicOperatorKey(String key) {
    String normalizedKey = normalizeOperatorKey(key);
    return OperatorAliasResolver.tryResolveOperator(normalizedKey)
        .filter(operator -> !Operators.isLogicCombinationOperator(operator))
        .isPresent();
  }

  /**
   * 遍历表达式树，给所有 FIELD 操作符添加路径前缀
   *
   * <p>示例：
   * <ul>
   *   <li>FIELD("name") → FIELD("user", "name")</li>
   *   <li>FIELD("dept", "name") → FIELD("user", "dept", "name")</li>
   * </ul>
   */
  private SExpression<?> addPathPrefixToFields(SExpression<?> expr, String prefix,
                                               ExpressionNormalizeContext context) {
    if (expr == null || expr.isEmpty()) {
      return expr;
    }

    // 如果是 FIELD 操作符，添加前缀
    if (expr.getOperator() == Operators.FIELD) {
      List<SExpression<?>> fieldParams = new ArrayList<>();
      fieldParams.add(SExpression.constant(prefix));
      for (Object param : expr.getParams()) {
        fieldParams.add(SExpression.constant(param));
      }
      return context.getClauseContext().buildSExpression(
          Operators.FIELD,
          fieldParams,
          context.getExpressionPath()).get();
    }

    if (expr.getOperator() == Operators.CONSTANT) {
      return expr;
    }

    if (expr.getOperator() == ExtOps.REL_ANY
        || expr.getOperator() == ExtOps.REL_ALL
        || expr.getOperator() == ExtOps.REL_NONE) {
      SExpression<?> relationField = addPathPrefixToFields(expr.getParamAsSExpression(0), prefix, context);
      SExpression<?> condition = expr.getParamAsSExpression(1);
      return context.getClauseContext().buildSExpression(
          expr.getOperator(),
          Arrays.asList(relationField, condition),
          context.getExpressionPath()).get();
    }

    // 递归处理子表达式
    List<Object> newParams = new ArrayList<>();
    List<SExpression<?>> normalizedSExprParams = new ArrayList<>();
    boolean allParamsAreSExpressions = true;
    for (Object param : expr.getParams()) {
      if (param instanceof SExpression) {
        SExpression<?> rewritten = addPathPrefixToFields((SExpression<?>) param, prefix, context);
        newParams.add(rewritten);
        normalizedSExprParams.add(rewritten);
      } else {
        newParams.add(param);
        allParamsAreSExpressions = false;
      }
    }

    if (allParamsAreSExpressions) {
      return context.getClauseContext().buildSExpression(
          expr.getOperator(),
          normalizedSExprParams,
          context.getExpressionPath()).get();
    }

    throw new NormalizeException("树改写阶段发现非法 mixed-param 表达式: operator="
        + expr.getOperator() + ", 仅 FIELD/CONSTANT 允许保留原始值参数");
  }

  /**
   * 构建特殊操作符表达式（$constant等）
   *
   * <p>处理在Map顶层使用的特殊操作符，如 {"$constant": value}。
   * 这些操作符不是字段操作符，也不是逻辑组合操作符，而是直接产生表达式的特殊操作符。
   * <p>
   * $ 前缀在此处去除后再传给 OperatorAliasResolver。
   *
   * @param operatorKey 操作符key（如 "$constant"）
   * @param value       操作符值
   * @param context     表达式规范化上下文
   * @return 规范化后的表达式
   * @throws NormalizeException 如果操作符无法识别或不支持在Map顶层使用
   */
  private SExpression<?> buildSpecialOperatorExpression(String operatorKey, Object value,
                                                        ExpressionNormalizeContext context) {
    // 规范化操作符key（去$前缀，转小写）
    String normalizedKey = normalizeOperatorKey(operatorKey);

    // 尝试解析操作符
    Optional<Operator> operatorOpt = OperatorAliasResolver.tryResolveOperator(normalizedKey);

    if (!operatorOpt.isPresent()) {
      throw new NormalizeException("无法识别的操作符: " + operatorKey + " 在 " + context.getExpressionPath());
    }

    Operator operator = operatorOpt.get();

    // 处理 $constant 操作符
    if (operator.equals(Operators.CONSTANT)) {
      return context.getClauseContext().buildSExpression(
          Operators.CONSTANT,
          Collections.singletonList(SExpression.constant(value)),
          context.getExpressionPath()).get();
    }

    throw new NormalizeException("操作符 " + operatorKey + " 不支持在Map顶层使用");
  }
}
