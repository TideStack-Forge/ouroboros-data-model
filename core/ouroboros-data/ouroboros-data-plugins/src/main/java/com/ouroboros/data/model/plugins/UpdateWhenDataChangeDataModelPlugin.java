package com.ouroboros.data.model.plugins;

import java.util.*;
import java.util.stream.Collectors;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.model.*;

/**
 * @author liansz
 **/
public class UpdateWhenDataChangeDataModelPlugin implements DataModelPlugin {

  private final List<DataModelField> fields;

  private UpdateWhenDataChangeDataModelPlugin(List<DataModelField> fields) {
    this.fields = fields;
  }

  @Override
  public Try<Long> update(SExpression<Boolean> where, Map<String, Object> data, DataModelPluginContext context) {
    Optional<SExpression<Boolean>> changedPredicate = buildChangedPredicate(data);
    if (changedPredicate.isEmpty()) {
      return Try.success(0L);
    }
    return DataModelPlugin.super.update(and(where, changedPredicate.get()), data, context);
  }

  private Optional<SExpression<Boolean>> buildChangedPredicate(Map<String, Object> data) {
    List<SExpression<Boolean>> predicates = data.entrySet().stream()
        .map(entry -> physicalField(entry.getKey())
            .map(field -> buildFieldChangedPredicate(field, entry.getValue())))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
    if (predicates.isEmpty()) {
      return Optional.empty();
    }
    if (predicates.size() == 1) {
      return Optional.of(predicates.get(0));
    }
    return Optional.of(SExpression.create(Operators.OR, predicates));
  }

  private Optional<DataModelField> physicalField(String name) {
    return fields.stream()
        .filter(field -> Objects.equals(field.getName(), name))
        .filter(field -> field.getValueType().isPhysical())
        .findFirst();
  }

  private SExpression<Boolean> buildFieldChangedPredicate(DataModelField field, Object value) {
    Object persistentValue = field.getValueType().toPersistentValue(value);
    SExpression<?> fieldExpression = SExpression.field(field.getName());
    if (persistentValue == null) {
      return SExpression.create(Operators.IS_NOT_NULL, fieldExpression);
    }
    return SExpression.create(
        Operators.OR,
        SExpression.create(Operators.NE, fieldExpression, SExpression.create(Operators.CONSTANT, persistentValue)),
        SExpression.create(Operators.IS_NULL, SExpression.field(field.getName())));
  }

  private SExpression<Boolean> and(SExpression<Boolean> left, SExpression<Boolean> right) {
    if (left == null || left.isEmpty()) {
      return right;
    }
    if (right == null || right.isEmpty()) {
      return left;
    }
    return SExpression.create(Operators.AND, left, right);
  }

  public static class Builder implements DataModelPluginBuilder {

    @Override
    public boolean support(String name) {
      return "UpdateWhenDataChange".equalsIgnoreCase(name);
    }

    @Override
    public Optional<DataModelPlugin> build(DataModel dataModel, Map<String, Object> config) {
      return Optional.of(new UpdateWhenDataChangeDataModelPlugin(dataModel.getFields()));
    }
  }
}
