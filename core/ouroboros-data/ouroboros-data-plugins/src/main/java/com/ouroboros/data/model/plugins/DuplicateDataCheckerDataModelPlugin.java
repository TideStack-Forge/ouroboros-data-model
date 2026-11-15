package com.ouroboros.data.model.plugins;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;

import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.exception.InvalidStatementException;
import com.ouroboros.data.exception.UniqueConstraintViolationException;
import com.ouroboros.data.model.*;
import com.ouroboros.data.record.Record;
import com.ouroboros.data.record.RecordList;

/**
 * @author liansz
 **/
public class DuplicateDataCheckerDataModelPlugin implements DataModelPlugin {
  private static final String PLUGIN_NAME = "DuplicateDataChecker";
  private static final List<String> LOGICAL_DELETE_PLUGIN_NAMES = List.of("LogicalDelete", "SoftDelete");

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final DataModel dataModel;
  private final List<DataModelUniqueConstraint> uniqueConstraints;
  private final boolean notifyDuplicateFields;
  private final Collection<String> primaryKeyFields;

  private DuplicateDataCheckerDataModelPlugin(DataModel dataModel,
                                              List<DataModelUniqueConstraint> uniqueConstraints,
                                              boolean notifyDuplicateFields) {
    this.dataModel = dataModel;
    this.uniqueConstraints = uniqueConstraints;
    this.notifyDuplicateFields = notifyDuplicateFields;

    primaryKeyFields = dataModel.getPrimaryKeys().stream().map(DataModelField::getName).collect(Collectors.toList());
  }

  @Override
  public Try<Record> insert(Map<String, Object> data, DataModelPluginContext context) {
    var checked = checkUniqueConditions(uniqueCheckConditions(data), Collections.emptyMap(), List.of(data));
    if (checked.isFailure()) {
      return Try.failure(checked.getCause());
    }
    return DataModelPlugin.super.insert(data, context);
  }

  @Override
  public Try<RecordList> batchInsert(List<Map<String, Object>> dataList, DataModelPluginContext context) {
    List<String> duplicateFields = getBatchDuplicateFieldLabels(dataList);
    if (!duplicateFields.isEmpty()) {
      return Try.failure(new UniqueConstraintViolationException(
          String.format("字段%s存在重复值，请检查!", String.join(",", duplicateFields)), duplicateFields));
    }
    var conditions = dataList.stream().flatMap(data -> uniqueCheckConditions(data).stream()).toList();
    var checked = checkUniqueConditions(conditions, Collections.emptyMap(), dataList);
    if (checked.isFailure()) {
      return Try.failure(checked.getCause());
    }
    return DataModelPlugin.super.batchInsert(dataList, context);
  }

  @Override
  public Try<Long> update(SExpression<Boolean> where, Map<String, Object> data, DataModelPluginContext context) {
    List<DataModelUniqueConstraint> affectedConstraints = affectedUniqueConstraints(data);
    if (!affectedConstraints.isEmpty()) {
      Map<String, Object> primaryKeyWhere = primaryKeyFields.stream()
          .map(field -> Tuple.of(field, deepPick(where, field)))
          .filter(tuple -> tuple._2() != null)
          .collect(Collectors.toMap(Tuple2::_1, Tuple2::_2, (v1, v2) -> v2));

      boolean hasCompletePrimaryKey = primaryKeyWhere.size() == primaryKeyFields.size();
      Map<String, Object> uniqueFieldWhere = uniqueConstraintFields().stream()
          .map(field -> Tuple.of(field, deepPick(where, field)))
          .filter(tuple -> tuple._2() != null)
          .collect(Collectors.toMap(Tuple2::_1, Tuple2::_2, (v1, v2) -> v2));
      Optional<Map<String, Object>> uniqueIdentityWhere = completeUniqueIdentityWhere(uniqueFieldWhere);

      if (!hasCompletePrimaryKey && uniqueIdentityWhere.isEmpty()) {
        return Try.failure(
            new InvalidStatementException(String.format("数据模型 %s 更新指定了唯一约束字段但涉及到批量更新！", dataModel.getFullName()))
        );
      }

      DataModel checkDataModel = uniquenessCheckDataModel(UniquenessScope.ACTIVE_RECORDS);
      var baseUniqueValuesResult = buildBaseUniqueValues(
          checkDataModel,
          affectedConstraints,
          data,
          hasCompletePrimaryKey,
          primaryKeyWhere,
          uniqueFieldWhere
      );
      if (baseUniqueValuesResult.isFailure()) {
        return Try.failure(baseUniqueValuesResult.getCause());
      }
      Optional<Map<String, Object>> baseUniqueValues = baseUniqueValuesResult.get();
      if (baseUniqueValues.isEmpty()) {
        return DataModelPlugin.super.update(where, data, context);
      }

      var updateConditionResult = buildUpdateUniqueCheckConditions(affectedConstraints, data, baseUniqueValues.get());
      if (updateConditionResult.isFailure()) {
        return Try.failure(updateConditionResult.getCause());
      }
      List<UniqueCheck> uniqueConditions = updateConditionResult.get();
      if (uniqueConditions.isEmpty()) {
        return DataModelPlugin.super.update(where, data, context);
      }
      Map<String, Object> exclusionWhere = buildExclusionConditions(
          hasCompletePrimaryKey ? primaryKeyWhere : uniqueIdentityWhere.get()
      );
      Map<String, Object> updatedValues = new LinkedHashMap<>(baseUniqueValues.get());
      updatedValues.putAll(data);
      var checked = checkUniqueConditions(uniqueConditions, exclusionWhere, List.of(updatedValues));
      if (checked.isFailure()) {
        return Try.failure(checked.getCause());
      }
    }

    return DataModelPlugin.super.update(where, data, context);
  }

