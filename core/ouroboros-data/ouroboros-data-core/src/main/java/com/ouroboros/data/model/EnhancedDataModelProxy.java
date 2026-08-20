package com.ouroboros.data.model;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import io.vavr.control.Try;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.ouroboros.data.adapter.DataAdapter;
import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.ModelQueryStatement;
import com.ouroboros.data.dsl.statement.ModelQueryStatementBuilder;
import com.ouroboros.data.dsl.statement.OmitClause;
import com.ouroboros.data.dsl.statement.PopulateClause;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.exception.InvalidStatementException;
import com.ouroboros.data.exception.StatementException;
import com.ouroboros.data.normalize.QueryNormalizeContext;
import com.ouroboros.data.normalize.normalizers.PopulateOmitNormalizer;
import com.ouroboros.data.record.Record;
import com.ouroboros.data.record.RecordList;
import com.ouroboros.data.station.DataStation;
import com.ouroboros.data.util.DataMaps;
import com.ouroboros.data.util.DataServices;

/**
 * 增强的数据模型代理类
 * <p>
 * 为基础数据模型提供以下增强能力：
 * 1. 元数据装饰能力 - 通过 DataModelMetaDecorator 机制动态增强元数据
 * 2. 插件扩展能力 - 通过插件链模式提供可扩展的数据操作能力
 * <p>
 * 该代理类采用装饰器模式，将核心数据操作委托给内部的 DefaultDataModel，
 * 同时在其基础上提供插件机制和元数据装饰功能。
 *
 * @author Song Mingxu
 */
public class EnhancedDataModelProxy implements DataModel {

  private static final ThreadLocal<Deque<QueryInvocation>> QUERY_INVOCATIONS =
      ThreadLocal.withInitial(ArrayDeque::new);

  private final DataModel coreDataModel;
  private final List<PluginDescriptor> pluginDescriptors;
  private final List<DataModelPlugin> plugins;
  private final DataModelPlugin tailPlugin = new TailPlugin();
  private final QueryNormalizeContext typedQueryNormalizeContext = QueryNormalizeContext.builder()
      .withDefaultNormalizers()
      .addClauseNormalizer(new PopulateOmitNormalizer())
      .build();

  /**
   * 构造增强数据模型代理
   *
   * @param originalMeta     原始数据模型元数据
   * @param dataModelCreator 数据模型创建器
   */
  public EnhancedDataModelProxy(DataModelMeta originalMeta, Function<DataModelMeta, DataModel> dataModelCreator) {
    this(originalMeta, Collections.emptyList(), dataModelCreator);
  }

  /**
   * 构造增强数据模型代理
   *
   * @param originalMeta      原始数据模型元数据
   * @param pluginDescriptors 额外的插件描述符列表
   * @param dataModelCreator  数据模型创建器
   */
  public EnhancedDataModelProxy(DataModelMeta originalMeta, Collection<PluginDescriptor> pluginDescriptors, Function<DataModelMeta, DataModel> dataModelCreator) {
    var decoratedMeta = new ImmutableDataModelMeta(DataModelMetaDecorator.applyDecorators(originalMeta));
    this.pluginDescriptors = new ArrayList<>();
    if (ObjectUtils.isNotEmpty(decoratedMeta.getPluginDescriptors())) {
      this.pluginDescriptors.addAll(decoratedMeta.getPluginDescriptors());
    }
    if (ObjectUtils.isNotEmpty(pluginDescriptors)) {
      this.pluginDescriptors.addAll(pluginDescriptors.stream()
          .map(p -> p instanceof ImmutableDataModelMeta.ImmutablePluginDescriptor
              ? p
              : new ImmutableDataModelMeta.ImmutablePluginDescriptor(p))
          .collect(Collectors.toList()));
    }
    var createdCoreDataModel = dataModelCreator.apply(decoratedMeta);
    DataModelValidators.validate(createdCoreDataModel);
    this.coreDataModel = createdCoreDataModel;
    this.plugins = initializePlugins();
  }

