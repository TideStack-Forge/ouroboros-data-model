package com.ouroboros.data.transpile.clausebuilder;

import static com.ouroboros.data.dsl.JoinType.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;

import com.ouroboros.data.dsl.ExtOps;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.transpile.*;
import com.ouroboros.data.exception.TranspileException;
import com.querydsl.core.types.Operator;
import com.querydsl.core.JoinType;
import com.querydsl.core.types.SubQueryExpressionImpl;
import com.querydsl.core.types.dsl.Expressions;

/**
 * JOIN 子句构建器
 *
 * <p>职责：
 * <ul>
 *   <li>处理 JOIN 子句</li>
 *   <li>包装 JoinTranspileContext（如果有 JOIN）</li>
 * </ul>
 */
public class JoinBuilder implements ClauseTranspiler {
  private static final Logger logger = LoggerFactory.getLogger(JoinBuilder.class);

  // ========== 新 API（使用 TranspileContext） ==========

  @Override
  public Try<Tuple2<OuroborosQueryMetadata, TranspileContext>> applyWithContext(
      QueryStatement query,
      OuroborosQueryMetadata queryMetadata,
      TranspileContext context) {

    var joins = query.getJoins();
    if (joins.isEmpty()) {
      // 无 JOIN，直接返回原 context
      return Try.success(Tuple.of(queryMetadata, context));
    }

    // 处理 JOIN，收集 joinTables
    Map<String, FieldSource> joinTables = new HashMap<>();

    for (var join : joins) {
      try {
        String alias = join.getAlias();
        String joinKey = alias;
        FieldSource fieldSource;

        if (join.isSubQuery()) {
          // 子查询 JOIN：转译子查询并添加到 queryMetadata
          SubqueryTranspileContext joinSubCtx = new SubqueryTranspileContext(context);
          var subQueryResult = context.getQueryTranspiler()
              .applyWithContext(join.getSubQuery(), joinSubCtx);
          if (subQueryResult.isFailure()) {
            return Try.failure(subQueryResult.getCause());
          }
          var subQueryExpr = new SubQueryExpressionImpl<>(Object.class, subQueryResult.get());
          queryMetadata.addJoin(toQuerydslJoinType(join.getType()), Expressions.as(subQueryExpr, alias));

          // 构建 SubqueryFieldSource，使外层可解析子查询投影列
          fieldSource = new SubqueryFieldSource(alias, subQueryResult.get());
        } else {
          // 普通表 JOIN
          String tableName = join.getTableName();
          FieldSource rawFieldSource = getFieldSource(tableName, context)
              .getOrElseThrow(cause -> new TranspileException("Join表不存在: " + tableName, cause));
          joinKey = alias == null || alias.isEmpty() ? tableName : alias;
          fieldSource = alias == null || alias.isEmpty()
              ? rawFieldSource
              : new AliasedFieldSource(rawFieldSource, joinKey);

          // 添加到 QueryMetadata
          var path = rawFieldSource.getSelfPath();
          var joinExpr = alias == null || alias.isEmpty()
              ? path
              : Expressions.as(path, joinKey);
          queryMetadata.addJoin(toQuerydslJoinType(join.getType()), joinExpr);
        }

        // 处理 ON 条件
        if (!join.getOn().isEmpty()) {
          TranspileContext existingContext = joinTables.isEmpty()
              ? context
              : new JoinTranspileContext(context, joinTables);
          SExpression<Boolean> qualifiedOn = qualifyOnCondition(join.getOn(), existingContext, joinKey, fieldSource);

          // 创建临时 Context 用于解析 ON 条件
          Map<String, FieldSource> tempJoinTables = new HashMap<>(joinTables);
          tempJoinTables.put(joinKey, fieldSource);
          TranspileContext tempContext = new JoinTranspileContext(context, tempJoinTables);

          var onPredicate = SExpressionTranspiler.transpilePredicate(qualifiedOn, tempContext);
          if (onPredicate.isFailure()) {
            return Try.failure(onPredicate.getCause());
          }
          queryMetadata.addJoinCondition(onPredicate.get());
        }

        // 收集到 joinTables
        joinTables.put(joinKey, fieldSource);

      } catch (Exception e) {
        logger.error("处理 JOIN 时发生错误: {}", join.getAlias(), e);
        return Try.failure(new TranspileException("处理 JOIN 失败: " + join.getAlias() + " - " + e.getMessage(), e));
      }
    }

    // 包装 JoinTranspileContext
    if (joinTables.isEmpty()) {
      return Try.success(Tuple.of(queryMetadata, context));
    }

    TranspileContext newContext = new JoinTranspileContext(context, joinTables);
    return Try.success(Tuple.of(queryMetadata, newContext));
  }

