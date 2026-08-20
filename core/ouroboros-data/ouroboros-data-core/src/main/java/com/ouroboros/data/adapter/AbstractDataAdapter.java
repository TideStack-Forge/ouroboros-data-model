package com.ouroboros.data.adapter;

import static io.vavr.API.*;

import java.util.*;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.DMLStatements;
import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.dsl.statement.*;
import com.ouroboros.data.exception.*;
import com.ouroboros.data.normalize.QueryNormalizeContext;
import com.ouroboros.data.record.RecordList;
import com.ouroboros.data.transpile.BaseTranspileContext;
import com.ouroboros.data.transpile.FieldSource;
import com.ouroboros.data.transpile.QueryTranspiler;
import com.ouroboros.data.transpile.TranspileContext;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.sql.RelationalPathBase;

public abstract class AbstractDataAdapter implements DataAdapter {

  private static final QueryNormalizeContext DEFAULT_NORMALIZE_CONTEXT =
      QueryNormalizeContext.builder().withDefaultNormalizers().build();

  @Override
  public Try<String> insert(InsertStatement statement) {
    return insert(statement, createMinimalContext(statement.getEntityName()));
  }

  @Override
  public Try<String> insert(InsertStatement statement, TranspileContext context) {
    throw new UnsupportedOperationException("insert(InsertStatement, TranspileContext) not implemented");
  }

  @Override
  public Try<String> insert(Map<String, ?> insertStatement) {
    return DMLStatements.normalizeInsertStatement(insertStatement)
        .flatMap(statement -> {
          if (statement instanceof InsertStatement insert) {
            return insert(insert);
          }
          return Try.failure(new InvalidStatementException("执行插入语句失败，请传入单条数据"));
        });
  }

  @Override
  public Try<String> insert(String entityName, Map<String, ?> data) {
    return DMLStatements.buildInsertStatement(entityName, (Map<String, Object>) data)
        .flatMap(this::insert);
  }

  @Override
  public Try<List<String>> batchInsert(BatchInsertStatement batchInsertStatement) {
    return batchInsert(batchInsertStatement, createMinimalContext(batchInsertStatement.getEntityName()));
  }

  @Override
  public Try<List<String>> batchInsert(BatchInsertStatement batchInsertStatement, TranspileContext context) {
    throw new UnsupportedOperationException("batchInsert(BatchInsertStatement, TranspileContext) not implemented");
  }

  @Override
  public Try<List<String>> batchInsert(Map<String, ?> batchInsertStatement) {
    return DMLStatements.normalizeInsertStatement(batchInsertStatement)
        .flatMap(statement -> {
          if (statement instanceof BatchInsertStatement batchInsert) {
            return batchInsert(batchInsert);
          }
          if (statement instanceof InsertStatement insert) {
            return insert(insert).map(Arrays::asList);
          }
          return Try.failure(new InvalidStatementException("不支持的Insert语句类型"));
        });
  }

  @Override
  public Try<List<String>> batchInsert(String entityName, List<Map<String, ?>> dataList) {
    return DMLStatements.buildBatchInsertStatement(entityName, dataList)
        .flatMap(this::batchInsert);
  }

  @Override
  public Try<Long> update(UpdateStatement statement) {
    return update(statement, createMinimalContext(statement.getEntityName()));
  }

  @Override
  public Try<Long> update(UpdateStatement statement, TranspileContext context) {
    throw new UnsupportedOperationException("update(UpdateStatement, TranspileContext) not implemented");
  }

  @Override
  public Try<Long> update(Map<String, ?> updateStatement) {
    return DMLStatements.normalizeUpdateStatement(updateStatement, DEFAULT_NORMALIZE_CONTEXT.forClause("WHERE"))
        .flatMap(this::update);
  }

  @Override
  public Try<Long> update(String entityName, Map<String, ?> where, Map<String, ?> data) {
    return DMLStatements.buildUpdateStatement(entityName, data, where, DEFAULT_NORMALIZE_CONTEXT.forClause("WHERE"))
        .flatMap(this::update);
  }

  @Override
  public Try<Long> delete(DeleteStatement statement) {
    return delete(statement, createMinimalContext(statement.getEntityName()));
  }

