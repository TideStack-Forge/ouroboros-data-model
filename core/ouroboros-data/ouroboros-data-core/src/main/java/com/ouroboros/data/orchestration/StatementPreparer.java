package com.ouroboros.data.orchestration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.OmitClause;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.query.DefaultProjectionFieldSupport;

/**
 * statement 预处理器。
 *
 * <p>负责 SELECT 展开、OMIT 过滤以及 COLUMNS 别名前缀补全。
 */
class StatementPreparer {

  QueryStatement prepare(QueryStatement statement, DataModel model, OrchestrationContext context) {
    List<SExpression<?>> originalSelect = statement.getSelect();
    OmitClause omitClause = context.getOmitClause();

    if ((originalSelect == null || originalSelect.isEmpty()) && omitClause == null
        && (statement.getJoins() == null || statement.getJoins().isEmpty())) {
      return statement;
    }

    String mainAlias = statement.getFrom() != null ? statement.getFrom().getName() : model.getName();

    List<SExpression<?>> select = new ArrayList<>(originalSelect);
    boolean hasWildcard = select.stream()
        .anyMatch(expr -> expr.getOperator() == Operators.COLUMNS
            && expr.getParams().stream().anyMatch("*"::equals));

    if (hasWildcard || select.isEmpty()) {
      select = expandWildcardSelect(select, model, mainAlias);
    } else {
      select = select.stream()
          .map(expr -> addAliasPrefix(expr, mainAlias))
          .collect(Collectors.toList());
    }

    if (omitClause != null) {
      Set<String> omitFields = omitClause.getFields();
      select = select.stream()
          .map(expr -> applyOmit(expr, omitFields))
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
    }

    return statement.getBuilder()
        .select(select.toArray(new SExpression<?>[0]))
        .build();
  }

  private List<SExpression<?>> expandWildcardSelect(
      List<SExpression<?>> originalSelect,
      DataModel model,
      String mainAlias) {
    if (originalSelect.isEmpty()) {
      return createExpandedMainFields(model, mainAlias);
    }

    List<SExpression<?>> result = new ArrayList<>();
    for (SExpression<?> expr : originalSelect) {
      if (expr.getOperator() != Operators.COLUMNS
          || expr.getParams().stream().noneMatch("*"::equals)) {
        result.add(addAliasPrefix(expr, mainAlias));
        continue;
      }

      for (Object param : expr.getParams()) {
        if ("*".equals(param)) {
          result.addAll(createExpandedMainFields(model, mainAlias));
          continue;
        }
        if (param instanceof SExpression<?> sExpr) {
          result.add(addAliasPrefix(sExpr, mainAlias));
        }
      }
    }
    return result;
  }

  private List<SExpression<?>> createExpandedMainFields(DataModel model, String mainAlias) {
    return model.getFields().stream()
        .filter(DefaultProjectionFieldSupport::isDirectDefaultProjectionField)
        .map(f -> (SExpression<?>) SExpression.alias(
            SExpression.columns(SExpression.field(mainAlias, f.getName())),
            f.getName()))
        .collect(Collectors.toList());
  }

  private String extractFieldName(SExpression<?> expr) {
    if (expr.getOperator() == Operators.ALIAS && expr.getParams().size() >= 2) {
      Object alias = expr.getParam(1);
      return alias instanceof String ? (String) alias : null;
    }
    if (expr.getOperator() == Operators.FIELD && !expr.getParams().isEmpty()) {
      Object lastSegment = expr.getParam(expr.getParams().size() - 1);
      return lastSegment instanceof String ? (String) lastSegment : null;
    }
    if (expr.getOperator() == Operators.COLUMNS && expr.getParams().size() >= 1) {
      Object col = expr.getParam(0);
      if (col instanceof SExpression<?> columnExpr
          && columnExpr.getOperator() == Operators.FIELD
          && !columnExpr.getParams().isEmpty()) {
        Object lastSegment = columnExpr.getParam(columnExpr.getParams().size() - 1);
        return lastSegment instanceof String ? (String) lastSegment : null;
      }
    }
    return null;
  }

  private SExpression<?> applyOmit(SExpression<?> expr, Set<String> omitFields) {
    String fieldName = extractFieldName(expr);
    if (fieldName != null && omitFields.contains(fieldName)) {
      return null;
    }

    if (expr.getOperator() == Operators.COLUMNS) {
      List<SExpression<?>> kept = expr.getParams().stream()
          .filter(SExpression.class::isInstance)
          .map(SExpression.class::cast)
          .map(column -> applyOmit(column, omitFields))
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
      if (kept.isEmpty()) {
        return null;
      }
      return SExpression.create(Operators.COLUMNS, kept);
    }

    if (expr.getOperator() == Operators.ALIAS && expr.getParams().size() >= 2) {
      Object inner = expr.getParam(0);
      if (inner instanceof SExpression<?> innerExpr) {
        SExpression<?> keptInner = applyOmit(innerExpr, omitFields);
        if (keptInner == null) {
          return null;
        }
        if (keptInner != innerExpr) {
          return SExpression.alias(keptInner, String.valueOf(expr.getParam(1)));
        }
      }
    }

    return expr;
  }

  private SExpression<?> addAliasPrefix(SExpression<?> expr, String mainAlias) {
    if (expr.getOperator() == Operators.FIELD && expr.getParams().size() == 1) {
      Object colName = expr.getParam(0);
      if (colName instanceof String) {
        return SExpression.field(mainAlias, (String) colName);
      }
    }
    if (expr.getOperator() == Operators.COLUMNS) {
      List<Object> prefixedParams = expr.getParams().stream()
          .map(param -> param instanceof SExpression<?> sExpr ? addAliasPrefix(sExpr, mainAlias) : param)
          .collect(Collectors.toList());
      return SExpression.create(Operators.COLUMNS, prefixedParams);
    }
    if (expr.getOperator() == Operators.ALIAS && expr.getParams().size() >= 2) {
      Object inner = expr.getParam(0);
      if (inner instanceof SExpression<?> innerExpr) {
        SExpression<?> prefixed = addAliasPrefix(innerExpr, mainAlias);
        if (prefixed != innerExpr) {
          return SExpression.alias(prefixed, String.valueOf(expr.getParam(1)));
        }
      }
    }
    return expr;
  }
}
