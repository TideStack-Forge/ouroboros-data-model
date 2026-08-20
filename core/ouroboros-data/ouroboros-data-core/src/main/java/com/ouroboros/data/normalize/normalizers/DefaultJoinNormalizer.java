package com.ouroboros.data.normalize.normalizers;

import static com.ouroboros.data.dsl.StatementPredicates.isStringKeyMap;

import java.util.*;

import com.ouroboros.data.dsl.JoinType;
import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.exception.NormalizeException;
import com.ouroboros.data.normalize.ClauseNormalizeContext;
import com.ouroboros.data.normalize.ClauseNormalizer;
import com.ouroboros.data.util.NormalizeUtils;
import com.ouroboros.data.util.DataMaps;

/**
 * JOIN子句规范化器（上下文感知版本）
 * <p>
 * 支持：INNER JOIN, LEFT JOIN, RIGHT JOIN, FULL JOIN, CROSS JOIN
 */
public class DefaultJoinNormalizer implements ClauseNormalizer {

  @Override
  public boolean supports(String clauseType) {
    return "QUERY".equalsIgnoreCase(clauseType) || "JOIN".equalsIgnoreCase(clauseType);
  }

  @Override
  public QueryStatement.QueryStatementBuilder normalize(Map<String, ?> clauseData,
                                                        QueryStatement.QueryStatementBuilder builder,
                                                        ClauseNormalizeContext context) {
    List<QueryStatement.JoinEntry> allJoinEntries = new ArrayList<>();

    // 按不同的JOIN类型提取
    allJoinEntries.addAll(getJoinEntryList(JoinType.INNERJOIN, Keyword.JOIN.findIn(clauseData), context));
    allJoinEntries.addAll(getJoinEntryList(JoinType.INNERJOIN, Keyword.INNER_JOIN.findIn(clauseData), context));
    allJoinEntries.addAll(getJoinEntryList(JoinType.LEFTJOIN, Keyword.LEFT_JOIN.findIn(clauseData), context));
    allJoinEntries.addAll(getJoinEntryList(JoinType.RIGHTJOIN, Keyword.RIGHT_JOIN.findIn(clauseData), context));
    allJoinEntries.addAll(getJoinEntryList(JoinType.FULLJOIN, Keyword.FULL_JOIN.findIn(clauseData), context));
    allJoinEntries.addAll(getJoinEntryList(JoinType.DEFAULT, Keyword.CROSS_JOIN.findIn(clauseData), context));

    builder.join(allJoinEntries);
    return builder;
  }

  /**
   * 获取JOIN条目列表
   */
  @SuppressWarnings("unchecked")
  protected List<QueryStatement.JoinEntry> getJoinEntryList(JoinType type, Object joinRaw, ClauseNormalizeContext context) {
    if (joinRaw == null) {
      return Collections.emptyList();
    }

    List<QueryStatement.JoinEntry> joinEntries = new ArrayList<>();

    if (joinRaw instanceof QueryStatement.JoinEntry joinEntry) {
      joinEntries.add(joinEntry);
    } else if (joinRaw instanceof Map<?, ?> joinMap && isStringKeyMap.test(joinMap)) {
      // 单个JOIN：{tableName: {on: {...}}}
      QueryStatement.JoinEntry entry = buildJoinEntry(type, (Map<String, ?>) joinMap, context);
      joinEntries.add(entry);
    } else if (joinRaw instanceof Collection<?> joinList) {
      // 多个JOIN：[{tableName: {on: {...}}}, ...]
      for (Object item : joinList) {
        if (item instanceof Map<?, ?> entryMap && isStringKeyMap.test(entryMap)) {
          QueryStatement.JoinEntry entry = buildJoinEntry(type, (Map<String, ?>) entryMap, context);
          joinEntries.add(entry);
        } else if (item instanceof QueryStatement.JoinEntry joinEntry) {
          joinEntries.add(joinEntry);
        } else {
          throw new NormalizeException("JOIN 子句格式错误，不支持的类型: " + item.getClass().getName());
        }
      }
    } else {
      throw new NormalizeException("JOIN 子句格式错误，不支持的类型: " + joinRaw.getClass().getName());
    }

    // 除了交叉连接外，其他JOIN必须有连接条件
    for (QueryStatement.JoinEntry entry : joinEntries) {
      if (entry.getType() != JoinType.DEFAULT && entry.getOn().isEmpty()) {
        throw new NormalizeException(entry.getType() + " 必须指定连接条件（ON子句）");
      }
    }

    return joinEntries;
  }

