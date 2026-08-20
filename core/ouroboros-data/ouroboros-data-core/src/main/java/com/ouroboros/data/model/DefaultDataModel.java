package com.ouroboros.data.model;

import static com.ouroboros.data.dsl.StatementPredicates.isBasicType;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vavr.Tuple;
import io.vavr.control.Try;

import com.ouroboros.data.adapter.DataAdapter;
import com.ouroboros.data.analyze.*;
import com.ouroboros.data.dsl.DMLStatements;
import com.ouroboros.data.dsl.ModelFieldPath;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.ModelQueryStatement;
import com.ouroboros.data.dsl.statement.ModelQueryStatementBuilder;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.exception.*;
import com.ouroboros.data.normalize.QueryNormalizeContext;
import com.ouroboros.data.normalize.normalizers.PopulateOmitNormalizer;
import com.ouroboros.data.orchestration.DefaultQueryOrchestrator;
import com.ouroboros.data.orchestration.MainQueryExecutor;
import com.ouroboros.data.orchestration.OrchestrationContext;
import com.ouroboros.data.orchestration.QueryOrchestrator;
import com.ouroboros.data.record.DefaultRecordImpl;
import com.ouroboros.data.record.DefaultRecordListImpl;
import com.ouroboros.data.record.Record;
import com.ouroboros.data.record.RecordList;
import com.ouroboros.data.station.DataStation;
import com.ouroboros.data.transpile.BaseTranspileContext;
import com.ouroboros.data.transpile.FieldSource;
import com.ouroboros.data.transpile.TranspileContext;
import com.ouroboros.data.util.DataJson;
import com.querydsl.core.QueryMetadata;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.FactoryExpression;
import com.querydsl.core.types.Operation;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.sql.RelationalPathBase;

/**
 * 默认数据模型实现
 * <p>
 * 专注于核心数据操作逻辑，不包含插件机制和元数据装饰功能。
 * 这些增强功能由 EnhancedDataModelProxy 提供。
 *
 * @author Song Mingxu
 */
public class DefaultDataModel extends AbstractDataModel {
  private final static Logger logger = LoggerFactory.getLogger("DataModel");

  private final DataStation<?> dataStation;
  private final DataAdapter adapter;
  private final QueryOrchestrator orchestrator;
  private final QueryNormalizeContext sharedNormalizeContext;

  public DefaultDataModel(DataModelMeta meta, DataStation<?> dataStation) {
    super(meta);
    this.dataStation = dataStation;
    this.adapter = dataStation.getDataAdapter();
    this.orchestrator = new DefaultQueryOrchestrator();
    this.sharedNormalizeContext = QueryNormalizeContext.builder()
        .withDefaultNormalizers()
        .addClauseNormalizer(new PopulateOmitNormalizer())
        .build();
  }

  /**
   * 获取元数据
   */
  public DataModelMeta getModelMeta() {
    return meta;
  }

  // ================== DataModel 接口实现：元数据相关方法（继承自AbstractDataModel）==================

  @Override
  public DataAdapter getAdapter() {
    return adapter;
  }

  @Override
  public DataStation<?> getDataStation() {
    return dataStation;
  }

  // ================== 抽象方法实现：核心数据操作方法 ==================

  @Override
  public Try<Record> insert(Map<String, Object> data) {
    var insertRecordResult = buildInsertRecord(data);
    if (insertRecordResult.isFailure()) {
      return insertRecordResult;
    }

    final Record insertRecord = insertRecordResult.get();

    // 创建 DML TranspileContext
    TranspileContext dmlContext = createTranspileContext();

    return DMLStatements.buildInsertStatement(getFullName(), insertRecord)
        .flatMap(insertStatement -> getAdapter().insert(insertStatement, dmlContext))
        .flatMap(recordPrimaryKey -> {
          if (!ObjectUtils.isEmpty(recordPrimaryKey)) {
            return get(recordPrimaryKey);
          }

          // 主键列表
          List<String> recordPrimaryKeys = new ArrayList<>();

          // 此处 recordPrimaryKey 只有依赖数据库生成才会有值，所以极有可能为null，需要在此兼容处理！
          if (ObjectUtils.isEmpty(recordPrimaryKey)) {
            // 获取所有主键列表
            getPrimaryKeys().stream()
                .map(DataModelField::getName)
                .forEach(recordPrimaryKeys::add);
          }

          var condition = ObjectUtils.isEmpty(recordPrimaryKeys)
              // 没有主键则直接使用原始数据进行查询
              ? insertRecord.entrySet().stream()
              .map(entry -> Tuple.of(entry.getKey(), entry.getValue() == null || isBasicType.test(entry.getValue()) ? entry.getValue() : DataJson.toJsonString(entry.getValue())))
              .collect(() -> new TreeMap<>(String::compareToIgnoreCase), (map, tuple) -> map.put(tuple._1(), tuple._2()), Map::putAll)
              // 有主键则将主键数据提取出来
              : insertRecord.keySet().stream()
              .filter(recordPrimaryKeys::contains)
              .collect(() -> new TreeMap<>(String::compareToIgnoreCase), (map, key) -> map.put(key, insertRecord.get(key)), Map::putAll);

          var result = query(null, condition);
          return result.map(recordList -> !recordList.isEmpty() ? recordList.get(0) : null);
        });
  }

