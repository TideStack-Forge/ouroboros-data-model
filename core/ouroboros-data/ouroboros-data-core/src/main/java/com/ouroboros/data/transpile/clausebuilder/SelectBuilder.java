package com.ouroboros.data.transpile.clausebuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;

import com.ouroboros.data.dsl.ExtOps;
import com.ouroboros.data.dsl.ModelFieldPath;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.query.DefaultProjectionFieldSupport;
import com.ouroboros.data.transpile.ClauseTranspiler;
import com.ouroboros.data.transpile.ColumnAliasTranspileContext;
import com.ouroboros.data.transpile.FieldSource;
import com.ouroboros.data.transpile.OuroborosQueryMetadata;
import com.ouroboros.data.transpile.TranspileContext;
import com.querydsl.core.types.*;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.sql.SQLExpressions;

public class SelectBuilder implements ClauseTranspiler {
  @Override
  public Try<Tuple2<OuroborosQueryMetadata, TranspileContext>> applyWithContext(QueryStatement query, OuroborosQueryMetadata queryMetadata, TranspileContext context) {
    var selectExprList = getSelectExpressions(query, context)
        .map(selectExpr -> context.transpile(selectExpr)
            .map(expr -> applyImplicitAlias(selectExpr, expr)))
        .collect(Collectors.toList());

    if (selectExprList.isEmpty()) {
      queryMetadata.setProjection(SQLExpressions.all);
      return Try.success(Tuple.of(queryMetadata, context));
    }

    // TODO: 检查重名字段
    return Try.<Expression<?>>sequence(selectExprList)
        .map(columns -> {
          // 提取列别名
          Map<String, Path<?>> columnAliases = new HashMap<>();
          for (Expression<?> expr : columns) {
            if (expr instanceof Operation<?>) {
              Operation<?> op = (Operation<?>) expr;
              if (op.getOperator() == Ops.ALIAS) {
                // ALIAS 操作符：第一个参数是值，第二个参数是别名
                String alias = op.getArg(1).toString();
                Expression<?> value = op.getArg(0);
                if (value instanceof Path<?>) {
                  columnAliases.put(alias, (Path<?>) value);
                } else {
                  columnAliases.put(alias, Expressions.path((Class<?>) value.getType(), alias));
                }
              }
            }
          }

          var flattenColumns = columns.toJavaStream()
              .flatMap(group -> group instanceof FactoryExpression<?> fac ? fac.getArgs().stream() : Stream.of(group))
              .collect(Collectors.toList());

          queryMetadata.setProjection(Projections.list(flattenColumns));

          // 创建 ColumnAliasTranspileContext（用于 ORDER BY/HAVING）
          TranspileContext newContext = columnAliases.isEmpty()
              ? context
              : new ColumnAliasTranspileContext(context, columnAliases);

          return Tuple.of(queryMetadata, newContext);
        });
  }

  private Stream<SExpression<?>> getSelectExpressions(QueryStatement query, TranspileContext context) {
    if (query.getSelect() == null || query.getSelect().isEmpty()) {
      return expandMainTableFields(query, context);
    }
    return query.getSelect().stream()
        .flatMap(selectExpr -> getSelectStream(query, context, selectExpr));
  }

  private Expression<?> applyImplicitAlias(SExpression<?> selectExpr, Expression<?> expr) {
    if (selectExpr.getOperator() != com.ouroboros.data.dsl.Operators.FIELD
        && selectExpr.getOperator() != ExtOps.FIELD) {
      return expr;
    }
    if (!(expr instanceof ModelFieldPath<?> modelFieldPath)) {
      return expr;
    }
    return modelFieldPath.as(modelFieldPath.getModelField().getName());
  }

  private Stream<SExpression<?>> getSelectStream(QueryStatement query, TranspileContext context, SExpression<?> sExpr) {
    var op = sExpr.getOperator();
    if (op == ExtOps.COLUMNS) {
      return sExpr.getParams().stream()
          .flatMap(param -> {
            if ("*".equals(param)) {
              return expandMainTableFields(query, context);
            }
            if (param instanceof SExpression<?> nestedExpr) {
              return getSelectStream(query, context, nestedExpr);
            }
            return Stream.empty();
          });
    }
    return Stream.of(sExpr);
  }

  private Stream<SExpression<?>> expandMainTableFields(QueryStatement query, TranspileContext context) {
    String mainAlias = Optional.ofNullable(query.getFrom())
        .map(QueryStatement.TableSource::getName)
        .orElse("");

    return resolveMainFieldSource(query, context)
        .<Stream<SExpression<?>>>map(fieldSource -> fieldSource.getFields().stream()
            .filter(path -> isSelectableField(fieldSource, path))
            .map(path -> toFieldExpression(mainAlias, path)))
        .orElseGet(Stream::empty);
  }

  private Optional<FieldSource> resolveMainFieldSource(QueryStatement query, TranspileContext context) {
    if (query.getFrom() == null) {
      return Optional.empty();
    }
    String mainAlias = query.getFrom().getName();
    Optional<FieldSource> byAlias = context.resolveTable(mainAlias);
    if (byAlias.isPresent()) {
      return byAlias;
    }
    return Optional.ofNullable(query.getFrom().getTableName())
        .flatMap(context::resolveTable);
  }

  private SExpression<?> toFieldExpression(String mainAlias, Path<?> path) {
    String fieldName = path instanceof ModelFieldPath<?> modelFieldPath
        ? modelFieldPath.getModelField().getName()
        : path.getMetadata().getName();
    if (mainAlias == null || mainAlias.isEmpty()) {
      return SExpression.field(fieldName);
    }
    return SExpression.field(mainAlias, fieldName);
  }

  private boolean isSelectableField(FieldSource fieldSource, Path<?> path) {
    if (!DefaultProjectionFieldSupport.isDirectDefaultProjectionPath(path)) {
      return false;
    }
    if (path instanceof ModelFieldPath<?>) {
      return true;
    }

    String fieldName = path.getMetadata().getName();
    return fieldSource.getField(fieldName).isPresent();
  }
}
