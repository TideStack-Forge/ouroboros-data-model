package com.ouroboros.data.dsl.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.PluginDescriptor;
import com.ouroboros.data.record.RecordList;

final class DefaultDataModelQuery implements DataModelQuery {
  private static final Set<Keyword> QUERY_CLAUSES = EnumSet.of(
      Keyword.WITH,
      Keyword.WITH_RECURSIVE,
      Keyword.SELECT,
      Keyword.DISTINCT,
      Keyword.OMIT,
      Keyword.FROM,
      Keyword.WHERE,
      Keyword.LIMIT,
      Keyword.OFFSET,
      Keyword.PAGE,
      Keyword.PAGESIZE,
      Keyword.ORDER,
      Keyword.GROUP,
      Keyword.HAVING,
      Keyword.UNION,
      Keyword.UNION_ALL,
      Keyword.POPULATE,
      Keyword.JOIN,
      Keyword.INNER_JOIN,
      Keyword.LEFT_JOIN,
      Keyword.RIGHT_JOIN,
      Keyword.FULL_JOIN,
      Keyword.CROSS_JOIN);

  private final Object from;
  private DataModel dataModel;
  private Object select;
  private Object populate;
  private Object where;
  private final Map<String, Object> clauses = new LinkedHashMap<>();

  private DefaultDataModelQuery(DataModel dataModel, Object from) {
    this.dataModel = dataModel;
    this.from = from;
  }

  static DataModelQuery standalone(Object from) {
    return new DefaultDataModelQuery(null, Objects.requireNonNull(from, "from must not be null"));
  }

  static DataModelQuery bound(DataModel dataModel) {
    return new DefaultDataModelQuery(dataModel, null);
  }

  @Override
  public DataModelQuery select(QueryExpression<?>... expressions) {
    if (expressions == null) {
      throw new IllegalArgumentException("expressions must not be null");
    }
    List<Object> selectItems = new ArrayList<>();
    for (QueryExpression<?> expression : expressions) {
      if (expression == null) {
        throw new IllegalArgumentException("expressions must not contain null");
      }
      selectItems.add(expression.toRawValue());
    }
    select = List.copyOf(selectItems);
    return this;
  }

  @Override
  public DataModelQuery select(String selectClause) {
    if (selectClause == null || selectClause.isBlank()) {
      throw new IllegalArgumentException("select must not be blank");
    }
    select = selectClause;
    return this;
  }

  @Override
  public DataModelQuery select(Collection<?> selectClause) {
    select = requireClause("select", selectClause);
    return this;
  }

  @Override
  public DataModelQuery select(Object[] selectClause) {
    if (selectClause == null) {
      throw new IllegalArgumentException("select must not be null");
    }
    return select(Arrays.asList(selectClause));
  }

  @Override
  public DataModelQuery select(Map<String, ?> selectClause) {
    select = requireClause("select", selectClause);
    return this;
  }

  @Override
  public DataModelQuery select(SExpression<?> selectExpression) {
    select = requireClause("select", selectExpression);
    return this;
  }

  @Override
  public DataModelQuery where(QueryCondition... conditions) {
    return where(QueryConditions.requireConditions(conditions));
  }

  @Override
  public DataModelQuery where(Collection<?> whereClause) {
    where = buildWhereFromCollection(whereClause);
    return this;
  }

  @Override
  public DataModelQuery where(Map<String, ?> whereClause) {
    where = requireWhereClause(whereClause);
    return this;
  }

  @Override
  public DataModelQuery where(SExpression<Boolean> whereExpression) {
    where = requireWhereClause(whereExpression);
    return this;
  }

  @Override
  public DataModelQuery andWhere(QueryCondition... conditions) {
    appendWhere("$and", QueryConditions.requireConditions(conditions));
    return this;
  }

  @Override
  public DataModelQuery andWhere(Collection<?> whereClause) {
    appendWhereValue("$and", buildWhereFromCollection(whereClause));
    return this;
  }

  @Override
  public DataModelQuery andWhere(Map<String, ?> whereClause) {
    appendWhereValue("$and", requireWhereClause(whereClause));
    return this;
  }