  private record UniqueCheck(UniquenessScope scope, Map<String, Object> condition) {
  }

  private List<UniqueCheck> uniqueCheckConditions(Map<String, Object> data) {
    return uniqueConstraints.stream()
        .map(constraint -> new UniqueCheck(constraint.getScope(), buildConstraintCondition(constraint, data)))
        .filter(check -> !check.condition().isEmpty())
        .toList();
  }

  private Try<Void> checkUniqueConditions(List<UniqueCheck> conditions, Map<String, Object> exclusion,
                                         List<Map<String, Object>> candidates) {
    for (UniqueCheck check : conditions) {
      DataModel checkDataModel = uniquenessCheckDataModel(check.scope());
      Map<String, Object> where = combineAnd(exclusion, check.condition());
      Try<Long> count = checkDataModel.count(where)
          .onFailure(e -> logger.error(String.format("数据模型 %s 查询数据错误: ", dataModel.getFullName()), e));
      if (count.isFailure()) {
        return Try.failure(count.getCause());
      }
      if (count.get() == 0) {
        continue;
      }
      if (notifyDuplicateFields) {
        var records = checkDataModel.query(Map.of(Keyword.WHERE.toString(), where, Keyword.LIMIT.toString(), 1));
        if (records.isFailure()) {
          return Try.failure(records.getCause());
        }
        var labels = records.get().stream().flatMap(record -> candidates.stream()
            .flatMap(candidate -> getDuplicateFieldLabels(candidate, record, check.scope()).stream()))
            .distinct().toList();
        if (!labels.isEmpty()) {
          return Try.failure(new UniqueConstraintViolationException(
              String.format("字段%s存在重复值，请检查!", String.join(",", labels)), labels));
        }
      }
      return Try.failure(new UniqueConstraintViolationException("存在重复字段，请检查!"));
    }
    return Try.success(null);
  }

  private Try<List<UniqueCheck>> buildUpdateUniqueCheckConditions(
      List<DataModelUniqueConstraint> affectedConstraints,
      Map<String, Object> data,
      Map<String, Object> baseUniqueValues
  ) {
    List<UniqueCheck> conditions = new ArrayList<>();
    for (DataModelUniqueConstraint constraint : affectedConstraints) {
      Map<String, Object> values = new LinkedHashMap<>(baseUniqueValues);
      values.putAll(data);
      if (buildConstraintValues(constraint, values).isEmpty()) {
        return Try.failure(new InvalidStatementException(
            String.format("数据模型 %s 更新指定了唯一约束字段但无法解析完整唯一约束值！", dataModel.getFullName())
        ));
      }
      Map<String, Object> condition = buildConstraintCondition(constraint, values);
      if (!condition.isEmpty()) {
        conditions.add(new UniqueCheck(constraint.getScope(), condition));
      }
    }
    return Try.success(conditions);
  }

  private Try<Optional<Map<String, Object>>> buildBaseUniqueValues(
      DataModel checkDataModel,
      List<DataModelUniqueConstraint> affectedConstraints,
      Map<String, Object> data,
      boolean hasCompletePrimaryKey,
      Map<String, Object> primaryKeyWhere,
      Map<String, Object> uniqueFieldWhere
  ) {
    Map<String, Object> candidateValues = new LinkedHashMap<>(uniqueFieldWhere);
    candidateValues.putAll(data);
    if (completeConstraints(affectedConstraints, candidateValues)) {
      return Try.success(Optional.of(uniqueFieldWhere));
    }
    if (!hasCompletePrimaryKey) {
      return Try.success(Optional.of(uniqueFieldWhere));
    }
    return loadCurrentUniqueValues(checkDataModel, primaryKeyWhere)
        .map(currentValues -> currentValues.map(values -> {
          Map<String, Object> baseValues = new LinkedHashMap<>(values);
          baseValues.putAll(uniqueFieldWhere);
          return baseValues;
        }));
  }