  /**
   * EnhancedDataModelProxy的构造函数，用于初始化数据模型代理对象
   *
   * @param rawDataModel      原始数据模型对象，如果上面已经有插件了，原来的插件也会生效，如果需要重新赋予插件列表，需要调用方调用 withoutPlugins() 再传入
   * @param pluginDescriptors 插件描述符集合
   */
  public EnhancedDataModelProxy(DataModel rawDataModel, Collection<PluginDescriptor> pluginDescriptors) {
    // 初始化核心数据模型
    this.coreDataModel = rawDataModel;
    // 初始化插件描述符列表
    this.pluginDescriptors = new ArrayList<>();
    // 如果插件描述符集合不为空，则将其全部添加到插件描述符列表中
    if (ObjectUtils.isNotEmpty(pluginDescriptors)) {
      this.pluginDescriptors.addAll(pluginDescriptors.stream()
          .map(p -> p instanceof ImmutableDataModelMeta.ImmutablePluginDescriptor
              ? p
              : new ImmutableDataModelMeta.ImmutablePluginDescriptor(p))
          .collect(Collectors.toList()));
    }
    // 初始化插件链
    this.plugins = initializePlugins();
  }

  /**
   * 初始化插件链
   */
  private List<DataModelPlugin> initializePlugins() {
    if (this.pluginDescriptors.isEmpty()) {
      return Collections.emptyList();
    }

    // 加载插件构建器
    var builders = DataServices.getCachedReversedServiceStream(DataModelPluginBuilder.class)
        .collect(Collectors.toList());

    // 构建插件
    return this.pluginDescriptors.stream()
        .map(pluginDescriptor -> buildPlugin(builders, pluginDescriptor))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  /**
   * 构建单个插件
   */
  private Optional<DataModelPlugin> buildPlugin(List<DataModelPluginBuilder> builders, PluginDescriptor pluginDescriptor) {
    return builders.stream()
        .filter(builder -> builder.support(pluginDescriptor.getName()))
        .map(builder -> builder.build(this, pluginDescriptor.getConfig()))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();
  }

  // ================== DataModel 接口实现：元数据相关方法 ==================

  @Override
  public List<DataModelField> getFields() {
    return coreDataModel.getFields();
  }

  @Override
  public Optional<DataModelField> getField(String name) {
    return coreDataModel.getField(name);
  }

  @Override
  public List<DataModelField> getPrimaryKeys() {
    return coreDataModel.getPrimaryKeys();
  }

  @Override
  public List<DataModelUniqueConstraintMeta> getUniqueConstraints() {
    return coreDataModel.getUniqueConstraints();
  }

  @Override
  public PrimaryKeyGenerator<?> getPrimaryKeyGenerator() {
    return coreDataModel.getPrimaryKeyGenerator();
  }

  @Override
  public String getNamespace() {
    return coreDataModel.getNamespace();
  }

  @Override
  public String getName() {
    return coreDataModel.getName();
  }

  @Override
  public String getFullName() {
    return coreDataModel.getFullName();
  }

  @Override
  public String getLabel() {
    return coreDataModel.getLabel();
  }

  @Override
  public String getDescription() {
    return coreDataModel.getDescription();
  }

  @Override
  public String getRawName() {
    return coreDataModel.getRawName();
  }

  @Override
  public String getFormatVersion() {
    return coreDataModel.getFormatVersion();
  }

  @Override
  public String getSource() {
    return coreDataModel.getSource();
  }

  @Override
  @JsonAnyGetter
  public Map<String, Object> getExtraProps() {
    return coreDataModel.getExtraProps();
  }

  @Override
  public Optional<Object> getExtraProp(String propName) {
    return coreDataModel.getExtraProp(propName);
  }

  @Override
  public <T> Optional<T> getExtraProp(Class<T> clazz, String propName) {
    return coreDataModel.getExtraProp(clazz, propName);
  }

  @Override
  public MigrationStrategy getMigrationStrategy() {
    return coreDataModel.getMigrationStrategy();
  }

  @Override
  public DataAdapter getAdapter() {
    return coreDataModel.getAdapter();
  }

  @Override
  public DataStation<?> getDataStation() {
    return coreDataModel.getDataStation();
  }

  // ================== DataModel 接口实现：数据操作方法（带插件支持）==================

  @Override
  public Try<Record> insert(Map<String, Object> data) {
    return plugins.isEmpty()
        ? coreDataModel.insert(data)
        : plugins.get(0).insert(data, pluginContext(1));
  }

  @Override
  public Try<RecordList> batchInsert(List<Map<String, Object>> dataList) {
    return plugins.isEmpty()
        ? coreDataModel.batchInsert(dataList)
        : plugins.get(0).batchInsert(dataList, pluginContext(1));
  }

  @Override
  public Try<Long> update(Map<String, Object> where, Map<String, Object> data) {
    return typedQueryNormalizeContext.forClause("WHERE")
        .normalizeCondition(where, "root")
        .flatMap(condition -> update(condition, data));
  }

  @Override
  public Try<Long> update(SExpression<Boolean> where, Map<String, Object> data) {
    return plugins.isEmpty()
        ? coreDataModel.update(where, data)
        : plugins.get(0).update(where, data, pluginContext(1));
  }

  @Override
  public Try<Long> delete(Map<String, Object> where) {
    return typedQueryNormalizeContext.forClause("WHERE")
        .normalizeCondition(where, "root")
        .flatMap(this::delete);
  }

  @Override
  public Try<Long> delete(SExpression<Boolean> where) {
    return plugins.isEmpty()
        ? coreDataModel.delete(where)
        : plugins.get(0).delete(where, pluginContext(1));
  }

  @Override
  public Try<Long> count(Map<String, Object> where) {
    Map<String, Object> queryMap = new LinkedHashMap<>();
    queryMap.put(Keyword.FROM.toString(), getFullName());
    queryMap.put(Keyword.WHERE.toString(), where);
    return typedQueryNormalizeContext.normalizeQuery(queryMap)
        .flatMap(this::count);
  }

  @Override
  public Try<Long> count(QueryStatement statement) {
    return Try.of(() -> new TypedQueryProvenance(statement.getFrom(), statement))
        .flatMap(provenance -> queryWithinInvocation(
            provenance,
            () -> plugins.isEmpty()
                ? coreDataModel.count(statement)
                : plugins.get(0).count(statement, pluginContext(1))));
  }

  @Override
  public Try<RecordList> query(Map<String, Object> statement) {
    Map<String, Object> queryMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    queryMap.putAll(statement);
    queryMap.put(Keyword.FROM.toString(), Collections.singletonMap(getFullName(), getRawName()));
    return typedQueryNormalizeContext.normalizeQuery(queryMap, new ModelQueryStatementBuilder())
        .flatMap(queryStatement -> Try.of(() -> new TypedQueryProvenance(queryStatement.getFrom(), queryStatement))
            .flatMap(provenance -> queryWithinInvocation(
                provenance,
                () -> plugins.isEmpty()
                    ? coreDataModel.query(queryStatement)
                    : plugins.get(0).query(queryStatement, pluginContext(1)))));
  }

  @Override
  public Try<RecordList> query(QueryStatement statement) {
    return Try.of(() -> new TypedQueryProvenance(statement.getFrom(), statement))
        .flatMap(provenance -> queryWithinInvocation(
            provenance,
            () -> plugins.isEmpty()
                ? coreDataModel.query(statement)
                : plugins.get(0).query(statement, pluginContext(1))));
  }

  private <T> Try<T> queryWithinInvocation(
      TypedQueryProvenance provenance,
      Supplier<Try<T>> query) {
    Deque<QueryInvocation> invocationStack = QUERY_INVOCATIONS.get();
    invocationStack.push(new QueryInvocation(this, provenance));
    try {
      return query.get();
    } finally {
      invocationStack.pop();
      if (invocationStack.isEmpty()) {
        QUERY_INVOCATIONS.remove();
      }
    }
  }

  private TypedQueryProvenance currentOwnedTypedQueryProvenance() {
    Deque<QueryInvocation> invocationStack = QUERY_INVOCATIONS.get();
    if (invocationStack.isEmpty() || invocationStack.peek().owner() != this) {
      return null;
    }
    return invocationStack.peek().provenance();
  }

  private DataModelPluginContext pluginContext(int nextPluginIndex) {
    return new DataModelPluginContext() {
      @Override
      public DataModel getDataModel() {
        return EnhancedDataModelProxy.this;
      }

      @Override
      public DataModel getCoreDataModel() {
        return coreDataModel;
      }

      @Override
      public DataModelPlugin getNextPlugin() {
        return nextPluginIndex < plugins.size()
            ? plugins.get(nextPluginIndex)
            : tailPlugin;
      }

      @Override
      public DataModelPluginContext getNextPluginContext() {
        return pluginContext(nextPluginIndex + 1);
      }
    };
  }

  // ================== DataModel 接口实现：其他方法 ==================

  @Override
  public Try<Record> get(Object id) {
    return get(id, Collections.emptyMap());
  }

  @Override
  public Try<Record> get(Object id, Map<String, Object> statement) {
    return DataModelPredicateBuilder.buildIdPredicate(this, id)
        .map(where -> Collections.singletonMap(Keyword.WHERE.toString(), (Object) where))
        .map(query -> DataMaps.merge(query, statement == null ? Collections.emptyMap() : statement))
        .flatMap(this::query)
        .map(records -> records.isEmpty() ? null : records.get(0));
  }

  @Override
  public Try<RecordList> query(List<?> ids) {
    return DataModelPredicateBuilder.buildIdsPredicate(this, ids)
        .map(where -> Collections.singletonMap(Keyword.WHERE.toString(), (Object) where))
        .flatMap(this::query);
  }

  @Override
  public Try<RecordList> query(List<String> select, Map<String, Object> where) {
    var statement = new HashMap<String, Object>();
    statement.put(Keyword.SELECT.toString(), select);
    statement.put(Keyword.WHERE.toString(), where);
    return query(statement);
  }

  @Override
  public Try<RecordList> query(List<String> select, Map<String, Object> where, Map<String, Object> orderBy) {
    var statement = new HashMap<String, Object>();
    statement.put(Keyword.SELECT.toString(), select);
    statement.put(Keyword.WHERE.toString(), where);
    statement.put(Keyword.ORDER.toString(), orderBy);
    return query(statement);
  }

  @Override
  public Try<RecordList> query(List<String> select, Map<String, Object> where, Map<String, Object> orderBy, Integer offset, Integer limit) {
    var statement = new HashMap<String, Object>();
    statement.put(Keyword.SELECT.toString(), select);
    statement.put(Keyword.WHERE.toString(), where);
    statement.put(Keyword.ORDER.toString(), orderBy);
    statement.put(Keyword.OFFSET.toString(), offset);
    statement.put(Keyword.LIMIT.toString(), limit);
    return query(statement);
  }

  @Override
  public Try<Long> update(Object id, Map<String, Object> data) {
    var whereResult = DataModelPredicateBuilder.buildIdPredicate(this, id);

    if (whereResult.isFailure()) {
      var cause = whereResult.getCause();
      if (cause instanceof StatementException) {
        return Try.failure(new InvalidStatementException("准备" + getFullName() + "根据ID更新数据的操作时发生错误", cause));
      }
      return Try.failure(cause);
    }

    return update(whereResult.get(), data);
  }

  @Override
  public Try<Long> update(List<?> ids, Map<String, Object> data) {
    var whereResult = DataModelPredicateBuilder.buildIdsPredicate(this, ids);

    if (whereResult.isFailure()) {
      var cause = whereResult.getCause();
      if (cause instanceof StatementException) {
        return Try.failure(new InvalidStatementException("准备" + getFullName() + "模型的根据ID列表批量更新数据的操作时发生错误", cause));
      }
      return Try.failure(cause);
    }

    return update(whereResult.get(), data);
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

    List<Map<String, Object>> insertData = dataList.stream()
        .filter(data -> ObjectUtils.isEmpty(data.get(primaryKeyName)))
        .collect(Collectors.toList());

    List<Map<String, Object>> havePrimaryKeyData = dataList.stream()
        .filter(data -> ObjectUtils.isNotEmpty(data.get(primaryKeyName)))
        .collect(Collectors.toList());

    List<Object> primaryKeys = havePrimaryKeyData.stream()
        .map(data -> data.get(primaryKeyName))
        .collect(Collectors.toList());
    Try<RecordList> queryEither = query(primaryKeys);
    if (queryEither.isFailure()) {
      return queryEither;
    }
    RecordList databaseRecords = queryEither.get();

    havePrimaryKeyData.stream()
        .filter(data -> databaseRecords.stream()
            .noneMatch(record -> Objects.equals(data.get(primaryKeyName), record.get(primaryKeyName)))
        )
        .forEach(insertData::add);

    List<Map<String, Object>> updateData = havePrimaryKeyData.stream()
        .filter(data -> databaseRecords.stream().anyMatch(record ->
            Objects.equals(data.get(primaryKeyName), record.get(primaryKeyName)) && !Objects.equals(data, record)
        ))
        .collect(Collectors.toList());

    return batchInsert(insertData).flatMap(insertRecords -> {
      Optional<Throwable> first = updateData.stream()
          .map(updateDataItem -> update(updateDataItem.get(primaryKeyName), updateDataItem))
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
    return DataModelPredicateBuilder.buildIdPredicate(this, id)
        .flatMap(this::delete);
  }

  @Override
  public Try<Long> delete(List<?> ids) {
    return DataModelPredicateBuilder.buildIdsPredicate(this, ids)
        .flatMap(this::delete);
  }

  // ================== 插件管理方法 ==================

  @Override
  public DataModel withPlugins(Collection<PluginDescriptor> pluginDescriptors) {
    if (pluginDescriptors.isEmpty()) {
      return this;
    }

    // 合并插件描述符
    var newDescriptors = Stream.concat(pluginDescriptors.stream(), this.pluginDescriptors.stream())
        .collect(Collectors.toList());

    return new EnhancedDataModelProxy(coreDataModel, newDescriptors);
  }

  @Override
  public boolean hasPlugin(String name) {
    return pluginDescriptors.stream()
        .filter(descriptor -> StringUtils.isNotBlank(descriptor.getName()))
        .anyMatch(descriptor -> descriptor.getName().equalsIgnoreCase(name));
  }

  @Override
  public DataModel withoutPlugins() {
    if (pluginDescriptors.isEmpty()) {
      return this;
    }

    return new EnhancedDataModelProxy(coreDataModel, Collections.emptyList());
  }

  @Override
  public DataModel withoutPlugins(Collection<String> pluginNames) {
    if (pluginNames.isEmpty()) {
      return this;
    }

    var newPluginDescriptors = pluginDescriptors.stream()
        .filter(pluginDescriptor -> pluginNames.stream()
            .noneMatch(pluginName -> pluginName.equalsIgnoreCase(pluginDescriptor.getName())))
        .collect(Collectors.toList());

    return new EnhancedDataModelProxy(coreDataModel, newPluginDescriptors);
  }

  /**
   * 获取核心数据模型
   */
  public DataModel getCoreDataModel() {
    return coreDataModel;
  }

  /**
   * 插件链尾部节点，负责将请求委托给核心数据模型
   */
  private class TailPlugin implements DataModelPlugin {
    @Override
    public Try<Record> insert(Map<String, Object> data, DataModelPluginContext context) {
      return coreDataModel.insert(data);
    }

    @Override
    public Try<RecordList> batchInsert(List<Map<String, Object>> dataList, DataModelPluginContext context) {
      return coreDataModel.batchInsert(dataList);
    }

    @Override
    public Try<Long> update(SExpression<Boolean> where, Map<String, Object> data, DataModelPluginContext context) {
      return coreDataModel.update(where, data);
    }

    @Override
    public Try<Long> delete(SExpression<Boolean> where, DataModelPluginContext context) {
      return coreDataModel.delete(where);
    }

    @Override
    public Try<Long> count(QueryStatement statement, DataModelPluginContext context) {
      var validation = validatePluginQueryStatement(statement);
      if (validation.isFailure()) {
        return Try.failure(validation.getCause());
      }
      return coreDataModel.count(statement);
    }

    @Override
    public Try<RecordList> query(QueryStatement statement, DataModelPluginContext context) {
      var validation = validatePluginQueryStatement(statement);
      if (validation.isFailure()) {
        return Try.failure(validation.getCause());
      }
      return coreDataModel.query(statement);
    }

    private Try<QueryStatement> validatePluginQueryStatement(QueryStatement statement) {
      TypedQueryProvenance provenance = currentOwnedTypedQueryProvenance();
      if (provenance == null) {
        return Try.success(statement);
      }
      if (statement.getFrom() != provenance.from) {
        return Try.failure(new InvalidStatementException(
            "配置数据模型插件时，不支持改写 typed 查询的根 FROM"));
      }
      if (provenance.hasRootSubqueryChanged(statement)) {
        return Try.failure(new InvalidStatementException(
            "配置数据模型插件时，不支持改写以子查询作为根 FROM 的查询条件或根子查询"));
      }
      return Try.success(statement);
    }
  }

  private static final class TypedQueryProvenance {
    private final Object from;
    private final Object whereSnapshot;
    private final Object rootFromSnapshot;

    private TypedQueryProvenance(Object from, QueryStatement canonical) {
      this.from = from;
      if (from instanceof QueryStatement.TableSource tableSource
          && tableSource.isSubQuery()
          && canonical != null) {
        this.whereSnapshot = snapshotQueryValue(canonical.getWhere());
        this.rootFromSnapshot = snapshotQueryValue(canonical.getFrom());
      } else {
        this.whereSnapshot = null;
        this.rootFromSnapshot = null;
      }
    }

    private boolean hasRootSubqueryChanged(QueryStatement normalized) {
      if (rootFromSnapshot == null) {
        return false;
      }
      return !Objects.equals(whereSnapshot, snapshotQueryValue(normalized.getWhere()))
          || !Objects.equals(rootFromSnapshot, snapshotQueryValue(normalized.getFrom()));
    }

    private static Object snapshotQueryValue(Object value) {
      return snapshotQueryValue(value, new IdentityHashMap<>());
    }

    private static Object snapshotQueryValue(Object value, IdentityHashMap<Object, Integer> seen) {
      if (value == null) {
        return new QueryValueSnapshot("null", null);
      }
      if (value instanceof String || value instanceof Boolean || value instanceof Character) {
        return new QueryValueSnapshot(value.getClass().getName(), value);
      }
      if (value instanceof Byte
          || value instanceof Short
          || value instanceof Integer
          || value instanceof Long
          || value instanceof Float
          || value instanceof Double) {
        return new QueryValueSnapshot(value.getClass().getName(), value);
      }
      if (value.getClass() == BigInteger.class || value.getClass() == BigDecimal.class) {
        return new QueryValueSnapshot(value.getClass().getName(), value);
      }
      if (value.getClass() == AtomicInteger.class) {
        AtomicInteger atomicInteger = (AtomicInteger) value;
        return new QueryValueSnapshot(AtomicInteger.class.getName(), atomicInteger.get());
      }
      if (value.getClass() == AtomicLong.class) {
        AtomicLong atomicLong = (AtomicLong) value;
        return new QueryValueSnapshot(AtomicLong.class.getName(), atomicLong.get());
      }
      if (value instanceof Number) {
        throw unsupportedSnapshotValue(value);
      }
      if (value instanceof Enum<?> enumValue) {
        return new QueryValueSnapshot(value.getClass().getName(), enumValue.name());
      }
      if (value instanceof Class<?> type) {
        return new QueryValueSnapshot(Class.class.getName(), type.getName());
      }
      if (value.getClass() == Date.class
          || value.getClass() == java.sql.Date.class
          || value.getClass() == java.sql.Time.class) {
        Date date = (Date) value;
        return new QueryValueSnapshot(
            value.getClass().getName(),
            date.getTime());
      }
      if (value.getClass() == java.sql.Timestamp.class) {
        java.sql.Timestamp timestamp = (java.sql.Timestamp) value;
        return new QueryValueSnapshot(
            java.sql.Timestamp.class.getName(),
            Arrays.asList(timestamp.getTime(), timestamp.getNanos()));
      }
      if (value instanceof Date) {
        throw unsupportedSnapshotValue(value);
      }
      if (isSupportedJavaTimeValue(value) || value instanceof UUID) {
        return new QueryValueSnapshot(value.getClass().getName(), value.toString());
      }

      Integer reference = seen.get(value);
      if (reference != null) {
        return new QueryValueSnapshot("reference", reference);
      }
      seen.put(value, seen.size());

      if (value instanceof SExpression<?> expression) {
        return new QueryValueSnapshot("expression", Arrays.asList(
            snapshotQueryValue(expression.getOperator(), seen),
            expression.getDataType(),
            snapshotQueryValue(expression.getParams(), seen)));
      }
      if (value instanceof QueryStatement statement) {
        Map<String, Object> clauses = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        statement.forEach((key, nested) -> clauses.put(
            key,
            snapshotQueryValue(nested, seen)));
        List<Object> statementSnapshot = new ArrayList<>();
        statementSnapshot.add(statement.getClass().getName());
        statementSnapshot.add(clauses);
        if (statement instanceof ModelQueryStatement modelStatement) {
          statementSnapshot.add(snapshotQueryValue(modelStatement.getPopulateClause(), seen));
          statementSnapshot.add(snapshotQueryValue(modelStatement.getOmitClause(), seen));
        }
        return new QueryValueSnapshot("query", statementSnapshot);
      }
      if (value instanceof QueryStatement.TableSource tableSource) {
        return new QueryValueSnapshot("from", Arrays.asList(
            tableSource.getAlias(),
            snapshotQueryValue(tableSource.getOrigin(), seen)));
      }
      if (value instanceof QueryStatement.JoinEntry joinEntry) {
        return new QueryValueSnapshot("join", Arrays.asList(
            joinEntry.getType(),
            joinEntry.getAlias(),
            snapshotQueryValue(joinEntry.getOrigin(), seen),
            snapshotQueryValue(joinEntry.getOn(), seen)));
      }
      if (value instanceof QueryStatement.CTEDefinition cte) {
        return new QueryValueSnapshot("cte", Arrays.asList(
            cte.getAlias(),
            cte.isRecursive(),
            snapshotQueryValue(cte.getQuery(), seen)));
      }
      if (value instanceof QueryStatement.UnionEntry unionEntry) {
        return new QueryValueSnapshot("union", Arrays.asList(
            unionEntry.isAll(),
            snapshotQueryValue(unionEntry.getQuery(), seen)));
      }
      if (value instanceof QueryStatement.OrderEntry orderEntry) {
        return new QueryValueSnapshot("order", Arrays.asList(
            orderEntry.getColumn(),
            orderEntry.getOrder()));
      }
      if (value instanceof PopulateClause populateClause) {
        return new QueryValueSnapshot(
            "populate",
            snapshotQueryValue(populateClause.getEntries(), seen));
      }
      if (value instanceof PopulateClause.PopulateEntry populateEntry) {
        return new QueryValueSnapshot("populate-entry", Arrays.asList(
            populateEntry.fieldName(),
            snapshotQueryValue(populateEntry.options(), seen)));
      }
      if (value instanceof OmitClause omitClause) {
        return new QueryValueSnapshot(
            "omit",
            snapshotQueryValue(omitClause.getFields(), seen));
      }
      if (value instanceof Map<?, ?> map) {
        List<Object> entries = new ArrayList<>(map.size());
        map.forEach((key, nested) -> entries.add(Arrays.asList(
            snapshotMapKey(key),
            snapshotQueryValue(nested, seen))));
        return new QueryValueSnapshot("map", entries);
      }
      if (value instanceof Collection<?> collection) {
        List<Object> snapshot = new ArrayList<>(collection.size());
        collection.forEach(item -> snapshot.add(snapshotQueryValue(item, seen)));
        return new QueryValueSnapshot(value instanceof Set<?> ? "set" : "collection", snapshot);
      }
      if (value.getClass().isArray()) {
        List<Object> snapshot = new ArrayList<>(Array.getLength(value));
        for (int index = 0; index < Array.getLength(value); index++) {
          snapshot.add(snapshotQueryValue(Array.get(value, index), seen));
        }
        return new QueryValueSnapshot(
            "array:" + value.getClass().getComponentType().getName(),
            snapshot);
      }
      throw unsupportedSnapshotValue(value);
    }

    private static Object snapshotMapKey(Object key) {
      if (key == null
          || key instanceof String
          || key instanceof Boolean
          || key instanceof Character
          || key instanceof Byte
          || key instanceof Short
          || key instanceof Integer
          || key instanceof Long
          || key instanceof Float
          || key instanceof Double
          || key instanceof Enum<?>
          || key instanceof Class<?>
          || key instanceof UUID
          || key.getClass() == BigInteger.class
          || key.getClass() == BigDecimal.class
          || isSupportedJavaTimeValue(key)) {
        return snapshotQueryValue(key, new IdentityHashMap<>());
      }
      throw new InvalidStatementException(
          "根 FROM 为子查询时，不支持非稳定标量类型的 Map key: "
              + key.getClass().getName());
    }

    private static boolean isSupportedJavaTimeValue(Object value) {
      Class<?> type = value.getClass();
      return type == Duration.class
          || type == Instant.class
          || type == LocalDate.class
          || type == LocalDateTime.class
          || type == LocalTime.class
          || type == MonthDay.class
          || type == OffsetDateTime.class
          || type == OffsetTime.class
          || type == Period.class
          || type == Year.class
          || type == YearMonth.class
          || type == ZonedDateTime.class
          || value instanceof ZoneId;
    }

    private static InvalidStatementException unsupportedSnapshotValue(Object value) {
      return new InvalidStatementException(
          "根 FROM 为子查询时，不支持无法安全比较的查询值类型: "
              + value.getClass().getName());
    }
  }

  private record QueryInvocation(
      EnhancedDataModelProxy owner,
      TypedQueryProvenance provenance) {}

  private record QueryValueSnapshot(String kind, Object value) {}
}
