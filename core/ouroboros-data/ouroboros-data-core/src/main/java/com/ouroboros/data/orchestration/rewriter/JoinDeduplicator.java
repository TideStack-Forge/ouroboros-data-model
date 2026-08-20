package com.ouroboros.data.orchestration.rewriter;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ouroboros.data.dsl.JoinType;
import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.dsl.statement.QueryStatement.JoinEntry;
import com.ouroboros.data.orchestration.OrchestrationContext;
/**
 * JOIN 去重优化器
 *
 * <p>职责：
 * <ul>
 *   <li>识别并合并重复的 JOIN（按表名分组）</li>
 *   <li>LEFT JOIN 优先原则</li>
 *   <li>替换 WHERE/SELECT 中的旧别名引用</li>
 *   <li>在 Context 中记录别名映射</li>
 * </ul>
 *
 * <p>不处理：
 * <ul>
 *   <li>子查询 JOIN 去重</li>
 *   <li>ORDER BY 中的别名替换</li>
 * </ul>
 *
 * @author Claude Code
 */
public class JoinDeduplicator implements StatementRewriter {
  private static final Logger logger = LoggerFactory.getLogger(JoinDeduplicator.class);
  @Override
  public QueryStatement rewrite(QueryStatement statement, OrchestrationContext context) {
    List<JoinEntry> joins = statement.getJoins();
    // 1. 无需去重
    if (joins.size() <= 1) {
      return statement;
    }
    // 2. 按 JOIN 语义分组（目标表 + 规范化 ON 条件，保持首次出现顺序）
    Map<JoinSignature, List<JoinEntry>> groupedJoins = new LinkedHashMap<>();
    for (JoinEntry join : joins) {
      if (!join.isSubQuery()) {
        groupedJoins
            .computeIfAbsent(buildJoinSignature(join), k -> new ArrayList<>())
            .add(join);
      }
    }
    // 3. 检查是否有重复
    boolean hasDuplicates = groupedJoins.values().stream()
        .anyMatch(list -> list.size() > 1);
    if (!hasDuplicates) {
      return statement;
    }
    // 4. 为每个表选择主 JOIN，记录别名映射
    Map<String, String> aliasMapping = new HashMap<>();
    Map<JoinSignature, JoinEntry> primaryJoins = new HashMap<>();
    for (Map.Entry<JoinSignature, List<JoinEntry>> entry : groupedJoins.entrySet()) {
      JoinSignature signature = entry.getKey();
      List<JoinEntry> group = entry.getValue();
      if (group.size() > 1) {
        JoinEntry primary = selectPrimaryJoin(group);
        primaryJoins.put(signature, primary);
        for (JoinEntry join : group) {
          if (join != primary) {
            aliasMapping.put(join.getAlias(), primary.getAlias());
          }
        }
      }
    }
    // 5. 构建去重后的 JOIN 列表（保持原始顺序）
    List<JoinEntry> deduplicatedJoins = new ArrayList<>();
    Set<JoinSignature> seenSignatures = new HashSet<>();
    for (JoinEntry join : joins) {
      if (join.isSubQuery()) {
        // 子查询 JOIN 直接保留
        deduplicatedJoins.add(join);
      } else {
        JoinSignature signature = buildJoinSignature(join);
        if (!seenSignatures.contains(signature)) {
          // 首次遇到该等价 JOIN
          seenSignatures.add(signature);
          JoinEntry primary = primaryJoins.get(signature);
          if (primary != null) {
            // 有重复，使用主 JOIN
            deduplicatedJoins.add(primary);
          } else {
            // 无重复，使用原 JOIN
            deduplicatedJoins.add(join);
          }
        }
        // 重复的 JOIN 跳过
      }
    }
    logger.debug("JOIN 去重: {} → {}, 别名映射: {}",
        joins.size(), deduplicatedJoins.size(), aliasMapping);
    // 6. 替换别名引用
    SExpression<Boolean> newWhere = replaceAliasInExpression(
        statement.getWhere(), aliasMapping);
    List<SExpression<?>> newSelect = replaceAliasInSelectList(
        statement.getSelect(), aliasMapping);
    // 7. 记录别名映射到 Context
    context.setAliasMapping(aliasMapping);
    // 8. 构建新的 QueryStatement
    return buildNewStatement(statement, deduplicatedJoins, newWhere, newSelect);
  }
  /**
   * 选择主 JOIN（LEFT JOIN 优先）
   */
  private JoinEntry selectPrimaryJoin(List<JoinEntry> joins) {
    for (JoinEntry join : joins) {
      if (join.getType() == JoinType.LEFTJOIN) {
        return join;
      }
    }
    return joins.get(0);
  }
  /**
   * 替换表达式中的别名引用
   */
  @SuppressWarnings("unchecked")
  private <T> SExpression<T> replaceAliasInExpression(
      SExpression<T> expr, Map<String, String> aliasMapping) {
    if (expr.isEmpty()) {
      return expr;
    }
    return (SExpression<T>) expr.transform((e, context) -> {
      if (e.getOperator() == Operators.FIELD) {
        for (Map.Entry<String, String> mapping : aliasMapping.entrySet()) {
          SExpression<?> replaced = replaceFieldAlias(e, mapping.getKey(), mapping.getValue());
          if (replaced != e) {
            return replaced;
          }
        }
      }
      return e;
    });
  }
  /**
   * 替换 SELECT 列表中的别名引用
   */
  private List<SExpression<?>> replaceAliasInSelectList(
      List<SExpression<?>> selectList, Map<String, String> aliasMapping) {
    if (selectList.isEmpty()) {
      return selectList;
    }
    return selectList.stream()
        .map(expr -> replaceAliasInExpression(expr, aliasMapping))
        .collect(Collectors.toList());
  }
  /**
   * 构建新的 QueryStatement
   */
  private QueryStatement buildNewStatement(
      QueryStatement original,
      List<JoinEntry> joins,
      SExpression<Boolean> where,
      List<SExpression<?>> select) {
    QueryStatement.QueryStatementBuilder builder = original.getBuilder();
    builder.replaceJoins(joins);
    builder.where(where);
    builder.replaceSelect(select);
    return builder.build();
  }
  private JoinSignature buildJoinSignature(JoinEntry join) {
    SExpression<Boolean> normalizedOn = normalizeOnCondition(join);
    return new JoinSignature(
        join.getTableName(),
        buildExpressionKey(normalizedOn));
  }
  private SExpression<Boolean> normalizeOnCondition(JoinEntry join) {
    SExpression<Boolean> on = join.getOn();
    if (on == null || on.isEmpty() || join.getAlias() == null || join.getAlias().isEmpty()) {
      return on;
    }
    @SuppressWarnings("unchecked")
    SExpression<Boolean> normalized = (SExpression<Boolean>) on.transform((expr, context) -> {
      if (expr.getOperator() != Operators.FIELD) {
        return expr;
      }
      return normalizeFieldReference(expr, join.getAlias());
    });
    return normalized;
  }
  private SExpression<?> replaceFieldAlias(SExpression<?> expr, String fromAlias, String toAlias) {
    if (expr.getOperator() != Operators.FIELD || fromAlias == null || toAlias == null) {
      return expr;
    }
    if (expr.getParams().isEmpty()) {
      return expr;
    }
    Object firstParam = expr.getParam(0);
    if (!(firstParam instanceof String firstSegment)) {
      return expr;
    }
    if (expr.getParams().size() == 1) {
      String oldPrefix = fromAlias + ".";
      if (!firstSegment.startsWith(oldPrefix)) {
        return expr;
      }
      String newFieldName = toAlias + "." + firstSegment.substring(oldPrefix.length());
      return SExpression.field(newFieldName);
    }
    if (!fromAlias.equals(firstSegment)) {
      return expr;
    }
    List<Object> newParams = new ArrayList<>(expr.getParams());
    newParams.set(0, toAlias);
    return SExpression.create(Operators.FIELD, newParams);
  }
  private SExpression<?> normalizeFieldReference(SExpression<?> expr, String joinAlias) {
    if (expr.getOperator() != Operators.FIELD || expr.getParams().isEmpty()) {
      return expr;
    }
    Object firstParam = expr.getParam(0);
    if (!(firstParam instanceof String firstSegment)) {
      return expr;
    }
    if (expr.getParams().size() == 1) {
      String joinPrefix = joinAlias + ".";
      if (firstSegment.startsWith(joinPrefix)) {
        return SExpression.field(buildCanonicalSegments("$JOIN",
            firstSegment.substring(joinPrefix.length())));
      }
      String leaf = firstSegment.contains(".")
          ? firstSegment.substring(firstSegment.lastIndexOf('.') + 1)
          : firstSegment;
      return SExpression.field("$SOURCE", leaf);
    }
    List<Object> newParams = new ArrayList<>(expr.getParams());
    if (joinAlias.equals(firstSegment)) {
      newParams.set(0, "$JOIN");
      return SExpression.create(Operators.FIELD, newParams);
    }
    newParams.set(0, "$SOURCE");
    return SExpression.create(Operators.FIELD, newParams);
  }
  private String buildExpressionKey(SExpression<?> expr) {
    if (expr == null || expr.isEmpty()) {
      return "";
    }
    StringBuilder builder = new StringBuilder();
    builder.append(expr.getOperator());
    builder.append('(');
    for (int i = 0; i < expr.getParams().size(); i++) {
      if (i > 0) {
        builder.append(',');
      }
      Object param = expr.getParam(i);
      if (param instanceof SExpression<?> sExpression) {
        builder.append(buildExpressionKey(sExpression));
      } else {
        builder.append(String.valueOf(param));
      }
    }
    builder.append(')');
    return builder.toString();
  }
  private String[] buildCanonicalSegments(String prefix, String suffixPath) {
    if (suffixPath == null || suffixPath.isEmpty()) {
      return new String[]{prefix};
    }
    String[] suffixSegments = suffixPath.split("\\.");
    String[] result = new String[suffixSegments.length + 1];
    result[0] = prefix;
    System.arraycopy(suffixSegments, 0, result, 1, suffixSegments.length);
    return result;
  }
  private record JoinSignature(String tableName, String normalizedOnKey) {
  }
}