  @Override
  public DataModelQuery andWhere(SExpression<Boolean> whereExpression) {
    appendWhereValue("$and", requireWhereClause(whereExpression));
    return this;
  }

  @Override
  public DataModelQuery orWhere(QueryCondition... conditions) {
    appendWhere("$or", QueryConditions.requireConditions(conditions));
    return this;
  }

  @Override
  public DataModelQuery orWhere(Collection<?> whereClause) {
    appendWhereValue("$or", buildWhereFromCollection(whereClause));
    return this;
  }

  @Override
  public DataModelQuery orWhere(Map<String, ?> whereClause) {
    appendWhereValue("$or", requireWhereClause(whereClause));
    return this;
  }

  @Override
  public DataModelQuery orWhere(SExpression<Boolean> whereExpression) {
    appendWhereValue("$or", requireWhereClause(whereExpression));
    return this;
  }

  @Override
  public DataModelQuery populate(String populateClause) {
    if (populateClause == null || populateClause.isBlank()) {
      throw new IllegalArgumentException("populate must not be blank");
    }
    populate = populateClause;
    return this;
  }

  @Override
  public DataModelQuery populate(String... populateFields) {
    if (populateFields == null) {
      throw new IllegalArgumentException("populate fields must not be null");
    }
    List<String> rawFields = new ArrayList<>();
    for (String field : populateFields) {
      if (field == null || field.isBlank()) {
        throw new IllegalArgumentException("populate fields must not contain blank values");
      }
      rawFields.add(field);
    }
    populate = List.copyOf(rawFields);
    return this;
  }

  @Override
  public DataModelQuery populate(Collection<?> populateClause) {
    populate = requireClause("populate", populateClause);
    return this;
  }

  @Override
  public DataModelQuery populate(Map<String, ?> populateClause) {
    populate = requireClause("populate", populateClause);
    return this;
  }

  @Override
  public DataModelQuery populate(PopulateSpec... populates) {
    populate = PopulateSpec.toRawMap(populates);
    return this;
  }

  @Override
  public DataModelQuery clause(String clauseName, Object clause) {
    String normalizedName = normalizeClauseName(clauseName);
    if (Keyword.SELECT.toString().equals(normalizedName)) {
      select = requireClause("clause", clause);
    } else if (Keyword.WHERE.toString().equals(normalizedName)) {
      where = requireWhereClause(clause);
    } else if (Keyword.POPULATE.toString().equals(normalizedName)) {
      populate = requireClause("clause", clause);
    } else {
      clauses.put(normalizedName, requireClause("clause", clause));
    }
    return this;
  }

  @Override
  public DataModelQuery clauses(Map<String, ?> clauses) {
    if (clauses == null) {
      throw new IllegalArgumentException("clauses must not be null");
    }
    clauses.forEach(this::clause);
    return this;
  }

  @Override
  public DataModelQuery withPlugins(PluginDescriptor... plugins) {
    requireBoundModel();
    dataModel = dataModel.withPlugins(plugins);
    return this;
  }

  @Override
  public DataModelQuery withoutPlugins(String... pluginNames) {
    requireBoundModel();
    dataModel = dataModel.withoutPlugins(pluginNames);
    return this;
  }

  @Override
  public Map<String, Object> build() {
    Map<String, Object> rawMap = new LinkedHashMap<>();
    if (from != null) {
      rawMap.put(Keyword.FROM.toString(), PopulateSpec.copyRaw(from));
    }
    if (select != null) {
      rawMap.put(Keyword.SELECT.toString(), PopulateSpec.copyRaw(select));
    }
    if (where != null) {
      rawMap.put(Keyword.WHERE.toString(), QueryRawValues.normalizeWhere(where));
    }
    if (populate != null) {
      rawMap.put(Keyword.POPULATE.toString(), PopulateSpec.copyRaw(populate));
    }
    clauses.forEach((key, value) -> rawMap.put(key, PopulateSpec.copyRaw(value)));
    validateFieldReferences(rawMap);
    return rawMap;
  }

  @Override
  public Try<RecordList> execute() {
    requireBoundModel();
    return dataModel.query(build());
  }

