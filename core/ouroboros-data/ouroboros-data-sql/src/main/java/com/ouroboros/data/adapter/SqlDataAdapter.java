package com.ouroboros.data.adapter;

import static com.ouroboros.data.dsl.StatementPredicates.isBasicType;
import static com.ouroboros.data.util.Asserts.assertAllSuccess;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.Date;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterUtils;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.object.SqlUpdate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.JdbcUtils;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.statement.BatchInsertStatement;
import com.ouroboros.data.dsl.statement.DeleteStatement;
import com.ouroboros.data.dsl.statement.InsertStatement;
import com.ouroboros.data.dsl.statement.UpdateStatement;
import com.ouroboros.data.exception.*;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.record.RecordList;
import com.ouroboros.data.sql.SqlTemplatesFactory;
import com.ouroboros.data.station.DataStationCenter;
import com.ouroboros.data.transpile.BaseTranspileContext;
import com.ouroboros.data.transpile.FieldSource;
import com.ouroboros.data.transpile.TranspileContext;
import com.ouroboros.data.dsl.ModelFieldPath;
import com.ouroboros.data.util.DataConverters;
import com.ouroboros.data.util.DataJson;
import com.querydsl.core.JoinType;
import com.querydsl.core.QueryMetadata;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.SubQueryExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.sql.MySQLTemplates;
import com.querydsl.sql.RelationalPath;
import com.querydsl.sql.SQLBindings;
import com.querydsl.sql.SQLTemplates;
import com.querydsl.sql.dml.SQLDeleteClause;
import com.querydsl.sql.dml.SQLInsertClause;
import com.querydsl.sql.dml.SQLUpdateClause;
import com.querydsl.sql.types.Null;

@SuppressWarnings("unused")
public class SqlDataAdapter extends AbstractDataAdapter {
  private final static Logger logger = LoggerFactory.getLogger(SqlDataAdapter.class);
  private final DataSource dataSource;
  private final JdbcTemplate jdbcTemplate;

  private final SQLTemplates sqlTemplates;

  public SqlDataAdapter(DataSource dataSource) {
    this(dataSource, null);
  }

