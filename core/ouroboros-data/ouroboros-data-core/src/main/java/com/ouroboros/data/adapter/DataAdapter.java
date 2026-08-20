package com.ouroboros.data.adapter;

import java.util.List;
import java.util.Map;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.statement.*;
import com.ouroboros.data.record.RecordList;
import com.ouroboros.data.transpile.TranspileContext;
import com.querydsl.core.QueryMetadata;

/**
 * 数据访问适配器，实现对数据源的数据访问方法的封装。
 * 如数据库的增、删、改、查。
 *
 * @author Song Mingxu
 * @version 1.0.0
 */
@SuppressWarnings("unused")
public interface DataAdapter {

  /**
   * 插入一条数据（使用默认 Context）
   *
   * @param statement 插入语句
   * @return 插入数据的主键
   * @since 1.0.0-beta.2
   */
  Try<String> insert(InsertStatement statement);

  /**
   * 插入一条数据（使用指定 TranspileContext）
   *
   * @param statement 插入语句
   * @param context   转译上下文
   * @return 插入数据的主键
   * @since 1.0.0-beta.2
   */
  Try<String> insert(InsertStatement statement, TranspileContext context);

  /**
   * 插入一条数据
   *
   * @param insertStatement 原始格式的Statement
   * @return 插入数据的主键
   */
  Try<String> insert(Map<String, ?> insertStatement);

  /**
   * 插入一条数据
   *
   * @param entityName 表名
   * @param data       数据
   * @return 插入数据的主键
   */
  Try<String> insert(String entityName, Map<String, ?> data);

  /**
   * 插入多条数据
   *
   * @return 插入的数据的主键列表
   */
  Try<List<String>> batchInsert(BatchInsertStatement batchInsertStatement);

  /**
   * 插入多条数据（使用指定 TranspileContext）
   *
   * @param batchInsertStatement 批量插入语句
   * @param context              转译上下文
   * @return 插入的数据的主键列表
   * @since 1.0.0-beta.2
   */
  Try<List<String>> batchInsert(BatchInsertStatement batchInsertStatement, TranspileContext context);

  /**
   * 插入多条数据
   *
   * @return 插入的数据的主键列表
   */
  Try<List<String>> batchInsert(Map<String, ?> batchInsertStatement);

  /**
   * 批量创建数据
   *
   * @param entityName 表名
   * @param dataList   数据列表
   * @return 批量创建的数据列表
   */
  Try<List<String>> batchInsert(String entityName, List<Map<String, ?>> dataList);

  /**
   * 更新数据（使用默认 Context）
   *
   * @param statement 更新语句
   * @return 更新数量
   * @since 1.0.0-beta.2
   */
  Try<Long> update(UpdateStatement statement);

  /**
   * 更新数据（使用指定 TranspileContext）
   *
   * @param statement 更新语句
   * @param context   转译上下文
   * @return 更新数量
   * @since 1.0.0-beta.2
   */
  Try<Long> update(UpdateStatement statement, TranspileContext context);

  /**
   * 更新数据
   *
   * @return 更新数量
   */
  Try<Long> update(Map<String, ?> updateStatement);

  /**
   * 更新数据
   *
   * @param entityName 表名
   * @param where      条件
   * @param data       数据
   * @return 更新数量
   */
  Try<Long> update(String entityName, Map<String, ?> where, Map<String, ?> data);

  /**
   * 删除数据（使用默认 Context）
   *
   * @param statement 删除语句
   * @return 删除数量
   * @since 1.0.0-beta.2
   */
  Try<Long> delete(DeleteStatement statement);

  /**
   * 删除数据（使用指定 TranspileContext）
   *
   * @param statement 删除语句
   * @param context   转译上下文
   * @return 删除数量
   * @since 1.0.0-beta.2
   */
  Try<Long> delete(DeleteStatement statement, TranspileContext context);

  /**
   * 删除数据
   *
   * @return 删除数量
   */
  Try<Long> delete(Map<String, ?> deleteStatement);

  /**
   * 删除数据
   *
   * @param entityName 表名
   * @param where      条件
   * @return 删除数量
   */
  Try<Long> delete(String entityName, Map<String, ?> where);


  /**
   * 查询数据
   *
   * @param queryMetadata 查询元数据
   * @return 结果列表
   */
  Try<RecordList> query(QueryMetadata queryMetadata);

  /**
   * 查询数据（使用默认 Context）
   *
   * @param statement 查询语句
   * @return 结果列表
   * @since 1.0.0-beta.2
   */
  Try<RecordList> query(QueryStatement statement);

  /**
   * 查询数据（使用指定 TranspileContext）
   *
   * @param statement 查询语句
   * @param context   转译上下文
   * @return 结果列表
   * @since 1.0.0-beta.2
   */
  Try<RecordList> query(QueryStatement statement, TranspileContext context);

  /**
   * 查询数据
   *
   * @param queryStatement 查询DSL
   * @return 结果列表
   */
  Try<RecordList> query(Map<String, ?> queryStatement);

  /**
   * 查询数据
   *
   * @param entityName 表名
   * @param whereMap   条件
   * @return 结果列表
   */
  Try<RecordList> query(String entityName, Map<String, ?> whereMap);

  /**
   * 查询数据数量
   *
   * @return 数量
   */
  Try<Long> count(QueryMetadata queryMetadata);

  /**
   * 查询数据数量（使用默认 Context）
   *
   * @param statement 查询语句
   * @return 数量
   * @since 1.0.0-beta.2
   */
  Try<Long> count(QueryStatement statement);

  /**
   * 查询数据数量（使用指定 TranspileContext）
   *
   * @param statement 查询语句
   * @param context   转译上下文
   * @return 数量
   * @since 1.0.0-beta.2
   */
  Try<Long> count(QueryStatement statement, TranspileContext context);

  /**
   * 查询数据数量
   *
   * @param entityName 表名
   * @param whereMap   条件
   * @return 数量
   */
  Try<Long> count(String entityName, Map<String, ?> whereMap);

  /**
   * 查询数据数量
   *
   * @param queryMap 条件
   * @return 数量
   */
  Try<Long> count(Map<String, ?> queryMap);

  /**
   * 执行DML语句
   */
  Try<?> execute(DMLStatement statement);

  /**
   * 执行DML语句（带 TranspileContext）
   */
  Try<?> execute(DMLStatement statement, TranspileContext context);

  /**
   * 执行DML语句
   */
  Try<?> execute(Map<String, ?> statement);
}