  private Object combine(String operator, List<QueryCondition> conditions) {
    if (conditions.isEmpty()) {
      return null;
    }
    if (conditions.size() == 1) {
      return conditions.get(0).toRawCondition();
    }
    return RawQueryCondition.combine(operator, conditions).toRawCondition();
  }

  private void appendWhere(String operator, List<QueryCondition> conditions) {
    Object appended = combine(operator, conditions);
    appendWhereValue(operator, appended);
  }

  private Object buildWhereFromCollection(Collection<?> whereClause) {
    if (whereClause == null) {
      throw new IllegalArgumentException("where must not be null");
    }
    if (whereClause.isEmpty()) {
      return null;
    }

    List<QueryCondition> conditions = new ArrayList<>();
    boolean allConditions = true;
    for (Object item : whereClause) {
      if (item instanceof QueryCondition condition) {
        conditions.add(condition);
      } else {
        allConditions = false;
      }
    }
    if (allConditions) {
      return combine("$and", conditions);
    }

    List<Object> rawItems = new ArrayList<>();
    for (Object item : whereClause) {
      if (item == null) {
        throw new IllegalArgumentException("where must not contain null");
      }
      rawItems.add(item instanceof QueryCondition condition
          ? condition.toRawCondition()
          : QueryRawValues.normalizeWhere(item));
    }
    return List.copyOf(rawItems);
  }

  private void appendWhereValue(String operator, Object appended) {
    if (appended == null) {
      return;
    }
    if (where == null) {
      where = appended;
      return;
    }
    List<Object> rawConditions = new ArrayList<>();
    rawConditions.add(where);
    rawConditions.add(appended);
    Map<String, Object> raw = new LinkedHashMap<>();
    raw.put(operator, rawConditions);
    where = raw;
  }

  private Object requireClause(String name, Object clause) {
    if (clause == null) {
      throw new IllegalArgumentException(name + " must not be null");
    }
    return PopulateSpec.copyRaw(clause);
  }

  private Object requireWhereClause(Object clause) {
    if (clause == null) {
      throw new IllegalArgumentException("where must not be null");
    }
    return QueryRawValues.normalizeWhere(clause);
  }

  private String normalizeClauseName(String clauseName) {
    if (clauseName == null || clauseName.isBlank()) {
      throw new IllegalArgumentException("clauseName must not be blank");
    }
    Keyword keyword = Keyword.fromString(clauseName);
    if (keyword == null || !QUERY_CLAUSES.contains(keyword)) {
      throw new IllegalArgumentException("Unsupported query clause: " + clauseName);
    }
    return keyword.toString();
  }

  private void requireBoundModel() {
    Objects.requireNonNull(dataModel, "DataModelQuery is not bound to a DataModel");
  }

  private void validateFieldReferences(Map<String, Object> rawMap) {
    if (dataModel == null) {
      return;
    }
    List<DataModelField> fields = dataModel.getFields();
    if (fields == null || fields.isEmpty()) {
      return;
    }

    Set<String> knownFields = fields.stream()
        .map(DataModelField::getName)
        .filter(name -> name != null && !name.isBlank())
        .collect(Collectors.toCollection(LinkedHashSet::new));
    if (knownFields.isEmpty()) {
      return;
    }

    Set<String> references = new LinkedHashSet<>();
    collectSelectReferences(rawMap.get(Keyword.SELECT.toString()), references);
    collectWhereReferences(rawMap.get(Keyword.WHERE.toString()), references);
    collectPopulateReferences(rawMap.get(Keyword.POPULATE.toString()), references);

    Set<String> modelAliases = modelAliases();
    for (String reference : references) {
      if (!isKnownFieldPath(reference, knownFields, modelAliases)) {
        throw new IllegalArgumentException("Unknown field '" + reference + "' for DataModel " + dataModel.getFullName());
      }
    }
  }

  private void collectExpressionReferences(Object raw, Set<String> references) {
    if (raw instanceof String fieldPath && !fieldPath.isBlank()) {
      references.add(fieldPath);
    } else if (raw instanceof Collection<?> values) {
      values.forEach(value -> collectExpressionReferences(value, references));
    } else if (raw instanceof Map<?, ?> values) {
      values.values().forEach(value -> collectExpressionReferences(value, references));
    }
  }