  private boolean completeConstraints(List<DataModelUniqueConstraint> constraints, Map<String, Object> values) {
    return constraints.stream()
        .allMatch(constraint -> !buildConstraintValues(constraint, values).isEmpty());
  }

  private Try<Optional<Map<String, Object>>> loadCurrentUniqueValues(
      DataModel checkDataModel,
      Map<String, Object> primaryKeyWhere
  ) {
    Map<String, Object> statement = new HashMap<>();
    statement.put(Keyword.WHERE.toString(), primaryKeyWhere);
    statement.put(Keyword.LIMIT.toString(), 1);
    return checkDataModel.query(statement)
        .map(records -> {
          if (records.isEmpty()) {
            return Optional.empty();
          }
          Map<String, Object> values = new LinkedHashMap<>();
          for (String field : uniqueConstraintFields()) {
            if (records.get(0).containsKey(field)) {
              values.put(field, records.get(0).get(field));
            }
          }
          return Optional.of(values);
        });
  }

  private Map<String, Object> buildConstraintCondition(
      DataModelUniqueConstraint constraint,
      Map<String, Object> data
  ) {
    Map<String, Object> condition = new LinkedHashMap<>();
    for (String field : constraint.getFields()) {
      if (!data.containsKey(field)) {
        return Collections.emptyMap();
      }
      condition.put(field, toPersistentValue(field, data.get(field)));
    }
    if (constraint.isNullsDistinct() && condition.containsValue(null)) {
      return Collections.emptyMap();
    }
    return condition;
  }

  private Optional<Map<String, Object>> completeUniqueIdentityWhere(Map<String, Object> uniqueFieldWhere) {
    return uniqueConstraints.stream()
        .filter(constraint -> !buildConstraintCondition(constraint, uniqueFieldWhere).isEmpty())
        .map(constraint -> buildConstraintValues(constraint, uniqueFieldWhere))
        .filter(condition -> !condition.isEmpty())
        .findFirst();
  }

  private Map<String, Object> buildConstraintValues(
      DataModelUniqueConstraint constraint,
      Map<String, Object> data
  ) {
    Map<String, Object> values = new LinkedHashMap<>();
    for (String field : constraint.getFields()) {
      if (!data.containsKey(field)) {
        return Collections.emptyMap();
      }
      values.put(field, data.get(field));
    }
    return values;
  }

  private Map<String, Object> combineOr(List<Map<String, Object>> conditions) {
    if (conditions.isEmpty()) {
      return Collections.emptyMap();
    }

    if (conditions.size() == 1) {
      return conditions.get(0);
    }

    return Collections.singletonMap("or", conditions);
  }

  private Map<String, Object> combineAnd(Map<String, Object> left, Map<String, Object> right) {
    if (left.isEmpty()) {
      return right;
    }
    if (right.isEmpty()) {
      return left;
    }
    if (Collections.disjoint(left.keySet(), right.keySet())) {
      Map<String, Object> combined = new LinkedHashMap<>(left);
      combined.putAll(right);
      return Collections.singletonMap("and", combined);
    }
    return Collections.singletonMap("and", List.of(left, right));
  }

  private DataModel uniquenessCheckDataModel(UniquenessScope scope) {
    var checkDataModel = dataModel.withoutPlugins();
    if (scope == UniquenessScope.ALL_RECORDS) {
      return checkDataModel;
    }
    var pluginDescriptors = logicalDeletePluginDescriptors();
    if (pluginDescriptors.isEmpty()) {
      return checkDataModel;
    }
    return checkDataModel.withPlugins(pluginDescriptors);
  }

  private List<PluginDescriptor> logicalDeletePluginDescriptors() {
    return LOGICAL_DELETE_PLUGIN_NAMES.stream()
        .filter(dataModel::hasPlugin)
        .map(PluginDescriptor::new)
        .collect(Collectors.toList());
  }

  private List<String> getDuplicateFieldLabels(Map<String, Object> data, Record record, UniquenessScope scope) {
    return uniqueConstraints.stream()
        .filter(constraint -> constraint.getScope() == scope)
        .filter(constraint -> isDuplicateConstraintValue(constraint, data, record))
        .flatMap(constraint -> constraint.getFields().stream())
        .map(this::fieldLabel)
        .collect(Collectors.toList());
  }

  private boolean isDuplicateConstraintValue(
      DataModelUniqueConstraint constraint,
      Map<String, Object> data,
      Record record
  ) {
    Map<String, Object> condition = buildConstraintCondition(constraint, data);
    return !condition.isEmpty() && constraint.getFields().stream()
        .allMatch(field -> Objects.equals(condition.get(field), toPersistentValue(field, record.get(field))));
  }