  public SqlDataAdapter(DataSource dataSource, String dialect) {
    super();
    this.dataSource = dataSource;

    // 如果在事务中，则取事务中的连接
    Supplier<Connection> finalConnectionSupplier = () -> DataSourceUtils.getConnection(dataSource);

    sqlTemplates = SqlTemplatesFactory.getSQLTemplates(dataSource, dialect);
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  private static DataModelException classifyException(String message, Exception e) {
    if (e instanceof DataIntegrityViolationException) {
      return new DataValidationException(message + ": 数据完整性约束冲突", e);
    }
    if (e instanceof DataAccessResourceFailureException) {
      return new ConnectionException(message + ": 数据源连接/资源异常", e);
    }
    if (e instanceof BadSqlGrammarException) {
      return new DatabaseException(message + ": SQL语法错误", e);
    }
    return new QueryExecutionException(message, e);
  }

  private static boolean hasGeneratedKeyValue(Object key) {
    return key != null
        && !(key instanceof RowId)
        && (!(key instanceof Number) || ((Number) key).doubleValue() != 0D);
  }

  private <PK_TYPE> PK_TYPE normalizeGeneratedKey(Object key, Class<PK_TYPE> keyClass) {
    if (keyClass == null || !hasGeneratedKeyValue(key)) {
      return null;
    }
    if (keyClass.isInstance(key)) {
      return keyClass.cast(key);
    }
    if (CharSequence.class.isAssignableFrom(keyClass)) {
      return keyClass.cast(DataConverters.toString(key));
    }
    if (Integer.class.isAssignableFrom(keyClass)) {
      return keyClass.cast(DataConverters.toInteger(key));
    }
    if (Long.class.isAssignableFrom(keyClass)) {
      return keyClass.cast(DataConverters.toLong(key));
    }
    if (BigDecimal.class.isAssignableFrom(keyClass)) {
      return keyClass.cast(DataConverters.toBigDecimal(key));
    }
    if (BigInteger.class.isAssignableFrom(keyClass)) {
      return keyClass.cast(DataConverters.toBigInteger(key));
    }
    if (UUID.class.isAssignableFrom(keyClass)) {
      return key instanceof UUID uuid
          ? keyClass.cast(uuid)
          : Try.of(() -> keyClass.cast(UUID.fromString(DataConverters.toString(key)))).getOrNull();
    }
    return null;
  }

  private <PK_TYPE> void collectGeneratedKey(List<PK_TYPE> generatedKeys, Object key, Class<PK_TYPE> keyClass) {
    var normalizedKey = normalizeGeneratedKey(key, keyClass);
    if (normalizedKey != null) {
      generatedKeys.add(normalizedKey);
    }
  }

  private RelationalPath<?> resolveRelationPath(String entityName, TranspileContext context) {
    FieldSource fieldSource = context.resolveTable(entityName)
        .orElseThrow(() -> new EntityNotFoundException("无法找到表: " + entityName, entityName));
    RelationalPath<?> relationPath = fieldSource.getTable();
    if (relationPath == null) {
      throw new TranspileException("无法为表构造可写 SQL 路径: " + entityName);
    }
    return relationPath;
  }

  private String[] getGeneratedKeyColumnNames(String entityName, RelationalPath<?> relationPath,
                                              TranspileContext context) {
    var primaryKey = relationPath.getPrimaryKey();
    if (primaryKey != null) {
      var metadata = primaryKey.getLocalColumns().stream()
          .map(relationPath::getMetadata)
          .collect(Collectors.toList());
      if (!metadata.isEmpty() && metadata.stream()
          .allMatch(column -> column != null && column.getName() != null && !column.getName().trim().isEmpty())) {
        return metadata.stream()
            .map(column -> column.getName())
            .toArray(String[]::new);
      }
    }

    var modelPrimaryKeys = context.resolveTable(entityName)
        .flatMap(FieldSource::getDataModel)
        .map(DataModel::getPrimaryKeys)
        .orElseGet(Collections::emptyList);
    if (modelPrimaryKeys.stream()
        .anyMatch(field -> field.getRawName() == null || field.getRawName().trim().isEmpty())) {
      return new String[0];
    }
    return modelPrimaryKeys.stream()
        .map(DataModelField::getRawName)
        .toArray(String[]::new);
  }

  @Override
  protected TranspileContext createMinimalContext(String entityName) {
    return findModelBackedContext(entityName).orElseGet(() -> super.createMinimalContext(entityName));
  }

  private Optional<TranspileContext> findModelBackedContext(String entityName) {
    String physicalTableName = extractPhysicalName(entityName);
    return DataStationCenter.getDataStationMap().values().stream()
        .filter(station -> Objects.equals(station.getDataSource(), dataSource))
        .flatMap(station -> station.getDataModelList().stream())
        .filter(model -> matchesEntity(model, entityName, physicalTableName))
        .findFirst()
        .map(model -> createModelBackedContext(entityName, model));
  }

  private boolean matchesEntity(DataModel model, String entityName, String physicalTableName) {
    return model.getFullName().equalsIgnoreCase(entityName)
        || model.getRawName().equalsIgnoreCase(physicalTableName);
  }

  private String extractPhysicalName(String entityName) {
    if (entityName == null) {
      return "";
    }
    int dotIndex = entityName.lastIndexOf('.');
    return dotIndex >= 0 ? entityName.substring(dotIndex + 1) : entityName;
  }

  private TranspileContext createModelBackedContext(String entityName, DataModel model) {
    String rawTableName = model.getRawName();
    String schemaName = "";
    int dotIndex = entityName == null ? -1 : entityName.lastIndexOf('.');
    if (dotIndex > 0) {
      schemaName = entityName.substring(0, dotIndex);
    }

    var tablePath = new com.querydsl.sql.RelationalPathBase<>(Object.class, rawTableName, schemaName, rawTableName);
    FieldSource fieldSource = new FieldSource() {
      @Override
      public Path<?> getSelfPath() {
        return tablePath;
      }

      @Override
      public Optional<Path<?>> getField(String fieldName) {
        return findField(model, fieldName)
            .map(field -> ModelFieldPath.of(Object.class, field));
      }

      @Override
      public List<Path<?>> getFields() {
        return model.getFields().stream()
            .map(field -> (Path<?>) ModelFieldPath.of(Object.class, field))
            .collect(Collectors.toList());
      }

      @Override
      public Optional<DataModel> getDataModel() {
        return Optional.of(model);
      }
    };
    return new BaseTranspileContext(fieldSource, entityName);
  }

  private Optional<DataModelField> findField(DataModel model, String fieldName) {
    String normalizedField = extractPhysicalName(fieldName);
    Optional<DataModelField> logicalField = model.getField(normalizedField);
    if (logicalField.isPresent()) {
      return logicalField;
    }
    return model.getFields().stream()
        .filter(field -> field.getRawName().equalsIgnoreCase(normalizedField))
        .findFirst();
  }

  @Override
  @SuppressWarnings("unchecked")
  public Try<String> insert(InsertStatement statement, TranspileContext context) {
    return Try.of(() -> {
      try {
        var values = statement.getValues();
        var relationPath = resolveRelationPath(statement.getEntityName(), context);
        var insert = new SQLInsertClause(null, sqlTemplates, relationPath);
        var setFields = values.entrySet().stream()
            .map(entry -> Try.run(() -> {
              var field = context.resolve(entry.getKey())
                  .orElseThrow(() -> new FieldNotFoundException("无法找到字段: " + entry.getKey(), entry.getKey(), statement.getEntityName()));
              var value = entry.getValue();
              var valueToInsert = isBasicType.test(value)
                  ? value
                  : DataJson.toJsonString(value);
              insert.set((Path<Object>) field, valueToInsert);
            }));
        assertAllSuccess(setFields);
        var sql = insert.getSQL().get(0);
        var pk = insert(Object.class, sql.getSQL(), sql.getNullFriendlyBindings().toArray(),
            getGeneratedKeyColumnNames(statement.getEntityName(), relationPath, context))
            .getOrElseThrow(e -> e);
        return pk == null ? null : pk.toString();
      } catch (DataModelException e) {
        throw e;
      } catch (Exception e) {
        throw classifyException("执行插入操作时发生错误", e);
      }
    });
  }

  @Override
  @SuppressWarnings("unchecked")
  public Try<List<String>> batchInsert(BatchInsertStatement statement, TranspileContext context) {
    return Try.of(() -> {
      try {
        var relationPath = resolveRelationPath(statement.getEntityName(), context);
        var sqlInsert = new SQLInsertClause(null, sqlTemplates, relationPath);
        var inserts = statement.getValuesList().stream().map(values -> Try.run(() -> {
          var setFields = values.entrySet().stream()
              .map(entry -> Try.run(() -> {
                var field = context.resolve(entry.getKey())
                    .orElseThrow(() -> new FieldNotFoundException("无法找到字段: " + entry.getKey(), entry.getKey(), statement.getEntityName()));
                var value = entry.getValue();
                var valueToInsert = value == null || isBasicType.test(value)
                    ? value
                    : DataJson.toJsonString(value);
                sqlInsert.set((Path<Object>) field, valueToInsert);
              }))
              .collect(Collectors.toList());
          assertAllSuccess(setFields);
          sqlInsert.addBatch();
        }));
        assertAllSuccess(inserts);

        var sqls = sqlInsert.getSQL();
        var sqlString = sqls.get(0).getSQL();
        var values = sqls.stream()
            .map(SQLBindings::getNullFriendlyBindings)
            .collect(Collectors.toList());

        var list = batchInsert(Object.class, sqlString, values,
                getGeneratedKeyColumnNames(statement.getEntityName(), relationPath, context))
            .getOrElseThrow(e -> e);
        return list.stream()
            .map(key -> key == null ? null : key.toString())
            .collect(Collectors.toList());
      } catch (DataModelException e) {
        throw e;
      } catch (Exception e) {
        throw classifyException("执行批量插入操作时发生错误", e);
      }
    });
  }

  @Override
  public Try<Long> delete(DeleteStatement statement, TranspileContext context) {
    return Try.of(() -> {
      try {
        var relationPath = resolveRelationPath(statement.getEntityName(), context);
        var deleteClause = new SQLDeleteClause(null, sqlTemplates, relationPath);
        var where = context.transpilePredicate(statement.getWhere())
            .getOrElseThrow(e -> new TranspileException("转译删除条件时发生错误，请检查", e));
        deleteClause.where(where);
        var sql = deleteClause.getSQL().get(0);

        return execute(sql.getSQL(), sql.getNullFriendlyBindings().toArray())
            .map(count -> (long) count)
            .getOrElseThrow(e -> e);
      } catch (DataModelException e) {
        throw e;
      } catch (Exception e) {
        throw classifyException("执行删除操作时发生错误", e);
      }
    });
  }

  @Override
  @SuppressWarnings("unchecked")
  public Try<Long> update(UpdateStatement statement, TranspileContext context) {
    return Try.of(() -> {
      try {
        var relationPath = resolveRelationPath(statement.getEntityName(), context);
        var sqlUpdate = new SQLUpdateClause(null, sqlTemplates, relationPath);
        var updateFields = statement.getValues().entrySet().stream()
            .map(entry -> Try.run(() -> {
              var field = context.resolve(entry.getKey())
                  .orElseThrow(() -> new FieldNotFoundException("无法找到字段: " + entry.getKey(), entry.getKey(), statement.getEntityName()));
              var value = entry.getValue();
              var valueToUpdate = value == null || isBasicType.test(value)
                  ? value
                  : DataJson.toJsonString(value);
              sqlUpdate.set((Path<Object>) field, valueToUpdate);
            }))
            .collect(Collectors.toList());
        assertAllSuccess(updateFields);

        var where = context.transpilePredicate(statement.getWhere())
            .getOrElseThrow(e -> e);
        sqlUpdate.where(where);

        var sql = sqlUpdate.getSQL().get(0);
        return execute(sql.getSQL(), sql.getNullFriendlyBindings().toArray())
            .map(count -> (long) count)
            .getOrElseThrow(e -> e);
      } catch (DataModelException e) {
        throw e;
      } catch (Exception e) {
        throw classifyException("执行更新操作时发生错误", e);
      }
    });
  }

  @Override
  public Try<RecordList> query(QueryMetadata metadata) {
    return Try.of(() -> {
      try {
        validateJoinSupport(metadata);
        var sql = new OuroborosSQLQuery(sqlTemplates, metadata).getSQL();
        return query(sql.getSQL(), sql.getNullFriendlyBindings().toArray())
            .getOrElseThrow(e -> e);
      } catch (DataModelException e) {
        throw e;
      } catch (Exception e) {
        throw classifyException("执行查询操作时发生错误", e);
      }
    });
  }

  @Override
  public Try<Long> count(QueryMetadata queryMetadata) {
    return Try.of(() -> {
      try {
        validateJoinSupport(queryMetadata);
        var select = queryMetadata.getProjection();
        var where = queryMetadata.getWhere();
        var from = queryMetadata.getJoins().stream()
            .filter(j -> j.getType() == JoinType.DEFAULT)
            .findFirst()
            .orElseThrow(() -> new StatementError("Missing FROM Clause"))
            .getTarget();

        var queryStatement = new OuroborosSQLQuery(sqlTemplates);
        queryStatement
            .from(from)
            .where(where);

        if (queryMetadata.isDistinct()) {
          queryStatement.distinct();
          queryStatement.select(ExpressionUtils.count(queryMetadata.getProjection()));
        } else {
          queryStatement.select(ExpressionUtils.count(Expressions.ONE));
        }
        var sql = queryStatement.getSQL();
        return query(sql.getSQL(), sql.getNullFriendlyBindings().toArray())
            .map(count -> count.isEmpty()
                ? 0L
                : count.get(0).values()
                .stream()
                .findFirst()
                .map(cnt -> DataConverters.toLong(cnt))
                .orElse(0L))
            .getOrElseThrow(e -> e);
      } catch (DataModelException e) {
        throw e;
      } catch (Exception e) {
        throw classifyException("执行count操作时发生错误", e);
      }
    });
  }

  private void validateJoinSupport(QueryMetadata metadata) {
    if (!isMySqlDialect() || !containsFullJoin(metadata, Collections.newSetFromMap(new IdentityHashMap<>()))) {
      return;
    }

    throw new DatabaseException(
        "当前 MySQL 方言不支持 FULL JOIN，请改用 LEFT/RIGHT JOIN + UNION，或切换到支持 FULL JOIN 的数据库",
        null,
        0,
        null);
  }

  private boolean isMySqlDialect() {
    return sqlTemplates instanceof MySQLTemplates;
  }

  private boolean containsFullJoin(QueryMetadata metadata, Set<QueryMetadata> visited) {
    if (metadata == null || !visited.add(metadata)) {
      return false;
    }

    for (var join : metadata.getJoins()) {
      if (join.getType() == JoinType.FULLJOIN) {
        return true;
      }
      if (join.getTarget() instanceof SubQueryExpression<?> subQuery
          && containsFullJoin(subQuery.getMetadata(), visited)) {
        return true;
      }
    }

    if (metadata instanceof com.ouroboros.data.transpile.OuroborosQueryMetadata ouroborosMetadata) {
      for (var union : ouroborosMetadata.getUnions()) {
        if (containsFullJoin(union, visited)) {
          return true;
        }
      }
      for (var unionAll : ouroborosMetadata.getUnionAlls()) {
        if (containsFullJoin(unionAll, visited)) {
          return true;
        }
      }
      for (var with : ouroborosMetadata.getWiths()) {
        if (containsFullJoin(with._2(), visited)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * 以原生SQL的方式执行查询语句
   *
   * @param sql    SQL语句
   * @param params 参数
   */
  public Try<RecordList> query(String sql, Object... params) {
    return Try.of(() -> {
      try {
        var values = Stream.of(params)
            .map(v -> v instanceof Null ? null : v)
            .toArray();

        var result = jdbcTemplate.query(sql, this::resultSetToRecordList, values);
        if (result == null) {
          throw new QueryExecutionException("执行SQL查询时发生错误，返回值为null");
        }
        return result.getOrElseThrow(e -> e);
      } catch (DataModelException e) {
        throw e;
      } catch (Exception e) {
        throw classifyException("执行SQL查询时发生错误", e);
      }
    });
  }

  private Try<RecordList> resultSetToRecordList(ResultSet rs) {
    return Try.of(() -> {
      try {
        var recordList = new ArrayList<Map<String, Object>>();
        var metadata = rs.getMetaData();
        var columnCount = metadata.getColumnCount();
        var fieldNames = new ArrayList<String>();
        for (int i = 1; i <= columnCount; i++) {
          // TODO: 此处可能会考虑不同的数据库表现行为（originalName与name不一致！）
          var fieldName = metadata.getColumnLabel(i);
          if (fieldNames.contains(fieldName)) {
            var tableName = metadata.getTableName(i);
            fieldName = tableName + "__" + fieldName;
          }
          if (fieldNames.contains(fieldName)) {
            fieldName = fieldName + "__" + i;
          }
          fieldNames.add(fieldName);
        }
        while (rs.next()) {
          var record = new LinkedHashMap<String, Object>(columnCount);
          for (int i = 1; i <= columnCount; i++) {
            record.put(fieldNames.get(i - 1), rs.getObject(i));
          }
          recordList.add(record);
        }
        rs.close();
        return RecordList.of(recordList);
      } catch (SQLException e) {
        throw new QueryExecutionException("处理查询结果集时发生错误", e);
      } catch (Exception e) {
        throw classifyException("处理查询结果集时发生未知错误", e);
      }
    });
  }

  /**
   * 以原生SQL的方式执行UPDATE、DELETE、INSERT语句
   *
   * @param sql    SQL语句
   * @param params List 形式的参数表
   * @return 影响行数
   */
  public Try<Integer> execute(String sql, List<Object> params) {
    return execute(sql, params.toArray());
  }

  /**
   * 以原生SQL的方式执行UPDATE、DELETE、INSERT语句
   *
   * @param sql    SQL语句
   * @param params 参数
   * @return 影响行数
   */
  public Try<Integer> execute(String sql, Object... params) {
    return Try.of(() -> {
      try {
        var jdbcTypes = Stream.of(params)
            .map(this::toJdbcType)
            .mapToInt(v -> v)
            .toArray();
        var values = Stream.of(params)
            .map(v -> v instanceof Null ? null : v)
            .toArray();

        var update = new SqlUpdate(dataSource, sql, jdbcTypes);
        return update.update(values);
      } catch (DataModelException e) {
        throw e;
      } catch (Exception e) {
        throw classifyException("执行SQL语句时发生未知错误", e);
      }
    });
  }

  /**
   * 以原生SQL的方式执行UPDATE、DELETE、INSERT语句
   *
   * @param sql    要求参数以命名的方式出现
   *               <p>
   *               如:
   *               <code>"UPDATE table SET name = :name, age = :age WHERE id = :id"</code>，
   *               </p>
   *               <p>
   *               或者
   *               <code>"UPDATE table SET name = :{name}, age = :{age} WHERE id = :{id}"</code>
   *               </p>
   * @param params Map 形式的参数表
   * @return Try<Integer> 影响的行数
   */
  public Try<Integer> execute(String sql, Map<String, ?> params) {
    return Try.of(() -> {
      try {
        var update = new SqlUpdate(dataSource, sql);
        return update.updateByNamedParam(params);
      } catch (DataModelException e) {
        throw e;
      } catch (Exception e) {
        throw classifyException("执行SQL语句时发生未知错误", e);
      }
    });
  }

  /**
   * 以原生SQL的方式执行自增主键打的插入语句，并返回主键
   *
   * @param <PK_TYPE> 主键类型
   * @param sql       参数以顺序的方式出现，如:
   *                  <code>"INSERT INTO table (name, age) VALUES (?, ?)"</code>
   * @param keyClass  主键类
   * @param params    参数
   * @return Either<Exception, Long> 异常或主键
   */
  public <PK_TYPE> Try<PK_TYPE> insert(Class<PK_TYPE> keyClass, String sql, Object... params) {
    return insert(keyClass, sql, params, new String[0]);
  }

  private <PK_TYPE> Try<PK_TYPE> insert(Class<PK_TYPE> keyClass, String sql, Object[] params,
                                        String[] generatedKeyColumnNames) {
    var keyHolder = new GeneratedKeyHolder();
    return Try.of(() -> {
      try {
        var jdbcTypes = Stream.of(params)
            .map(this::toJdbcType)
            .mapToInt(i -> i)
            .toArray();
        var values = Stream.of(params)
            .map(v -> v instanceof Null ? null : v)
            .toArray();

        var update = new SqlUpdate(dataSource, sql, jdbcTypes);
        update.setParameters();
        if (generatedKeyColumnNames.length == 0) {
          update.setReturnGeneratedKeys(true);
        } else {
          update.setGeneratedKeysColumnNames(generatedKeyColumnNames);
        }
        update.update(values, keyHolder);
        var key = Try.of(() -> keyHolder.getKey()).getOrElse(() -> null);
        return normalizeGeneratedKey(key, keyClass);
      } catch (DataModelException e) {
        throw e;
      } catch (Exception e) {
        throw classifyException("执行插入操作并获取主键时发生错误", e);
      }
    });
  }

  /**
   * 以原生SQL的方式执行自增主键打的插入语句，并返回主键
   *
   * @param <PK_TYPE> 主键类型
   * @param sql       要求参数以命名的方式出现
   *                  <p>
   *                  如:
   *                  <code>"UPDATE table SET name = :name, age = :age WHERE id = :id"</code>，
   *                  </p>
   *                  <p>
   *                  或者
   *                  <code>"UPDATE table SET name = :{name}, age = :{age} WHERE id = :{id}"</code>
   *                  </p>
   *                  * @param keyClass 主键类
   * @param data      数据
   * @return Either<Exception, Long> 异常或主键
   */
  public <PK_TYPE> Try<PK_TYPE> insert(Class<PK_TYPE> keyClass, String sql,
                                       Map<String, Object> data) {
    var parsedSql = NamedParameterUtils.parseSqlStatement(sql);
    var keyHolder = new GeneratedKeyHolder();
    return Try.of(() -> {
      try {
        MapSqlParameterSource paramSource = new MapSqlParameterSource(data);
        var sqlToUse = NamedParameterUtils.substituteNamedParameters(parsedSql, paramSource);
        Object[] params = NamedParameterUtils.buildValueArray(parsedSql, paramSource, null);
        jdbcTemplate.update(new PreparedStatementCreatorFactory(sqlToUse).newPreparedStatementCreator(params), keyHolder);
        var key = keyHolder.getKey();
        return normalizeGeneratedKey(key, keyClass);
      } catch (DataModelException e) {
        throw e;
      } catch (Exception e) {
        throw classifyException("执行命名参数插入操作并获取主键时发生错误", e);
      }
    });
  }

  /**
   * 以原生SQL的方式执行批量插入语句，并返回主键列表
   *
   * @param <PK_TYPE> 主键类型
   * @param sql       参数以顺序的方式出现，如:
   *                  <code>"INSERT INTO table (name, age) VALUES (?, ?)"</code>
   * @param keyClass  主键类
   * @param data      参数列表
   * @return Either<Exception, List < PK_TYPE>> 异常或主键列表
   */
  public <PK_TYPE> Try<List<PK_TYPE>> batchInsert(Class<PK_TYPE> keyClass, String sql,
                                                  List<List<Object>> data) {
    return batchInsert(keyClass, sql, data, new String[0]);
  }

  private <PK_TYPE> Try<List<PK_TYPE>> batchInsert(Class<PK_TYPE> keyClass, String sql,
                                                   List<List<Object>> data,
                                                   String[] generatedKeyColumnNames) {
    PreparedStatementCreator psc = conn -> {
      if (keyClass == null) {
        return conn.prepareStatement(sql);
      }
      if (generatedKeyColumnNames.length == 0) {
        return conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      }
      return conn.prepareStatement(sql, generatedKeyColumnNames);
    };
    PreparedStatementCallback<Try<List<PK_TYPE>>> batchCallBack = stm -> {
      var list = new ArrayList<PK_TYPE>();
      return Try.of(() -> {
        try {
          // 如果支持批量插入，则使用批量插入
          // TODO: 暂时禁用，因为MySQL上出现了问题
          if (false && JdbcUtils.supportsBatchUpdates(stm.getConnection())) {
            for (List<Object> row : data) {
              for (int i = 0; i < row.size(); i++) {
                stm.setObject(i + 1, row.get(i));
              }
              stm.addBatch();
            }
            stm.executeBatch();
            var recordSet = stm.getGeneratedKeys();
            while (recordSet.next()) {
              collectGeneratedKey(list, recordSet.getObject(1), keyClass);
            }
            recordSet.close();
            return list;
          }
          // 否则依次插入
          for (List<Object> row : data) {
            for (int i = 0; i < row.size(); i++) {
              Object value = row.get(i);
              stm.setObject(i + 1, value instanceof Null ? null : value);
            }
            stm.executeUpdate();
            var recordSet = stm.getGeneratedKeys();
            if (recordSet.next()) {
              collectGeneratedKey(list, recordSet.getObject(1), keyClass);
            }
            recordSet.close();
          }
          return list;
        } catch (SQLException e) {
          throw new QueryExecutionException("执行批量插入时发生数据库错误", e);
        } catch (Exception e) {
          throw classifyException("执行批量插入时发生错误", e);
        }
      });
    };
    var executeResult = jdbcTemplate.execute(psc, batchCallBack);
    if (executeResult == null) {
      return Try.failure(new QueryExecutionException("执行批量插入SQL语句时发生未知错误"));
    }
    return executeResult;
  }

  /**
   * 以原生SQL的方式执行批量插入语句，并返回主键列表
   *
   * @param <PK_TYPE> 主键类型
   * @param sql       参数以顺序的方式出现，如:
   *                  <code>"INSERT INTO table (name, age) VALUES (:name, :age)"</code>
   * @param keyClass  主键类
   * @param data      参数列表
   * @return 主键列表
   */
  @SuppressWarnings("unchecked")
  public <PK_TYPE> Try<List<PK_TYPE>> batchInsert(Class<PK_TYPE> keyClass, String sql,
                                                  Map<String, Object>... data) {
    var parsedSql = NamedParameterUtils.parseSqlStatement(sql);
    PreparedStatementCreator psc = conn -> keyClass != null
        ? conn.prepareStatement(parsedSql.toString(), Statement.RETURN_GENERATED_KEYS)
        : conn.prepareStatement(sql);
    PreparedStatementCallback<Try<List<PK_TYPE>>> batchCallBack = stm -> Try.of(() -> {
      try {
        var list = new ArrayList<PK_TYPE>();
        // 如果支持批量插入，则使用批量插入
        if (JdbcUtils.supportsBatchUpdates(stm.getConnection())) {
          for (var row : data) {
            MapSqlParameterSource paramSource = new MapSqlParameterSource(row);
            Object[] params = NamedParameterUtils.buildValueArray(parsedSql, paramSource, null);
            for (int i = 0; i < params.length; i++) {
              stm.setObject(i + 1, params[i]);
            }
            stm.addBatch();
          }
          stm.executeBatch();
          stm.getGeneratedKeys();
          var recordSet = stm.getGeneratedKeys();
          while (recordSet.next()) {
            collectGeneratedKey(list, recordSet.getObject(1), keyClass);
          }
          recordSet.close();
          return list;
        }
        // 否则依次插入
        for (var row : data) {
          MapSqlParameterSource paramSource = new MapSqlParameterSource(row);
          Object[] params = NamedParameterUtils.buildValueArray(parsedSql, paramSource, null);
          for (int i = 0; i < params.length; i++) {
            stm.setObject(i + 1, params[i]);
          }
          stm.executeUpdate();
          var recordSet = stm.getGeneratedKeys();
          if (recordSet.next()) {
            collectGeneratedKey(list, recordSet.getObject(1), keyClass);
          }
          recordSet.close();
        }
        return list;
      } catch (SQLException e) {
        throw new QueryExecutionException("执行命名参数批量插入时发生数据库错误", e);
      } catch (Exception e) {
        throw classifyException("执行命名参数批量插入时发生错误", e);
      }
    });
    var executeResult = jdbcTemplate.execute(psc, batchCallBack);
    if (executeResult == null) {
      return Try.failure(new QueryExecutionException("执行批量插入SQL语句时发生未知错误"));
    }

    return executeResult;
  }

  /**
   * 返回jdbcTemplate
   *
   * @return jdbcTemplate
   */
  public JdbcTemplate getJdbcTemplate() {
    return jdbcTemplate;
  }

  public DataSource getDataSource() {
    return dataSource;
  }

  // TODO: 可能会有遗漏的类型
  private Integer toJdbcType(Object param) {
    if (param instanceof BigDecimal) {
      return Types.DECIMAL;
    } else if (param instanceof Number) {
      return Types.NUMERIC;
    } else if (param instanceof Boolean) {
      return Types.BOOLEAN;
    } else if (param instanceof Time || param instanceof LocalTime) {
      return Types.TIME;
    } else if (param instanceof java.sql.Date || param instanceof LocalDate) {
      return Types.DATE;
    } else if (param instanceof LocalDateTime || param instanceof Date) {
      return Types.TIMESTAMP;
    } else if (param instanceof Clob) {
      return Types.CLOB;
    } else if (param instanceof Blob) {
      return Types.BLOB;
    } else if (param instanceof Null || param == null) {
      return Types.NULL;
    }
    return Types.VARCHAR;
  }

  /**
   * 以原生SQL的方式执行存储过程
   *
   * @param sql    SQL语句
   * @param params 参数列表
   * @return Either<Exception, Map < String, Object>> 异常或结果
   */
  public Try<Map<String, Object>> call(String sql, List<SqlParameter> params) {
    return Try.of(() -> {
      try {
        return jdbcTemplate.call(conn -> conn.prepareCall(sql), params);
      } catch (DataModelException e) {
        throw e;
      } catch (Exception e) {
        throw classifyException("执行存储过程时发生错误", e);
      }
    });
  }

}