  /**
   * 获取 FieldSource
   *
   * @param tableName 表名
   * @param context   Context
   * @return FieldSource
   */
  private Try<FieldSource> getFieldSource(String tableName, TranspileContext context) {
    // 从 context 解析
    return context.resolveTable(tableName)
        .map(Try::success)
        .orElseGet(() -> Try.failure(new TranspileException("Join表不存在: " + tableName)));
  }

  private JoinType toQuerydslJoinType(com.ouroboros.data.dsl.JoinType joinType) {
    if (joinType == null) {
      return JoinType.DEFAULT;
    }
    switch (joinType) {
      case INNERJOIN:
        return JoinType.INNERJOIN;
      case LEFTJOIN:
        return JoinType.LEFTJOIN;
      case RIGHTJOIN:
        return JoinType.RIGHTJOIN;
      case FULLJOIN:
        return JoinType.FULLJOIN;
      case DEFAULT:
      default:
        return JoinType.DEFAULT;
    }
  }

  @SuppressWarnings("unchecked")
  private SExpression<Boolean> qualifyOnCondition(
      SExpression<Boolean> onCondition,
      TranspileContext existingContext,
      String currentJoinName,
      FieldSource currentJoinSource) {
    return (SExpression<Boolean>) rewriteOnExpression(onCondition, existingContext, currentJoinName, currentJoinSource);
  }

  private SExpression<?> rewriteOnExpression(
      SExpression<?> expr,
      TranspileContext existingContext,
      String currentJoinName,
      FieldSource currentJoinSource) {
    if (expr == null || expr.isEmpty()) {
      return expr;
    }

    List<Object> rewrittenParams = new ArrayList<>();
    boolean changed = false;

    for (int i = 0; i < expr.getParams().size(); i++) {
      Object param = expr.getParam(i);
      Object rewrittenParam = param;
      if (param instanceof SExpression<?> childExpr) {
        rewrittenParam = rewriteOnChild(expr, childExpr, i, existingContext, currentJoinName, currentJoinSource);
      }
      rewrittenParams.add(rewrittenParam);
      if (rewrittenParam != param) {
        changed = true;
      }
    }

    return changed ? SExpression.create(expr.getOperator(), rewrittenParams) : expr;
  }

  private SExpression<?> rewriteOnChild(
      SExpression<?> parent,
      SExpression<?> childExpr,
      int childIndex,
      TranspileContext existingContext,
      String currentJoinName,
      FieldSource currentJoinSource) {
    if (!isBareField(childExpr)) {
      return rewriteOnExpression(childExpr, existingContext, currentJoinName, currentJoinSource);
    }

    String fieldName = String.valueOf(childExpr.getParam(0));
    boolean currentJoinHasField = currentJoinSource.getField(fieldName).isPresent();
    if (!currentJoinHasField) {
      return childExpr;
    }

    boolean existingContextHasField = canResolve(existingContext, fieldName);
    Optional<SExpression<?>> siblingField = getSiblingField(parent, childIndex);
    if (siblingField.isPresent()) {
      if (childIndex == 1) {
        return qualifyField(fieldName, currentJoinName);
      }
      if (!existingContextHasField) {
        return qualifyField(fieldName, currentJoinName);
      }
      return childExpr;
    }

    return qualifyField(fieldName, currentJoinName);
  }

  private Optional<SExpression<?>> getSiblingField(SExpression<?> parent, int childIndex) {
    if (parent.getParams().size() != 2) {
      return Optional.empty();
    }
    int siblingIndex = childIndex == 0 ? 1 : 0;
    Object sibling = parent.getParam(siblingIndex);
    if (!(sibling instanceof SExpression<?> siblingExpr) || !isBareField(siblingExpr)) {
      return Optional.empty();
    }
    return Optional.of(siblingExpr);
  }

  private boolean canResolve(TranspileContext context, String fieldName) {
    try {
      return context.resolve(fieldName).isPresent();
    } catch (AmbiguousFieldException exception) {
      return true;
    }
  }

  private boolean isBareField(SExpression<?> expr) {
    if (expr == null || expr.isEmpty()) {
      return false;
    }

    Operator operator = expr.getOperator();
    if (operator != Operators.FIELD && operator != ExtOps.FIELD) {
      return false;
    }
    if (expr.getParams().size() != 1) {
      return false;
    }
    Object param = expr.getParam(0);
    return param instanceof String && !String.valueOf(param).contains(".");
  }

  private SExpression<?> qualifyField(String fieldName, String currentJoinName) {
    return SExpression.field(currentJoinName, fieldName);
  }
}