  private List<String> getBatchDuplicateFieldLabels(List<Map<String, Object>> dataList) {
    for (DataModelUniqueConstraint constraint : uniqueConstraints) {
      Set<List<Object>> seen = new HashSet<>();
      for (Map<String, Object> data : dataList) {
        Map<String, Object> condition = buildConstraintCondition(constraint, data);
        if (condition.isEmpty()) {
          continue;
        }
        List<Object> key = constraint.getFields().stream()
            .map(condition::get)
            .collect(Collectors.toList());
        if (!seen.add(key)) {
          return constraint.getFields().stream()
              .map(this::fieldLabel)
              .collect(Collectors.toList());
        }
      }
    }
    return Collections.emptyList();
  }

  private String fieldLabel(String field) {
    return dataModel.getField(field)
        .map(f -> StringUtils.defaultIfBlank(f.getLabel(), f.getName()))
        .orElse(field);
  }

  private Object toPersistentValue(String field, Object value) {
    Function<Object, Object> valueConverter = dataModel.getField(field)
        .map(DataModelField::getValueType)
        .map(valueType -> (Function<Object, Object>) valueType::toPersistentValue)
        .orElse(o -> o);
    return valueConverter.apply(value);
  }

  private List<DataModelUniqueConstraint> affectedUniqueConstraints(Map<String, Object> data) {
    return uniqueConstraints.stream()
        .filter(constraint -> constraint.getFields().stream().anyMatch(data::containsKey))
        .collect(Collectors.toList());
  }

  private List<String> uniqueConstraintFields() {
    return uniqueConstraints.stream()
        .flatMap(constraint -> constraint.getFields().stream())
        .distinct()
        .collect(Collectors.toList());
  }

  private Map<String, Object> buildExclusionConditions(Map<String, Object> values) {
    if (values.isEmpty()) {
      return Collections.emptyMap();
    }
    List<Map<String, Object>> conditions = values.entrySet().stream()
        .map(entry -> {
          Map<String, Object> comparison = new LinkedHashMap<>();
          comparison.put("!=", toPersistentValue(entry.getKey(), entry.getValue()));
          Map<String, Object> condition = new LinkedHashMap<>();
          condition.put(entry.getKey(), comparison);
          return condition;
        })
        .collect(Collectors.toList());
    return combineOr(conditions);
  }

  private Object deepPick(SExpression<?> expression, String key) {
    if (expression == null || expression.isEmpty() || key == null) {
      return null;
    }

    if (isComparisonWithField(expression, key)) {
      return comparisonValue(expression, key);
    }

    for (Object param : expression.getParams()) {
      if (param instanceof SExpression<?> nested) {
        Object result = deepPick(nested, key);
        if (result != null) {
          return result;
        }
      }
    }

    return null;
  }

  private boolean isComparisonWithField(SExpression<?> expression, String key) {
    return expression.getParams().size() >= 2
        && expression.getOperator() == Operators.EQ
        && (isField(expression.getParam(0), key) || isField(expression.getParam(1), key));
  }

  private Object comparisonValue(SExpression<?> expression, String key) {
    if (isField(expression.getParam(0), key)) {
      return expressionValue(expression.getParam(1));
    }
    return expressionValue(expression.getParam(0));
  }

  private boolean isField(Object value, String key) {
    if (!(value instanceof SExpression<?> fieldExpression)
        || fieldExpression.getOperator() != Operators.FIELD
        || fieldExpression.getParams().isEmpty()) {
      return false;
    }
    Object last = fieldExpression.getParam(fieldExpression.getParams().size() - 1);
    return key.equals(last);
  }

  private Object expressionValue(Object value) {
    if (value instanceof SExpression<?> expression
        && expression.getOperator() == Operators.CONSTANT
        && !expression.getParams().isEmpty()) {
      return expression.getParam(0);
    }
    return value;
  }

  public static class Builder implements DataModelPluginBuilder {

    @Override
    public boolean support(String name) {
      return PLUGIN_NAME.equalsIgnoreCase(name);
    }

    @Override
    public Optional<DataModelPlugin> build(DataModel dataModel, Map<String, Object> config) {
      List<DataModelUniqueConstraint> uniqueConstraints = DataModelUniqueConstraints.resolve(dataModel);

      if (uniqueConstraints.isEmpty()) {
        return Optional.empty();
      }

      return Optional.of(
          new DuplicateDataCheckerDataModelPlugin(
              dataModel,
              uniqueConstraints,
              (boolean) config.getOrDefault("notifyDuplicateFields", true)
          )
      );
    }
  }
}