  @Override
  @SuppressWarnings({"unchecked"})
  public Try<RecordList> batchInsert(List<Map<String, Object>> dataList) {
    if (ObjectUtils.isEmpty(dataList)) {
      return Try.success(RecordList.empty());
    }

    var result = Try.of(() -> {
      var records = dataList.stream().map(this::buildInsertRecord).collect(Collectors.toList());
      if (records.stream().anyMatch(Try::isFailure)) {
        // OPTIMIZE: 增加数据校验失败类型，将错误明细带出去
        throw new RecordValidationException("数据校验失败", Collections.emptyMap());
      }
      return records.stream().map(Try::get).collect(Collectors.toList());
    });

    if (result.isFailure()) {
      var cause = result.getCause();
      if (cause instanceof RecordValidationException) {
        return Try.failure(cause);
      }
      return Try.failure(new RecordValidationException("校验批量插入数据时发生未知错误", Collections.emptyMap()));
    }

    List<Record> records = result.get();

    // 创建 DML TranspileContext
    TranspileContext dmlContext = createTranspileContext();

    // 归一化插入语句
    var statement = DMLStatements.buildBatchInsertStatement(getFullName(), (List<Map<String, ?>>) (List<?>) records);

    if (statement.isFailure()) {
      var cause = statement.getCause();
      if (cause instanceof StatementException) {
        return Try.failure(new StatementSyntaxException("构造" + getFullName() + "模型的批量插入数据时发生语法错误", cause));
      }
      return Try.failure(cause);
    }

    // 执行插入操作
    var insertResult = statement.flatMap(batchInsertStatement -> getAdapter().batchInsert(batchInsertStatement, dmlContext));

    if (insertResult.isFailure()) {
      var cause = insertResult.getCause();
      if (cause instanceof DataAccessException) {
        return Try.failure(new QueryExecutionException("执行" + getFullName() + "模型的批量插入语句时发生错误", cause));
      }
      return Try.failure(cause);
    }

    // 根据主键填充插入数据
    var returnResult = insertResult.flatMap(ids -> {
      // 主键列表
      List<String> recordPrimaryKeys = getPrimaryKeys().stream()
          .map(DataModelField::getName)
          .collect(Collectors.toList());

      if (recordPrimaryKeys.size() == 1) {
        String primaryKey = recordPrimaryKeys.get(0);
        if (ids != null && !ids.isEmpty()) {
          return querySinglePrimaryKeyRecordsInInputOrder(primaryKey, ids);
        }

        List<Object> submittedPrimaryKeys = records.stream()
            .map(record -> record.get(primaryKey))
            .collect(Collectors.toList());
        if (submittedPrimaryKeys.stream().allMatch(ObjectUtils::isNotEmpty)) {
          return querySinglePrimaryKeyRecordsInInputOrder(primaryKey, submittedPrimaryKeys);
        }
      }

      // 没有主键，直接使用原始数据进行查询
      var conditions = recordPrimaryKeys.isEmpty()
          ? records.stream()
          .map(data -> data.entrySet().stream()
              .map(entry -> Tuple.of(entry.getKey(), entry.getValue() == null || isBasicType.test(entry.getValue()) ? entry.getValue() : DataJson.toJsonString(entry.getValue())))
              .collect(() -> new HashMap<String, Object>(), (map, tuple) -> map.put(tuple._1(), tuple._2()), Map::putAll))
          .collect(Collectors.toList())
          : records.stream()
          .map(data -> data.entrySet().stream()
              .filter(entry -> recordPrimaryKeys.contains(entry.getKey()))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
          .collect(Collectors.toList());
      var or = Collections.singletonMap("or", (Object) conditions);
      return query(null, or);
    });

    if (returnResult.isFailure()) {
      var cause = returnResult.getCause();
      if (cause instanceof DataAccessException) {
        return Try.failure(new QueryExecutionException("执行" + getFullName() + "模型的批量插入后自动填充时发生错误", cause));
      }
      return Try.failure(cause);
    }

    return returnResult;
  }

  private Try<RecordList> querySinglePrimaryKeyRecordsInInputOrder(String primaryKey, List<?> orderedPrimaryKeys) {
    List<Object> primaryKeys = new ArrayList<>(orderedPrimaryKeys);
    return query(primaryKeys)
        .map(records -> reorderBySinglePrimaryKey(records, primaryKey, orderedPrimaryKeys));
  }

  private RecordList reorderBySinglePrimaryKey(RecordList records, String primaryKey, List<?> orderedPrimaryKeys) {
    Map<String, Record> recordsByPrimaryKey = new LinkedHashMap<>();
    for (Record record : records) {
      String key = primaryKeyKey(getValueIgnoreCase(record, primaryKey));
      if (key != null) {
        recordsByPrimaryKey.putIfAbsent(key, record);
      }
    }

    List<Record> orderedRecords = orderedPrimaryKeys.stream()
        .map(this::primaryKeyKey)
        .map(recordsByPrimaryKey::get)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    return RecordList.of(orderedRecords);
  }

  private String primaryKeyKey(Object key) {
    return key == null ? null : String.valueOf(key);
  }

  private Object getValueIgnoreCase(Map<String, ?> record, String fieldName) {
    if (record.containsKey(fieldName)) {
      return record.get(fieldName);
    }
    for (Map.Entry<String, ?> entry : record.entrySet()) {
      String key = entry.getKey();
      if (key != null && key.equalsIgnoreCase(fieldName)) {
        return entry.getValue();
      }
    }
    return null;
  }

  @Override
  public Try<Long> update(Map<String, Object> where, Map<String, Object> data) {
    return sharedNormalizeContext.forClause("WHERE")
        .normalizeCondition(where, "root")
        .flatMap(condition -> update(condition, data));
  }

  @Override
  public Try<Long> update(SExpression<Boolean> where, Map<String, Object> data) {
    TranspileContext dmlContext = createTranspileContext();
    return buildUpdateRecord(data)
        .flatMap(record -> DMLStatements.buildUpdateStatement(getFullName(), record, where))
        .flatMap(updateStatement -> getAdapter().update(updateStatement, dmlContext));
  }

  @Override
  public Try<Long> delete(Map<String, Object> where) {
    return sharedNormalizeContext.forClause("WHERE")
        .normalizeCondition(where, "root")
        .flatMap(this::delete);
  }

  @Override
  public Try<Long> delete(SExpression<Boolean> where) {
    TranspileContext dmlContext = createTranspileContext();
    return DMLStatements.buildDeleteStatement(getFullName(), where)
        .flatMap(deleteStatement -> getAdapter().delete(deleteStatement, dmlContext));
  }

  @Override
  public Try<Long> count(Map<String, Object> where) {
    // 构建完整查询 Map（带 FROM）
    Map<String, Object> queryMap = new LinkedHashMap<>();
    queryMap.put("FROM", getFullName());
    queryMap.put("WHERE", where);

    var queryStatement = sharedNormalizeContext.normalizeQuery(queryMap);
    if (queryStatement.isFailure()) {
      var cause = queryStatement.getCause();
      if (cause instanceof StatementException) {
        return Try.failure(cause);
      }
      return Try.failure(new InvalidStatementException(
          "准备" + getFullName() + "模型的count操作条件时发生错误", cause));
    }

    return count(queryStatement.get());
  }

  @Override
  public Try<Long> count(QueryStatement statement) {
    var countStatement = validateCountOnlyStatement(statement)
        .flatMap(s -> prepareQueryStatement(s, "准备" + getFullName() + "模型的count操作条件时发生错误"));

    if (countStatement.isFailure()) {
      return Try.failure(countStatement.getCause());
    }

    OrchestrationContext rewriteContext = new OrchestrationContext();
    var rewrittenStatement = orchestrator.rewriteStatement(countStatement.get(), this, rewriteContext);
    if (rewrittenStatement.isFailure()) {
      return Try.failure(rewrittenStatement.getCause());
    }

    TranspileContext context = createTranspileContext();
    var transpileResult = context.getQueryTranspiler().applyWithContext(rewrittenStatement.get(), context);
    if (transpileResult.isFailure()) {
      var cause = transpileResult.getCause();
      if (cause instanceof TranspileException) {
        return Try.failure(cause);
      }
      return Try.failure(new TranspileException(
          "转译" + getFullName() + "模型的count操作条件时发生错误", cause));
    }

    return getAdapter().count(transpileResult.get());
  }

  @Override
  public Try<RecordList> query(Map<String, Object> statement) {
    var result = Try.of(() -> {
      var queryMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
      queryMap.putAll(statement);
      queryMap.put("FROM", Collections.singletonMap(getFullName(), getRawName()));

      var normalizeResult = sharedNormalizeContext.normalizeQuery(queryMap, new ModelQueryStatementBuilder());
      if (normalizeResult.isFailure()) {
        var cause = normalizeResult.getCause();
        if (cause instanceof StatementException) {
          throw cause;
        }
        throw new InvalidStatementException("准备" + getFullName() + "模型的查询语句的时候发生错误，可能存在语法错误", cause);
      }

      return query(normalizeResult.get())
          .getOrElseThrow(cause -> {
            if (cause instanceof DataAccessException) {
              return new QueryExecutionException("执行" + getFullName() + "模型的查询语句的时候发生错误", cause);
            }
            return cause;
          });
    });

    if (result.isSuccess()) {
      return result;
    }

    var cause = result.getCause();
    if (cause instanceof StatementException || cause instanceof DataAccessException) {
      return Try.failure(cause);
    }
    return Try.failure(new QueryExecutionException("执行查询语句时发生未知错误", cause));
  }

  @Override
  public Try<RecordList> query(QueryStatement statement) {
    return prepareQueryStatement(statement, "准备" + getFullName() + "模型的查询语句的时候发生错误，可能存在语法错误")
        .flatMap(this::orchestrateQuery);
  }

  private Try<QueryStatement> prepareQueryStatement(QueryStatement statement, String message) {
    QueryAnalyzeContext analyzeContext = QueryAnalyzeContext.builder()
        .model(this)
        .build();
    return new DefaultQueryTransformer(Arrays.asList(new WildcardExpandAnalyzer()))
        .transform(statement, analyzeContext)
        .flatMap(transformed -> new DefaultQueryAnalyzer().analyze(transformed, analyzeContext))
        .flatMap(analyzed -> new DefaultQueryTransformer(Arrays.asList(new OptimizationAnalyzer()))
            .transform(analyzed, analyzeContext))
        .recoverWith(cause -> {
          if (cause instanceof StatementException) {
            return Try.failure(cause);
          }
          return Try.failure(new InvalidStatementException(message, cause));
        });
  }

  private Try<QueryStatement> validateCountOnlyStatement(QueryStatement statement) {
    return Try.of(() -> {
      if (statement == null) {
        throw new InvalidStatementException("count(QueryStatement) 不支持空查询语句");
      }
      for (String key : statement.keySet()) {
        var keyword = com.ouroboros.data.dsl.Keyword.fromString(key);
        if (isNoOpCountClause(keyword, statement.get(key))) {
          continue;
        }
        if (keyword != com.ouroboros.data.dsl.Keyword.FROM
            && keyword != com.ouroboros.data.dsl.Keyword.WHERE) {
          throw new InvalidStatementException(
              "count(QueryStatement) 只支持 FROM/WHERE，不支持 " + key);
        }
      }
      if (statement instanceof ModelQueryStatement modelStatement) {
        if (modelStatement.getPopulateClause() != null
            && !modelStatement.getPopulateClause().getEntries().isEmpty()) {
          throw new InvalidStatementException("count(QueryStatement) 不支持 POPULATE");
        }
        if (modelStatement.getOmitClause() != null
            && !modelStatement.getOmitClause().getFields().isEmpty()) {
          throw new InvalidStatementException("count(QueryStatement) 不支持 OMIT");
        }
      }
      return statement;
    });
  }

  private boolean isNoOpCountClause(com.ouroboros.data.dsl.Keyword keyword, Object value) {
    if (keyword == null) {
      return false;
    }
    return switch (keyword) {
      case DISTINCT -> isNoOpDistinctClause(value);
      case SELECT -> isNoOpSelectClause(value);
      case JOIN, ORDER, UNION, WITH -> isEmptyCollection(value);
      case GROUP, HAVING -> isEmptyExpression(value);
      case LIMIT, OFFSET -> value == null;
      default -> false;
    };
  }

  private boolean isNoOpDistinctClause(Object value) {
    return !Boolean.TRUE.equals(value);
  }

  private boolean isNoOpSelectClause(Object value) {
    if (!(value instanceof List<?> select) || select.size() != 1) {
      return false;
    }
    Object onlySelect = select.get(0);
    if (!(onlySelect instanceof SExpression<?> expression)) {
      return false;
    }
    return expression.getOperator() == Operators.COLUMNS
        && expression.getParams().size() == 1
        && "*".equals(expression.getParam(0));
  }

  private boolean isEmptyCollection(Object value) {
    return value instanceof Collection<?> collection && collection.isEmpty();
  }

  private boolean isEmptyExpression(Object value) {
    return value instanceof SExpression<?> expression && expression.isEmpty();
  }

  /**
   * 通过 QueryOrchestrator 执行查询（共用方法）
   *
   * @param statement 查询语句
   * @return 查询结果
   */
  private Try<RecordList> orchestrateQuery(QueryStatement statement) {
    String mainAlias = statement.getFrom() != null
        ? statement.getFrom().getName() : getRawName();
    OrchestrationContext context = new OrchestrationContext();
    if (statement instanceof ModelQueryStatement mqs) {
      if (mqs.getPopulateClause() != null) {
        context.setPopulateClause(mqs.getPopulateClause());
      }
      if (mqs.getOmitClause() != null) {
        context.setOmitClause(mqs.getOmitClause());
      }
    }
    TranspileContext queryContext = createTranspileContext();
    MainQueryExecutor mainExecutor = (query) -> this.getAdapter().query(query, queryContext)
        .map(this::convertRecordValues);
    return orchestrator.orchestrate(statement, this, mainExecutor, context);
  }

  private RecordList convertRecordValues(RecordList records) {
    RecordListMapper recordListMapper = new RecordListMapper();
    getFields().stream()
        .filter(field -> Boolean.TRUE.equals(field.getValueType().isPhysical()))
        .forEach(field -> recordListMapper.addRecordModifier(record -> {
          var key = field.getName();
          if (!record.containsKey(key)) {
            return;
          }
          var convertResult = Try.of(() -> field.getValueType().convert(record.get(key)));
          if (convertResult.isFailure()) {
            logger.warn("字段'{}'类型转换时发生错误, 错误信息：{}", key, convertResult.getCause().getMessage());
            return;
          }
          record.put(key, convertResult.get());
        }));
    return recordListMapper.apply(records);
  }

  /**
   * 将查询结果转换为 RecordList
   *
   * @param resultList 原始查询结果
   * @return RecordList
   */
  private RecordList convertToRecordList(List<?> resultList) {
    if (resultList == null || resultList.isEmpty()) {
      return RecordList.empty();
    }

    List<Record> records = new ArrayList<>();
    for (Object item : resultList) {
      if (item instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) item;
        records.add(Record.of(map));
      } else {
        // 如果不是 Map，跳过或记录警告
        logger.warn("Query result item is not a Map: {}", item.getClass());
      }
    }

    return RecordList.of(records);
  }