  @Override
  public Try<Long> delete(DeleteStatement statement, TranspileContext context) {
    throw new UnsupportedOperationException("delete(DeleteStatement, TranspileContext) not implemented");
  }

  @Override
  public Try<Long> delete(Map<String, ?> deleteStatement) {
    return DMLStatements.normalizeDeleteStatement(deleteStatement, DEFAULT_NORMALIZE_CONTEXT.forClause("WHERE"))
        .flatMap(this::delete);
  }

  @Override
  public Try<Long> delete(String entityName, Map<String, ?> where) {
    return DMLStatements.buildDeleteStatement(entityName, where, DEFAULT_NORMALIZE_CONTEXT.forClause("WHERE"))
        .flatMap(this::delete);
  }

  @Override
  public Try<RecordList> query(QueryStatement statement) {
    String tableName = statement.getFrom() != null ? statement.getFrom().getTableName() : "";
    var minimalContext = createMinimalContext(tableName);
    var queryMetadata = QueryTranspiler.DEFAULT.applyWithContext(statement, minimalContext);
    if (queryMetadata.isFailure()) {
      var cause = queryMetadata.getCause();
      if (cause instanceof DataModelException dmCause) {
        return Try.failure(dmCause);
      }
      return Try.failure(new TranspileException("转译查询语句时发生错误", cause));
    }
    return queryMetadata.flatMap(this::query);
  }

  @Override
  public Try<RecordList> query(QueryStatement statement, TranspileContext context) {
    var queryMetadata = context.getQueryTranspiler().applyWithContext(statement, context);
    if (queryMetadata.isFailure()) {
      var cause = queryMetadata.getCause();
      if (cause instanceof DataModelException dmCause) {
        return Try.failure(dmCause);
      }
      return Try.failure(new TranspileException("转译查询语句的时候发生错误，请检查字段名、实体名等拼写是否正确", cause));
    }
    return queryMetadata.flatMap(this::query);
  }

  /**
   * 低级 Adapter API：仅执行 Normalize → Transpile，不经过 Analyze 阶段。
   *
   * <p><b>管线差异说明：</b>DataAdapter 走 Normalize → Transpile 最短路径，
   * 不经过 Analyze、Orchestration、Plugin 链、关联改写、填充等处理。
   * 如需完整管线处理（含 Plugin、关联改写、填充等），应通过 DataModel 入口查询。
   *
   * @see com.ouroboros.data.model.DataModel#query(Map)
   */
  @Override
  public Try<RecordList> query(Map<String, ?> queryMap) {
    var queryStatement = DEFAULT_NORMALIZE_CONTEXT.normalizeQuery(queryMap);
    if (queryStatement.isFailure()) {
      var cause = queryStatement.getCause();
      if (cause instanceof DataModelException dmCause) {
        return Try.failure(dmCause);
      }
      return Try.failure(new NormalizeException("准备查询时发现语法错误", cause));
    }

    return queryStatement.flatMap(this::query);
  }

  @Override
  public Try<RecordList> query(String entityName, Map<String, ?> whereMap) {
    return normalizeEntityQuery(entityName, whereMap)
        .flatMap(this::query);
  }

  @Override
  public Try<Long> count(QueryStatement statement) {
    String tableName = statement.getFrom() != null ? statement.getFrom().getTableName() : "";
    var minimalContext = createMinimalContext(tableName);
    var queryMetadata = QueryTranspiler.DEFAULT.applyWithContext(statement, minimalContext);
    if (queryMetadata.isFailure()) {
      var cause = queryMetadata.getCause();
      if (cause instanceof DataModelException dmCause) {
        return Try.failure(dmCause);
      }
      return Try.failure(new TranspileException("转译count语句时发生错误", cause));
    }
    return queryMetadata.flatMap(this::count);
  }

  @Override
  public Try<Long> count(QueryStatement statement, TranspileContext context) {
    var result = Try
        .of(() -> {
          var query = context.getQueryTranspiler().applyWithContext(statement, context).getOrElseThrow(e -> e);
          return count(query).getOrElseThrow(e -> e);
        });
    if (result.isFailure()) {
      var cause = result.getCause();
      if (cause instanceof DataModelException dmCause) {
        return Try.failure(dmCause);
      }
      return Try.failure(new QueryExecutionException("执行count操作时发生未知错误", cause));
    }
    return result;
  }

