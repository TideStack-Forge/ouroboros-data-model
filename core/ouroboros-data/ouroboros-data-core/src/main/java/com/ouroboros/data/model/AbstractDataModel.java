package com.ouroboros.data.model;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import io.vavr.control.Try;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.exception.InvalidStatementException;
import com.ouroboros.data.exception.PrimaryKeyValidationException;
import com.ouroboros.data.exception.StatementException;
import com.ouroboros.data.exception.ValidationException;
import com.ouroboros.data.record.Record;
import com.ouroboros.data.record.RecordList;
import com.ouroboros.data.util.DataMaps;
import com.ouroboros.data.util.DataStrings;

public abstract class AbstractDataModel implements DataModel {
  protected final DataModelMeta meta;
  private final Map<String, DataModelField> fieldMap;
  private final List<DataModelField> fields;
  private final List<DataModelField> primaryKeys;
  private final PrimaryKeyGenerator<?> primaryKeyGenerator;

  public AbstractDataModel(DataModelMeta meta) {
    this.meta = meta;
    this.fields = meta.getFields().stream()
        .map(fieldMeta -> new DefaultDataModelField(this, fieldMeta))
        .collect(Collectors.toList());
    this.fieldMap = this.fields.stream()
        .collect(() -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER), (map, field) -> map.put(field.getName(), field), Map::putAll);
    this.primaryKeys = meta.getPrimaryKeys().stream()
        .map(fieldMap::get)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    this.primaryKeyGenerator = PrimaryKeyGeneratorFactory.getPkGenerator(meta.getPrimaryKeyGenerator()).orElse(null);
  }

  @Override
  public List<DataModelField> getFields() {
    return fields;
  }

  @Override
  public Optional<DataModelField> getField(String name) {
    return Optional.ofNullable(fieldMap.get(name));
  }

  @Override
  public List<DataModelField> getPrimaryKeys() {
    return primaryKeys;
  }

  @Override
  public List<DataModelUniqueConstraintMeta> getUniqueConstraints() {
    return meta.getUniqueConstraints();
  }

  @Override
  public PrimaryKeyGenerator<?> getPrimaryKeyGenerator() {
    return primaryKeyGenerator;
  }

  @Override
  public MigrationStrategy getMigrationStrategy() {
    return meta.getMigrationStrategy();
  }

  @Override
  @JsonAnyGetter
  public Map<String, Object> getExtraProps() {
    return meta.getExtraProps();
  }

  @Override
  public Optional<Object> getExtraProp(String propName) {
    return meta.getExtraProp(propName);
  }

  @Override
  public <T> Optional<T> getExtraProp(Class<T> clazz, String propName) {
    return meta.getExtraProp(clazz, propName);
  }

  @Override
  public String getFormatVersion() {
    return meta.getFormatVersion();
  }

  @Override
  public String getSource() {
    return meta.getSource();
  }

  @Override
  public String getNamespace() {
    return meta.getNamespace();
  }

  @Override
  public String getName() {
    return meta.getName();
  }

  @Override
  public String getFullName() {
    return meta.getFullName();
  }

  @Override
  public String getLabel() {
    return meta.getLabel();
  }

  @Override
  public String getDescription() {
    return meta.getDescription();
  }

  @Override
  public String getRawName() {
    return StringUtils.isBlank(meta.getRawName())
        ? DataStrings.toSnakeCase(getFullName().replaceAll("\\.", "_"))
        : meta.getRawName();
  }


  @Override
  public Try<Record> get(Object id) {
    return get(id, Collections.emptyMap());
  }

  @Override
  public Try<Record> get(Object id, Map<String, Object> statement) {
    return buildIdPredicate(id)
        .map(m -> Collections.singletonMap(Keyword.WHERE.toString(), (Object) m))
        .map(m -> DataMaps.merge(m, statement == null ? Collections.emptyMap() : statement))
        .flatMap(this::query)
        .map(list -> list.isEmpty() ? null : list.get(0));
  }


  @Override
  public Try<RecordList> query(List<?> ids) {
    return buildIdsPredicate(ids)
        .map(m -> Collections.singletonMap(Keyword.WHERE.toString(), (Object) m))
        .flatMap(this::query);
  }

  @Override
  public Try<RecordList> query(List<String> select, Map<String, Object> where) {
    var rawQuery = new HashMap<String, Object>();
    rawQuery.put(Keyword.SELECT.toString(), select);
    rawQuery.put(Keyword.WHERE.toString(), where);
    return query(rawQuery);
  }

  @Override
  public Try<RecordList> query(List<String> select, Map<String, Object> where, Map<String, Object> orderBy) {
    var rawQuery = new HashMap<String, Object>();
    rawQuery.put(Keyword.SELECT.toString(), select);
    rawQuery.put(Keyword.WHERE.toString(), where);
    rawQuery.put(Keyword.ORDER.toString(), orderBy);
    return query(rawQuery);
  }

  @Override
  public Try<RecordList> query(List<String> select, Map<String, Object> where, Map<String, Object> orderBy, Integer offset, Integer limit) {
    var rawQuery = new HashMap<String, Object>();
    rawQuery.put(Keyword.SELECT.toString(), select);
    rawQuery.put(Keyword.WHERE.toString(), where);
    rawQuery.put(Keyword.ORDER.toString(), orderBy);
    rawQuery.put(Keyword.OFFSET.toString(), offset);
    rawQuery.put(Keyword.LIMIT.toString(), limit);
    return query(rawQuery);
  }

  @Override
  public Try<Long> update(Object id, Map<String, Object> data) {
    var predicateResult = buildIdPredicate(id);

    if (predicateResult.isFailure()) {
      var cause = predicateResult.getCause();
      if (cause instanceof StatementException) {
        return Try.failure(new InvalidStatementException("准备" + getFullName() + "根据ID更新数据的操作时发生错误", cause));
      }
      return Try.failure(cause);
    }

    return update(predicateResult.get(), data);
  }

  @Override
  public Try<Long> update(List<?> ids, Map<String, Object> data) {
    var predicateResult = buildIdsPredicate(ids);

    if (predicateResult.isFailure()) {
      var cause = predicateResult.getCause();
      if (cause instanceof StatementException) {
        return Try.failure(new InvalidStatementException("准备" + getFullName() + "模型的根据ID列表批量更新数据的操作时发生错误", cause));
      }
      return Try.failure(cause);
    }

    return update(predicateResult.get(), data);
  }


  @Override
  public Try<Record> insertOrUpdate(Map<String, Object> data) {
    List<DataModelField> primaryKeyFields = getPrimaryKeys();
    if (primaryKeyFields.size() > 1) {
      return Try.failure(new InvalidStatementException("联合主键不支持插入或更新"));
    }

    String primaryKeyName = primaryKeyFields.get(0).getName();
    if (ObjectUtils.isEmpty(data.get(primaryKeyName))) {
      return insert(data);
    } else {
      return get(data.get(primaryKeyName))
          .flatMap(record -> {
            if (record == null) {
              return insert(data);
            }

            if (Objects.equals(data, record)) {
              return Try.success(record);
            }

            return update(data.get(primaryKeyName), data).map(rows -> Record.of(data));
          });
    }
  }

  @Override
  public Try<RecordList> batchInsertOrUpdate(List<Map<String, Object>> dataList) {
    List<DataModelField> primaryKeyFields = getPrimaryKeys();
    if (primaryKeyFields.size() > 1) {
      return Try.failure(new InvalidStatementException("联合主键不支持批量插入或更新"));
    }

    String primaryKeyName = primaryKeyFields.get(0).getName();

    // 找出无主键的数据
    List<Map<String, Object>> insertData = dataList.stream()
        .filter(data -> ObjectUtils.isEmpty(data.get(primaryKeyName)))
        .collect(Collectors.toList());

    // 找出有主键的数据
    List<Map<String, Object>> havePrimaryKeyData = dataList.stream()
        .filter(data -> ObjectUtils.isNotEmpty(data.get(primaryKeyName)))
        .collect(Collectors.toList());

    // 取出数据中所有的主键，查出相应数据
    List<Object> primaryKeys = havePrimaryKeyData.stream()
        .map(data -> data.get(primaryKeyName))
        .collect(Collectors.toList());
    Try<RecordList> queryEither = query(primaryKeys);
    if (queryEither.isFailure()) {
      return queryEither;
    }
    RecordList databaseRecords = queryEither.get();

    // 从有主键列表中寻找数据库中没有的数据，将其加入到insertData列表中
    havePrimaryKeyData.stream()
        .filter(data -> databaseRecords.stream()
            .noneMatch(record -> Objects.equals(data.get(primaryKeyName), record.get(primaryKeyName)))
        )
        .forEach(insertData::add);

    // 从有主键列表中寻找数据库中存在但数据不一致的数据
    List<Map<String, Object>> updateData = havePrimaryKeyData.stream()
        .filter(data -> databaseRecords.stream().anyMatch(record ->
                Objects.equals(data.get(primaryKeyName), record.get(primaryKeyName)) && !Objects.equals(data, record)
            )
        ).collect(Collectors.toList());

    // 执行插入与更新
    return batchInsert(insertData).flatMap(insertRecords -> {
      Optional<Throwable> first = updateData.stream()
          .map(data -> update(data.get(primaryKeyName), data))
          .filter(Try::isFailure)
          .map(Try::getCause)
          .findFirst();

      return first
          .<Try<RecordList>>map(Try::failure)
          .orElseGet(() -> Try.success(RecordList.of(
              Stream.concat(
                  insertRecords.stream(),
                  updateData.stream()
              ).collect(Collectors.toList())
          )));

    });
  }

  @Override
  public Try<Long> delete(Object id) {
    return buildIdPredicate(id)
        .flatMap(this::delete);
  }

  @Override
  public Try<Long> delete(List<?> ids) {
    return buildIdsPredicate(ids)
        .flatMap(this::delete);
  }

  // 构造根据ID查询的条件
  protected Try<Map<String, Object>> buildIdPredicate(Object id) {
    return DataModelPredicateBuilder.buildIdPredicate(this, id);
  }

  // 构造根据ID列表查询的条件
  protected Try<Map<String, Object>> buildIdsPredicate(List<?> id) {
    return DataModelPredicateBuilder.buildIdsPredicate(this, id);
  }

  private List<String> checkRules(DataModelField field, Object valueToCheck) {
    var errorLabel = Optional.ofNullable(field.getLabel()).filter(StringUtils::isNotEmpty).orElseGet(field::getName);
    var rules = field.getRules();
    return rules.stream()
        .filter(r -> !r.validate(valueToCheck))
        .map(r -> r.getFailMessage(errorLabel))
        .collect(Collectors.toList());
  }

  private Predicate<DataModelField> skipPrimaryKeyFields() {
    var primaryKeys = getPrimaryKeys();
    return field -> !primaryKeys.contains(field);
  }

  private Predicate<DataModelField> skipVirtualFields() {
    return field -> field.getValueType().isPhysical();
  }

  // 检查构造用于插入的Record
  protected Try<Record> buildInsertRecord(Map<String, Object> data) {
    // 错误列表
    var errorMessageMap = new LinkedHashMap<String, List<String>>();
    var record = new LinkedHashMap<String, Object>();

    // 先写入非主键字段（避免在有主键生成器的情况下，其他字段有问题的时候仍然生成主键）
    getFields().stream()
        .filter(skipPrimaryKeyFields())
        .filter(skipVirtualFields())
        .<Consumer<Map<String, Object>>>forEach(field -> {
          var fieldName = field.getName();
          var rawValue = Optional.ofNullable(data.get(fieldName)).orElseGet(() -> field.getDefaultValue(data));
          var storeValue = field.getValueType().toPersistentValue(rawValue);
          var errors = checkRules(field, rawValue);
          if (!errors.isEmpty()) {
            errorMessageMap.put(fieldName, errors);
          }

          if (storeValue != null) {
            record.put(fieldName, storeValue);
          }
        });

    // 如果存在错误，提前返回错误
    if (!errorMessageMap.isEmpty()) {
      return Try.failure(new ValidationException(errorMessageMap));
    }

    // 再写入主键
    List<DataModelField> primaryKeyFields = getPrimaryKeys();
    primaryKeyFields.forEach(pkField -> {
      if (data.containsKey(pkField.getName())) {
        record.put(pkField.getName(), pkField.getValueType().convertToPersistentValue(data.get(pkField.getName())));
      }
    });

    // 检查主键
    if (primaryKeyFields.size() == 1) {
      DataModelField pkField = primaryKeyFields.get(0);
      // 如果是自增主键，则不写入主键
      if (pkField.getIsAutoIncrement()) {
        if (ObjectUtils.isEmpty(record.get(pkField.getName()))) {
          record.remove(pkField.getName());
        }
      } else {
        // 如果未设置主键，使用主键生成器生成主键
        Object primaryKeyValue = record.get(pkField.getName());
        if (ObjectUtils.isEmpty(primaryKeyValue) && getPrimaryKeyGenerator() != null) {
          primaryKeyValue = getPrimaryKeyGenerator().next(record);
        }

        // 主键不能为空
        if (ObjectUtils.isEmpty(primaryKeyValue)) {
          return Try.failure(new PrimaryKeyValidationException("主键不能为空", Collections.singletonList(pkField.getName())));
        }

        record.put(pkField.getName(), pkField.getValueType().convertToPersistentValue(primaryKeyValue));
      }
    }
    // 复合主键
    else if (primaryKeyFields.size() > 1) {
      // 检查联合主键是否完整
      var emptyKeys = primaryKeyFields.stream()
          .filter(f -> ObjectUtils.isEmpty(record.get(f.getName())))
          .map(DataModelField::getName)
          .collect(Collectors.toList());
      if (!emptyKeys.isEmpty()) {
        return Try.failure(new PrimaryKeyValidationException("联合主键不完整: " + String.join(",", emptyKeys) + " 为空", emptyKeys));
      }
    }

    return Try.success(Record.of(record));
  }

  // 检查构造用于更新的Record
  protected Try<Record> buildUpdateRecord(Map<String, Object> data) {
    var errorMessageMap = new LinkedHashMap<String, List<String>>();
    var record = new LinkedHashMap<String, Object>();

    data.keySet().stream()
        .map(this::getField)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .filter(skipPrimaryKeyFields())
        .filter(skipVirtualFields())
        .forEach(field -> {
          var fieldName = field.getName();
          var rawValue = data.get(fieldName);
          var storeValue = field.getValueType().toPersistentValue(rawValue);
          var errors = checkRules(field, rawValue);
          if (!errors.isEmpty()) {
            errorMessageMap.put(fieldName, errors);
          }
          record.put(fieldName, storeValue);
        });

    if (errorMessageMap.isEmpty()) {
      return Try.success(Record.of(record));
    }

    return Try.failure(new ValidationException(errorMessageMap));
  }

  @Override
  public abstract Try<Record> insert(Map<String, Object> data);

  @Override
  public abstract Try<RecordList> batchInsert(List<Map<String, Object>> dataList);

  @Override
  public abstract Try<Long> update(Map<String, Object> where, Map<String, Object> data);

  @Override
  public abstract Try<Long> delete(Map<String, Object> where);

  @Override
  public abstract Try<Long> count(Map<String, Object> where);

  @Override
  public abstract Try<RecordList> query(Map<String, Object> statement);

  @Override
  public DataModel withPlugins(Collection<PluginDescriptor> pluginDescriptors) {
    return new EnhancedDataModelProxy(this, pluginDescriptors);
  }

  @Override
  public boolean hasPlugin(String name) {
    return false;
  }

  @Override
  public DataModel withoutPlugins() {
    return this;
  }

  @Override
  public DataModel withoutPlugins(Collection<String> pluginNames) {
    return this;
  }


}