  // ================== 便利方法和插件方法（继承自AbstractDataModel）==================

  // ================== 内部辅助方法 ==================

  // 这些方法是DefaultDataModel特有的，AbstractDataModel中没有

  // 下面是查询相关的辅助方法，这些方法原本在 BaseDataModel 中

  /**
   * 为当前模型创建 TranspileContext
   */
  private TranspileContext createTranspileContext() {
    Path<?> tablePath = new RelationalPathBase<>(Object.class, getRawName(), "", getRawName());
    FieldSource fieldSource = new FieldSource() {
      @Override
      public Path<?> getSelfPath() {
        return tablePath;
      }

      @Override
      public Optional<Path<?>> getField(String fieldName) {
        return DefaultDataModel.this.getField(fieldName)
            .map(field -> ModelFieldPath.of(Object.class, field));
      }

      @Override
      public List<Path<?>> getFields() {
        return DefaultDataModel.this.getFields().stream()
            .map(field -> (Path<?>) ModelFieldPath.of(Object.class, field))
            .collect(Collectors.toList());
      }

      @Override
      public Optional<DataModel> getDataModel() {
        return Optional.of(DefaultDataModel.this);
      }
    };
    return new BaseTranspileContext(fieldSource, getFullName());
  }

  // 重映射原始记录
  private Stream<Path<?>> getPathsHelper(Expression<?> expr) {
    if (expr instanceof Path) {
      return Stream.of((Path<?>) expr);
    }
    if (expr instanceof FactoryExpression<?> fac) {
      return fac.getArgs().stream().flatMap(this::getPathsHelper);
    }
    if (expr instanceof Operation) {
      var op = (Operation<?>) expr;
      if (op.getOperator() == Operators.ALIAS) {
        return getPathsHelper(op.getArg(1));
      }
    }
    return Stream.empty();
  }