  @Override
  public Try<Long> count(String entityName, Map<String, ?> whereMap) {
    return normalizeEntityQuery(entityName, whereMap)
        .flatMap(this::count);
  }

  /**
   * 低级 Adapter API：仅执行 Normalize → Transpile，不经过 Analyze 阶段。
   * <p>
   * 高级 API（DataModel.count）负责完整的 Normalize → Analyze → Transpile 链路，
   * 包含类型检查、关联路径验证和查询优化。
   */
  @Override
  public Try<Long> count(Map<String, ?> queryMap) {
    var normalized = DEFAULT_NORMALIZE_CONTEXT.normalizeQuery(queryMap);
    return normalized.flatMap(this::count);
  }

  @Override
  public Try<?> execute(Map<String, ?> statement) {
    return DMLStatements.normalizeStatement(statement, DEFAULT_NORMALIZE_CONTEXT.forClause("WHERE"))
        .flatMap(this::execute);
  }

  @Override
  public Try<?> execute(DMLStatement statement) {
    return Match(statement)
        .of(
            Case($(InsertStatement.class::isInstance), (InsertStatement insert) -> insert(insert)),
            Case($(BatchInsertStatement.class::isInstance), (BatchInsertStatement batchInsert) -> batchInsert(batchInsert)),
            Case($(UpdateStatement.class::isInstance), (UpdateStatement update) -> update(update)),
            Case($(DeleteStatement.class::isInstance), (DeleteStatement delete) -> delete(delete)),
            Case($(),
                () -> Try.failure(new InvalidStatementException("不支持的DML语句类型: " + statement.getClass().getName())))
        );
  }

  @Override
  public Try<?> execute(DMLStatement statement, TranspileContext context) {
    return Match(statement)
        .of(
            Case($(InsertStatement.class::isInstance), (InsertStatement insert) -> insert(insert, context)),
            Case($(BatchInsertStatement.class::isInstance), (BatchInsertStatement batchInsert) -> batchInsert(batchInsert, context)),
            Case($(UpdateStatement.class::isInstance), (UpdateStatement update) -> update(update, context)),
            Case($(DeleteStatement.class::isInstance), (DeleteStatement delete) -> delete(delete, context)),
            Case($(),
                () -> Try.failure(new InvalidStatementException("不支持的DML语句类型: " + statement.getClass().getName())))
        );
  }

  protected TranspileContext createMinimalContext(String tableName) {
    String schemaName = "";
    String physicalTableName = tableName;
    int dotIndex = tableName == null ? -1 : tableName.lastIndexOf('.');
    if (dotIndex > 0) {
      schemaName = tableName.substring(0, dotIndex);
      physicalTableName = tableName.substring(dotIndex + 1);
    }

    Path<?> tablePath = new RelationalPathBase<>(Object.class, physicalTableName, schemaName, physicalTableName);
    return new BaseTranspileContext(
        new FieldSource() {
          @Override
          public Optional<Path<?>> getField(String fieldName) {
            String physicalFieldName = fieldName;
            int fieldDotIndex = fieldName == null ? -1 : fieldName.lastIndexOf('.');
            if (fieldDotIndex > 0) {
              physicalFieldName = fieldName.substring(fieldDotIndex + 1);
            }
            return Optional.of(Expressions.path(Object.class, physicalFieldName));
          }

          @Override
          public List<Path<?>> getFields() {
            return Collections.emptyList();
          }

          @Override
          public Path<?> getSelfPath() {
            return tablePath;
          }
        }, tableName);
  }

  private Try<QueryStatement> normalizeEntityQuery(String entityName, Map<String, ?> whereMap) {
    Map<String, Object> queryMap = new HashMap<>();
    queryMap.put(Keyword.FROM.toString(), entityName);
    if (whereMap != null && !whereMap.isEmpty()) {
      queryMap.put(Keyword.WHERE.toString(), whereMap);
    }

    var queryStatement = DEFAULT_NORMALIZE_CONTEXT.normalizeQuery(queryMap);
    if (queryStatement.isFailure()) {
      var cause = queryStatement.getCause();
      if (cause instanceof DataModelException dmCause) {
        return Try.failure(dmCause);
      }
      return Try.failure(new NormalizeException("准备查询时发现语法错误", cause));
    }
    return queryStatement;
  }
}