  /**
   * 构建单个JOIN条目
   */
  private QueryStatement.JoinEntry buildJoinEntry(JoinType type, Map<String, ?> joinMap, ClauseNormalizeContext context) {
    // 转换为不区分大小写的Map
    TreeMap<String, Object> joinStrKeyMap = new TreeMap<>(String::compareToIgnoreCase);
    joinStrKeyMap.putAll(joinMap);

    // 提取ON条件
    Object joinOn = joinStrKeyMap.get("on");

    // 提取表信息（排除on字段）
    Map<String, ?> joinTarget = DataMaps.omit(joinMap, key -> "on".equalsIgnoreCase(key));

    // 构建表源
    QueryStatement.TableSource tableSource = NormalizeUtils.buildTableSource(joinTarget, context).get();

    // 构建JOIN条件
    SExpression<Boolean> joinCondition = buildJoinCondition(joinOn, context);

    return new QueryStatement.JoinEntry(type, tableSource, joinCondition);
  }

  /**
   * 构建JOIN条件
   */
  @SuppressWarnings("unchecked")
  private SExpression<Boolean> buildJoinCondition(Object joinOn, ClauseNormalizeContext context) {
    if (joinOn == null) {
      // 没有条件，返回空表达式（交叉连接允许）
      return SExpression.empty(Boolean.class);
    }

    // 如果已经是SExpression
    if (joinOn instanceof SExpression<?> joinOnExpr) {
      if (!joinOnExpr.getDataType().isAssignableFrom(Boolean.class)) {
        throw new NormalizeException("JOIN条件必须是Boolean类型，但得到: " + joinOnExpr.getDataType().getName());
      }
      return (SExpression<Boolean>) joinOnExpr;
    }

    // 如果是Map：{leftField: rightField, ...}
    if (joinOn instanceof Map<?, ?> joinOnMap) {
      return buildJoinConditionFromMap(joinOnMap);
    }

    // 如果是List：[{leftField: rightField}, ...]
    if (joinOn instanceof List<?> joinOnList) {
      List<SExpression<Boolean>> conditions = new ArrayList<>();
      for (Object item : joinOnList) {
        if (item instanceof Map<?, ?> itemMap) {
          conditions.add(buildJoinConditionFromMap(itemMap));
        }
      }

      if (conditions.isEmpty()) {
        return SExpression.empty(Boolean.class);
      } else if (conditions.size() == 1) {
        return conditions.get(0);
      } else {
        // 多个条件用AND组合
        return SExpression.create(Operators.AND, conditions);
      }
    }

    throw new NormalizeException("JOIN条件格式不支持: " + joinOn.getClass().getName());
  }

  /**
   * 从Map构建JOIN条件
   * Map格式：{leftField: rightField, ...}
   */
  @SuppressWarnings("unchecked")
  private SExpression<Boolean> buildJoinConditionFromMap(Map<?, ?> conditionMap) {
    // 检查所有key和value都是字符串
    for (Map.Entry<?, ?> entry : conditionMap.entrySet()) {
      if (!(entry.getKey() instanceof String) || !(entry.getValue() instanceof String)) {
        throw new NormalizeException("JOIN条件的key和value必须都是字符串（字段名）");
      }
    }

    List<SExpression<Boolean>> conditions = new ArrayList<>();
    for (Map.Entry<?, ?> entry : conditionMap.entrySet()) {
      String leftField = (String) entry.getKey();
      String rightField = (String) entry.getValue();

      // 创建字段表达式
      SExpression<?> leftFieldExpr = SExpression.create(Operators.FIELD, leftField);
      SExpression<?> rightFieldExpr = SExpression.create(Operators.FIELD, rightField);

      // 创建等于条件
      SExpression<Boolean> eqCondition = SExpression.create(Operators.EQ, leftFieldExpr, rightFieldExpr);
      conditions.add(eqCondition);
    }

    if (conditions.isEmpty()) {
      return SExpression.empty(Boolean.class);
    } else if (conditions.size() == 1) {
      return conditions.get(0);
    } else {
      // 多个条件用AND组合
      return SExpression.create(Operators.AND, conditions);
    }
  }
}