  private void prepareValueConverter(QueryMetadata metadata, RecordListMapper recordListMapper) {
    getPathsHelper(metadata.getProjection())
        .filter(ModelFieldPath.class::isInstance)
        .map(ModelFieldPath.class::cast)
        .forEach(path -> {
          var key = path.toString();
          var valueType = path.getModelField().getValueType();
          recordListMapper.addRecordModifier(record -> {
            var convertResult = Try.of(() -> valueType.convert(record.get(key)));
            if (convertResult.isFailure()) {
              logger.warn("字段'{}'类型转换时发生错误, 错误信息：{}", key, convertResult.getCause().getMessage());
            } else {
              record.put(key, convertResult.get());
            }
          });
        });
  }

  public static class RecordListMapper {
    List<Consumer<Map<String, Object>>> recordModifierList = new ArrayList<>();
    List<Consumer<RecordList>> resultWatcherList = new ArrayList<>();
    List<Function<RecordList, RecordList>> recordListModifiers = new ArrayList<>();

    RecordList apply(RecordList records) {
      resultWatcherList.forEach(watcher -> watcher.accept(records));

      // 应用 RecordList 级别的修改器（用于 Populate 策略）
      RecordList modifiedRecords = records;
      for (var modifier : recordListModifiers) {
        modifiedRecords = modifier.apply(modifiedRecords);
      }
      final RecordList finalRecords = modifiedRecords;

      // 将所有的 recordModifier 聚合成一个
      Consumer<Map<String, Object>> recordModifier = record -> recordModifierList.forEach(modifier -> modifier.accept(record));
      // 如果是 DefaultRecordListImpl，则调用其专属方法在内部修改（避免大量的内存拷贝）
      if (finalRecords instanceof DefaultRecordListImpl recordList) {
        recordList.mutateRecord(recordModifier);
        return finalRecords;
      }

      // 否则进行内存拷贝
      var newList = new ArrayList<>(finalRecords);
      var iterator = newList.listIterator();
      while (iterator.hasNext()) {
        var record = iterator.next();
        // 如果是 DefaultRecordImpl，则调用其专属方法在内部修改（避免大量的内存拷贝）
        if (record instanceof DefaultRecordImpl defaultRecord) {
          defaultRecord.mutate(recordModifier);
          continue;
        }

        // 否则进行内存拷贝
        Map<String, Object> newRecord = new LinkedHashMap<>(record);
        recordModifier.accept(newRecord);
        iterator.set(Record.of(newRecord));
      }
      return RecordList.of(newList);
    }

    public void addRecordModifier(Consumer<Map<String, Object>> recordModifier) {
      recordModifierList.add(recordModifier);
    }

    public void addResultWatcher(Consumer<RecordList> resultWatcher) {
      resultWatcherList.add(resultWatcher);
    }

    /**
     * 添加 RecordList 级别的修改器（用于 Populate 策略）
     *
     * @param modifier RecordList 到 RecordList 的转换函数
     */
    public void addRecordListModifier(Function<RecordList, RecordList> modifier) {
      recordListModifiers.add(modifier);
    }

    RecordListMapper combine(RecordListMapper other) {
      var newMapper = new RecordListMapper();
      newMapper.recordModifierList.addAll(this.recordModifierList);
      newMapper.recordModifierList.addAll(other.recordModifierList);
      newMapper.resultWatcherList.addAll(this.resultWatcherList);
      newMapper.resultWatcherList.addAll(other.resultWatcherList);
      newMapper.recordListModifiers.addAll(this.recordListModifiers);
      newMapper.recordListModifiers.addAll(other.recordListModifiers);
      return newMapper;
    }
  }
}
