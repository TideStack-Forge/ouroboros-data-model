package com.ouroboros.data.orchestration.rewriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ouroboros.data.dsl.ExtOps;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.orchestration.OrchestrationContext;
import com.ouroboros.data.orchestration.OrchestrationException;
import com.ouroboros.data.record.Record;
import com.ouroboros.data.record.RecordList;
import com.querydsl.core.types.Operator;
/**
 * 裸 FIELD toOne 跨源条件改写器。
 *
 * <p>不依赖 REL_* 包装器，而是直接在原布尔表达式树中定位“仅由指定 relation path 组成”的子表达式，
 * 并将其替换为本地外键条件，保持原有布尔结构。
 */
public record BareFieldCrossSourceConditionRewriter(String preQueryStepName,
                                                    String localFieldPath,
                                                    String relationFieldPath,
                                                    String referenceKeyName,
                                                    int maxInListSize) implements StatementRewriter {
  private static final Logger logger = LoggerFactory.getLogger(BareFieldCrossSourceConditionRewriter.class);
  @Override
  public QueryStatement rewrite(QueryStatement statement, OrchestrationContext context) {
    RecordList preQueryResult = context.getResult(preQueryStepName);
    if (preQueryResult == null) {
      throw new OrchestrationException("Pre-query result not found: " + preQueryStepName);
    }
    List<Object> ids = extractIds(preQueryResult);
    SExpression<Boolean> replacement = buildReplacementCondition(ids);
    AtomicBoolean replaced = new AtomicBoolean(false);
    SExpression<Boolean> rewrittenWhere = replaceMatchingExpression(statement.getWhere(), replacement, replaced);
    if (!replaced.get()) {
      throw new OrchestrationException("Bare field relation condition not found: " + relationFieldPath);
    }
    return statement.getBuilder()
        .where(rewrittenWhere)
        .build();
  }
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
  private SExpression<Boolean> buildReplacementCondition(List<Object> ids) {
    if (ids.isEmpty()) {
      return SExpression.create(
          Operators.EQ,
          SExpression.constant(1),
          SExpression.constant(0)
      );
    }
    SExpression<?> localFieldExpr = buildFieldExpression(localFieldPath);
    if (ids.size() == 1) {
      return SExpression.create(
          Operators.EQ,
          localFieldExpr,
          SExpression.constant(ids.get(0))
      );
    }
    if (ids.size() <= maxInListSize) {
      List<Object> params = new ArrayList<>();
      params.add(localFieldExpr);
      ids.forEach(id -> params.add(SExpression.constant(id)));
      return SExpression.create(Operators.IN, params.toArray());
    }
    List<Object> firstBatch = ids.subList(0, maxInListSize);
    List<Object> params = new ArrayList<>();
    params.add(localFieldExpr);
    firstBatch.forEach(id -> params.add(SExpression.constant(id)));
    return SExpression.create(Operators.IN, params.toArray());
  }
  @SuppressWarnings("unchecked")
  private SExpression<Boolean> replaceMatchingExpression(
      SExpression<Boolean> where, SExpression<Boolean> replacement, AtomicBoolean replaced) {
    if (where == null || where.isEmpty()) {
      return where;
    }
    SExpression<?> transformed = where.transform((expr, context) -> {
      if (!replaced.get() && matchesImplicitRelationCondition(expr)) {
        replaced.set(true);
        return replacement;
      }
      return expr;
    });
    return (SExpression<Boolean>) transformed;
  }
  private boolean matchesImplicitRelationCondition(SExpression<?> expr) {
    if (expr == null || expr.isEmpty() || expr.getDataType() != Boolean.class || isRelationOperator(expr.getOperator())) {
      return false;
    }
    MatchState state = new MatchState();
    collectMatchingFields(expr, state);
    return state.matched && !state.unsupported;
  }
  private void collectMatchingFields(SExpression<?> expr, MatchState state) {
    if (expr == null || expr.isEmpty() || state.unsupported) {
      return;
    }
    if (isRelationOperator(expr.getOperator())) {
      state.unsupported = true;
      return;
    }
    if (expr.getOperator() == Operators.FIELD) {
      List<String> segments = extractFieldSegments(expr);
      List<String> relationSegments = Arrays.asList(relationFieldPath.split("\\."));
      if (segments.size() <= relationSegments.size() || !matchesPrefix(segments, relationSegments)) {
        state.unsupported = true;
        return;
      }
      state.matched = true;
      return;
    }
    for (Object param : expr.getParams()) {
      if (param instanceof SExpression<?> childExpr) {
        collectMatchingFields(childExpr, state);
      }
    }
  }
  private boolean matchesPrefix(List<String> fieldSegments, List<String> relationSegments) {
    for (int index = 0; index < relationSegments.size(); index++) {
      if (!relationSegments.get(index).equals(fieldSegments.get(index))) {
        return false;
      }
    }
    return true;
  }
  private List<String> extractFieldSegments(SExpression<?> fieldExpr) {
    List<String> result = new ArrayList<>();
    fieldExpr.getParams().forEach(param -> {
      if (param instanceof String) {
        result.addAll(Arrays.asList(((String) param).split("\\.")));
      }
    });
    return result;
  }
  private boolean isRelationOperator(Operator operator) {
    return operator == ExtOps.REL_ANY || operator == ExtOps.REL_ALL || operator == ExtOps.REL_NONE;
  }
  private SExpression<?> buildFieldExpression(String fieldPath) {
    return SExpression.field(fieldPath.split("\\."));
  }
  private static class MatchState {
    private boolean matched;
    private boolean unsupported;
  }
}
