package com.ouroboros.data.transpile.clausebuilder;

import java.util.Optional;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;

import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.transpile.*;
import com.ouroboros.data.exception.TranspileException;
import com.querydsl.core.JoinType;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.SubQueryExpressionImpl;
import com.querydsl.core.types.dsl.Expressions;

/**
 * FROM 子句构建器
 *
 * <p>职责：
 * <ul>
 *   <li>处理 FROM 子句</li>
 *   <li>创建 BaseTranspileContext（Context 链的起点）</li>
 * </ul>
 */
public class FromBuilder implements ClauseTranspiler {

  @Override
  public Try<Tuple2<OuroborosQueryMetadata, TranspileContext>> applyWithContext(
      QueryStatement query,
      OuroborosQueryMetadata queryMetadata,
      TranspileContext context) {

    var from = query.getFrom();
    if (from == null) {
      return Try.failure(new TranspileException("From子句不能为空"));
    }

    var joins = query.getJoins();
    var alias = from.getAlias();

    if (!from.isSubQuery()) {
      // 普通表
      var tableName = from.getTableName();

      // 从 context 获取 FieldSource
      return getFieldSource(tableName, context)
          .map(fieldSource -> {
            var path = fieldSource.getSelfPath();
            boolean useAlias = !joins.isEmpty()
                || (alias != null && !alias.isEmpty() && !tableName.equalsIgnoreCase(alias));
            var fromExpr = useAlias ? Expressions.as(path, alias) : path;
            queryMetadata.addJoin(JoinType.DEFAULT, fromExpr);

            // 创建 BaseTranspileContext
            String effectiveAlias = (alias == null || alias.isEmpty()) ? tableName : alias;
            FieldSource contextFieldSource = useAlias
                ? new AliasedFieldSource(fieldSource, effectiveAlias)
                : fieldSource;
            TranspileContext newContext = new BaseTranspileContext(contextFieldSource, effectiveAlias,
                context.getQueryTranspiler(), context);

            return Tuple.of(queryMetadata, newContext);
          });
    }

    // 子查询
    SubqueryTranspileContext subCtx = new SubqueryTranspileContext(context);
    var tryTranspileSubQuery = context.getQueryTranspiler().applyWithContext(from.getSubQuery(), subCtx);
    return tryTranspileSubQuery
        .map(subQueryMetadata -> {
          var subQueryExpr = new SubQueryExpressionImpl<>(Object.class, subQueryMetadata);
          queryMetadata.addJoin(JoinType.DEFAULT, Expressions.as(subQueryExpr, alias));

          // 构建 SubqueryFieldSource，使外层可解析子查询投影列
          var fieldSource = new SubqueryFieldSource(alias, subQueryMetadata);
          TranspileContext newContext = new BaseTranspileContext(fieldSource, alias,
              context.getQueryTranspiler(), context);
          return Tuple.of(queryMetadata, newContext);
        });
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
        .orElseGet(() -> Try.failure(new TranspileException("From子句中的表名不存在: " + tableName)));
  }
}
