package com.ouroboros.data.orchestration.rewriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ouroboros.data.dsl.ExtOps;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.querydsl.core.types.Operator;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.orchestration.OrchestrationContext;
import com.ouroboros.data.orchestration.OrchestrationException;
import com.ouroboros.data.record.Record;
import com.ouroboros.data.record.RecordList;
/**
 * 跨源条件改写器
 *
 * <p>职责：
 * <ul>
 *   <li>将跨源关联条件改写为本地字段的 IN/EQ 条件</li>
 *   <li>根据预查询结果生成合适的条件</li>
 * </ul>
 *
 * <p>改写策略：
 * <ul>
 *   <li>预查询结果为空 → 1 = 0</li>
 *   <li>预查询结果为单个值 → field = value</li>
 *   <li>预查询结果为多个值且 ≤ maxInListSize → field IN (...)</li>
 *   <li>预查询结果为多个值且 > maxInListSize → UNION ALL 分批</li>
 * </ul>
 *
 * @author Claude Code
 */
public record CrossSourceConditionRewriter(String preQueryStepName,
                                           String localFieldPath,
                                           String relationFieldPath,
                                           String referenceKeyName,
                                           int maxInListSize) implements StatementRewriter {
  private static final Logger logger = LoggerFactory.getLogger(CrossSourceConditionRewriter.class);
  @Override
  public QueryStatement rewrite(QueryStatement statement, OrchestrationContext context) {
    logger.debug("Rewriting cross-source condition for field: {}", localFieldPath);
    // 从 Context 获取预查询结果
    RecordList preQueryResult = context.getResult(preQueryStepName);
    if (preQueryResult == null) {
      throw new OrchestrationException("Pre-query result not found: " + preQueryStepName);
    }
    // 提取 ID 列表
    List<Object> ids = extractIds(preQueryResult);
    logger.debug("Extracted {} IDs from pre-query result", ids.size());
    // 先移除原始 REL_* 条件
    statement = removeRelationCondition(statement);
    // 根据结果数量选择改写策略
    if (ids.isEmpty()) {
      // 结果为空 → 1 = 0
      logger.debug("Empty result, rewriting to FALSE condition");
      return rewriteToFalse(statement);
    } else if (ids.size() == 1) {
      // 单个值 → field = value
      logger.debug("Single value, rewriting to EQUALS condition");
      return rewriteToEquals(statement, ids.get(0));
    } else if (ids.size() <= maxInListSize) {
      // 多个值且未超阈值 → field IN (...)
      logger.debug("Multiple values ({}), rewriting to IN condition", ids.size());
      return rewriteToIn(statement, ids);
    } else {
      // 多个值且超阈值 → UNION ALL 分批
      logger.debug("Large result set ({}), rewriting to batched UNION ALL (maxInListSize={})", ids.size(), maxInListSize);
      return rewriteToBatchedUnion(statement, ids);
    }
  }
  /**
   * 从预查询结果中提取 ID 列表
   */
  private List<Object> extractIds(RecordList result) {
    List<Object> ids = new ArrayList<>();
    for (Record record : result) {
      Object id = record.get(referenceKeyName);
      if (id != null) {
        ids.add(id);
      }
    }
    return ids;
  }
  /**
   * 改写为 FALSE 条件（1 = 0）
   * <p>
   * Round 3 实现：追加 AND 1 = 0 条件
   */
  private QueryStatement rewriteToFalse(QueryStatement statement) {
    SExpression<Boolean> falseCondition = SExpression.create(
        Operators.EQ,
        SExpression.constant(1),
        SExpression.constant(0)
    );
    return appendWhereCondition(statement, falseCondition);
  }
  /**
   * 改写为 EQUALS 条件（field = value）
   * <p>
   * Round 3 实现：追加 AND localField = value 条件
   */
  private QueryStatement rewriteToEquals(QueryStatement statement, Object value) {
    SExpression<?> fieldExpr = buildFieldExpression(localFieldPath);
    SExpression<Boolean> eqCondition = SExpression.create(
        Operators.EQ,
        fieldExpr,
        SExpression.constant(value)
    );
    return appendWhereCondition(statement, eqCondition);
  }
  /**
   * 改写为 IN 条件（field IN (...)）
   * <p>
   * Round 3 实现：追加 AND localField IN (...) 条件
   */
  private QueryStatement rewriteToIn(QueryStatement statement, List<Object> values) {
    SExpression<?> fieldExpr = buildFieldExpression(localFieldPath);
    List<Object> params = new ArrayList<>();
    params.add(fieldExpr);
    for (Object value : values) {
      params.add(SExpression.constant(value));
    }
    SExpression<Boolean> inCondition = SExpression.create(
        Operators.IN,
        params.toArray()
    );
    return appendWhereCondition(statement, inCondition);
  }
  /**
   * 改写为分批 UNION ALL（ids 超过 maxInListSize 时）
   * <p>
   * 将 ID 列表按 maxInListSize 拆分为多个批次，每批生成 IN 条件，通过 UNION ALL 合并。
   */
  private QueryStatement rewriteToBatchedUnion(QueryStatement statement, List<Object> ids) {
    // 第一个批次
    List<Object> firstBatch = ids.subList(0, maxInListSize);
    QueryStatement result = rewriteToIn(statement, firstBatch);
    // 后续批次通过 UNION ALL 追加
    for (int i = maxInListSize; i < ids.size(); i += maxInListSize) {
      int end = Math.min(i + maxInListSize, ids.size());
      List<Object> batch = ids.subList(i, end);
      QueryStatement batchQuery = rewriteToIn(statement, batch);
      result = result.getBuilder().unionAll(batchQuery).build();
    }
    return result;
  }
  /**
   * 将新条件 AND 到现有 WHERE 子句
   * <p>
   * Round 3 新增辅助方法
   */
  private QueryStatement appendWhereCondition(QueryStatement statement, SExpression<Boolean> newCondition) {
    SExpression<Boolean> existingWhere = statement.getWhere();
    SExpression<Boolean> combinedWhere;
    if (existingWhere.isEmpty()) {
      combinedWhere = newCondition;
    } else {
      combinedWhere = SExpression.create(Operators.AND, existingWhere, newCondition);
    }
    return statement.getBuilder()
        .where(combinedWhere)
        .build();
  }
  /**
   * 移除 WHERE 中匹配的 REL_* 关联条件
   * <p>
   * 使用 SExpression.transform() 将匹配的 REL_ANY/REL_ALL/REL_NONE 替换为 1 = 1（TRUE）
   */
  @SuppressWarnings("unchecked")
  private QueryStatement removeRelationCondition(QueryStatement statement) {
    SExpression<Boolean> where = statement.getWhere();
    if (where.isEmpty()) {
      return statement;
    }
    SExpression<?> transformed = where.transform((expr, context) -> {
      if (isMatchingRelationCondition(expr)) {
        return SExpression.create(Operators.EQ,
            SExpression.constant(1), SExpression.constant(1));
      }
      return expr;
    });
    return statement.getBuilder()
        .where((SExpression<Boolean>) transformed)
        .build();
  }
  private SExpression<?> buildFieldExpression(String fieldPath) {
    return SExpression.field(fieldPath.split("\\."));
  }
  /**
   * 判断表达式是否为当前字段的关联条件
   */
  private boolean isMatchingRelationCondition(SExpression<?> expr) {
    Operator op = expr.getOperator();
    if (op == ExtOps.REL_ANY || op == ExtOps.REL_ALL || op == ExtOps.REL_NONE) {
      SExpression<?> fieldExpr = expr.getParamAsSExpression(0);
      if (fieldExpr.getOperator() != Operators.FIELD) {
        return false;
      }
      return relationFieldPath.equals(String.join(".",
          fieldExpr.getParams().stream().map(String::valueOf).toArray(String[]::new)));
    }
    return false;
  }
}
