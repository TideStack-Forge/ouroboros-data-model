package com.ouroboros.data.model.plugins;

import java.util.*;
import java.util.stream.Collectors;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.SExpressions;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.model.*;
import com.ouroboros.data.record.RecordList;
import com.ouroboros.data.util.DataConverters;

/**
 * @author liansz
 **/
public class NormalizeWhereDataModelPlugin implements DataModelPlugin {
  private final Set<String> fields;
  private final boolean removeEmptyValue;
  private final boolean removeNullValue;

  public NormalizeWhereDataModelPlugin(Set<String> fields, boolean removeEmptyValue, boolean removeNullValue) {
    this.fields = fields;
    this.removeEmptyValue = removeEmptyValue;
    this.removeNullValue = removeNullValue;
  }

  @Override
  public Try<Long> update(SExpression<Boolean> where, Map<String, Object> data, DataModelPluginContext context) {
    return DataModelPlugin.super.update(normalize(where), data, context);
  }

  @Override
  public Try<Long> delete(SExpression<Boolean> where, DataModelPluginContext context) {
    return DataModelPlugin.super.delete(normalize(where), context);
  }

  @Override
  public Try<Long> count(QueryStatement statement, DataModelPluginContext context) {
    return DataModelPlugin.super.count(statement.getBuilder()
        .where(normalize(statement.getWhere()))
        .build(), context);
  }

  @Override
  public Try<RecordList> query(QueryStatement statement, DataModelPluginContext context) {
    return DataModelPlugin.super.query(statement.getBuilder()
        .where(normalize(statement.getWhere()))
        .build(), context);
  }

  private SExpression<Boolean> normalize(SExpression<Boolean> where) {
    return SExpressions.filter(where, (expression, context) -> shouldKeepExpression(expression))
        .map(expression -> expression.isEmpty()
            ? SExpression.empty(Boolean.class)
            : SExpression.<Boolean>create(expression.getOperator(), expression.getParams()))
        .orElseGet(() -> SExpression.empty(Boolean.class));
  }

  private boolean shouldKeepExpression(SExpression<?> expression) {
    return !isRemovedConstant(expression) && !isUnknownField(expression);
  }

  private boolean isRemovedConstant(SExpression<?> expression) {
    if (expression.getOperator() == Operators.CONSTANT && !expression.getParams().isEmpty()) {
      Object value = expression.getParam(0);
      return (removeNullValue && value == null)
          || (removeEmptyValue && value instanceof String str && str.isEmpty());
    }
    return false;
  }

  private boolean isUnknownField(SExpression<?> expression) {
    if (expression.getOperator() == Operators.FIELD && !expression.getParams().isEmpty()) {
      String fieldName = expression.getParams().get(expression.getParams().size() - 1).toString();
      return !fields.contains(fieldName);
    }
    return false;
  }

  public static class Builder implements DataModelPluginBuilder {

    @Override
    public boolean support(String name) {
      return "NormalizeWhere".equalsIgnoreCase(name);
    }

    @Override
    public Optional<DataModelPlugin> build(DataModel dataModel, Map<String, Object> config) {
      Set<String> fields = Optional.ofNullable(dataModel)
          .map(DataModel::getFields)
          .orElseGet(Collections::emptyList).stream()
          .filter(f -> f.getValueType().isPhysical())
          .map(DataModelField::getName)
          .collect(Collectors.toSet());
      var removeEmptyValue = Optional.ofNullable(config)
          .map(c -> c.get("removeEmptyValue"))
          .map(DataConverters::toBoolean)
          .orElse(true);
      var removeNullValue = Optional.ofNullable(config)
          .map(c -> c.get("removeNullValue"))
          .map(DataConverters::toBoolean)
          .orElse(false);

      return Optional.of(new NormalizeWhereDataModelPlugin(fields, removeEmptyValue, removeNullValue));
    }
  }
}