  private void collectSelectReferences(Object raw, Set<String> references) {
    if (raw instanceof String rawSelect) {
      collectSelectStringReferences(rawSelect, references);
    } else if (raw instanceof Collection<?> values) {
      values.forEach(value -> collectSelectReferences(value, references));
    } else if (raw instanceof Map<?, ?> values) {
      values.values().forEach(value -> collectSelectReferences(value, references));
    } else {
      collectExpressionReferences(raw, references);
    }
  }

  private void collectSelectStringReferences(String rawSelect, Set<String> references) {
    for (String item : rawSelect.split(",")) {
      String field = item.trim();
      if (field.isBlank() || "*".equals(field)) {
        continue;
      }
      String[] aliasParts = field.split("(?i)( as | )");
      String candidate = aliasParts.length > 0 ? aliasParts[0].trim() : field;
      if (isFieldPath(candidate)) {
        references.add(candidate);
      }
    }
  }

  private void collectPopulateReferences(Object raw, Set<String> references) {
    if (raw instanceof String fields) {
      for (String field : fields.split(",")) {
        String trimmed = field.trim();
        if (!trimmed.isBlank()) {
          references.add(trimmed);
        }
      }
      return;
    }
    if (raw instanceof Collection<?> values) {
      values.forEach(value -> collectPopulateReferences(value, references));
      return;
    }
    if (raw instanceof Map<?, ?> values) {
      for (Object key : values.keySet()) {
        if (key instanceof String field && !field.isBlank()) {
          references.add(field);
        }
      }
    }
  }

  private void collectWhereReferences(Object raw, Set<String> references) {
    if (raw instanceof Collection<?> values) {
      values.forEach(value -> collectWhereReferences(value, references));
      return;
    }
    if (!(raw instanceof Map<?, ?> values)) {
      return;
    }
    for (Map.Entry<?, ?> entry : values.entrySet()) {
      if (!(entry.getKey() instanceof String key)) {
        continue;
      }
      if (isLogicalOperatorKey(key)) {
        collectLogicalCondition(key, entry.getValue(), references);
      } else if (key.startsWith("$")) {
        continue;
      } else {
        references.add(key);
      }
    }
  }

  private void collectLogicalCondition(String operator, Object value, Set<String> references) {
    collectWhereReferences(value, references);
  }

  private boolean isLogicalOperatorKey(String key) {
    String normalized = key.startsWith("$") ? key.substring(1) : key;
    return "and".equalsIgnoreCase(normalized)
        || "or".equalsIgnoreCase(normalized)
        || "not".equalsIgnoreCase(normalized)
        || "xor".equalsIgnoreCase(normalized)
        || "xnor".equalsIgnoreCase(normalized)
        || "nor".equalsIgnoreCase(normalized);
  }

  private boolean isFieldPath(String value) {
    return value.matches("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*");
  }

  private Set<String> modelAliases() {
    Set<String> aliases = new HashSet<>();
    addAlias(aliases, dataModel.getName());
    addAlias(aliases, dataModel.getRawName());
    addAlias(aliases, lowerCamel(dataModel.getName()));
    return aliases;
  }

  private void addAlias(Set<String> aliases, String alias) {
    if (alias != null && !alias.isBlank()) {
      aliases.add(alias);
    }
  }

  private boolean isKnownFieldPath(String reference, Set<String> knownFields, Set<String> modelAliases) {
    if (knownFields.contains(reference)) {
      return true;
    }
    String[] segments = reference.split("\\.");
    if (segments.length == 1) {
      return false;
    }
    if (modelAliases.contains(segments[0])) {
      return segments.length > 1 && knownFields.contains(segments[1]);
    }
    return knownFields.contains(segments[0]);
  }

  private String lowerCamel(String value) {
    if (value == null || value.isBlank()) {
      return value;
    }
    if (value.length() == 1) {
      return value.toLowerCase();
    }
    return Character.toLowerCase(value.charAt(0)) + value.substring(1);
  }
}
