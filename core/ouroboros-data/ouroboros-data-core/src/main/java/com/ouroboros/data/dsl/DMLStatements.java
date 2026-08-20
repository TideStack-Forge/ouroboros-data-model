package com.ouroboros.data.dsl;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MapUtils;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.statement.*;
import com.ouroboros.data.exception.*;
import com.ouroboros.data.normalize.ClauseNormalizeContext;
import com.ouroboros.data.util.Asserts;
import com.ouroboros.data.util.DataLists;
import com.ouroboros.data.util.DataMaps;

public class DMLStatements {

  static Predicate<Object> isEmptyWhere = v ->
      v == null
          || (v instanceof List<?> list && list.isEmpty())
          || (v instanceof Map<?, ?> map && map.isEmpty());

  /**
   * 构造 INSERT 语句
   *
   * @param entityName
   * @param values
   * @return
   */
  public static Try<InsertStatement> buildInsertStatement(String entityName,
                                                          Map<String, Object> values) {
    var result = Try.of(() -> {
      Asserts.assertValidEntityName(entityName);
      Asserts.assertValidValueMap(values);
      return InsertStatement.of(entityName, values);
    });

    if (result.isSuccess()) {
      return result;
    }

    var cause = result.getCause();
    if (cause instanceof StatementError) {
      return Try.failure(cause);
    }
    return Try.failure(new StatementSyntaxException("构造插入语句失败", cause));
  }

  /**
   * 构造 INSERT 语句(多值)
   *
   * @param entityName
   * @param valuesList
   */
  public static Try<BatchInsertStatement> buildBatchInsertStatement(String entityName,
                                                                    List<Map<String, ?>> valuesList) {
    var result = Try.of(() -> {
      // 验证 Entity 名
      Asserts.assertValidEntityName(entityName);

      if (valuesList == null || valuesList.isEmpty()) {
        throw new ValuesListError("Values List is empty", Collections.emptyMap());
      }

      // 校验所有 map 的 key 均为 String 类型（与 normalizeInsertStatement 的 Lists::isStringKeyMapList 校验一致）
      if (!DataLists.isStringKeyMapList(valuesList)) {
        throw new ValuesListError("Values List contains non-string keys", Collections.emptyMap());
      }

      // 先把所有map的key组成set
      var expectFields = valuesList.isEmpty()
          ? Collections.<String>emptySet()
          : valuesList.stream().map(Map::keySet)
          .reduce(new TreeSet<>(String::compareToIgnoreCase), (acc, set) -> {
            acc.addAll(set);
            return acc;
          });

      // 对不存在的值补充null
      List<Map<String, ?>> newValueList = valuesList.stream()
          .map(valueMap -> {
            Map<String, Object> newValueMap = new LinkedHashMap<>();
            expectFields.forEach(field -> {
              newValueMap.put(field, valueMap.get(field));
            });

            return newValueMap;
          })
          .collect(Collectors.toList());
      return BatchInsertStatement.of(entityName, newValueList);
    });

    if (result.isSuccess()) {
      return result;
    }

    var cause = result.getCause();
    if (cause instanceof StatementError) {
      return Try.failure(cause);
    }
    return Try.failure(new StatementSyntaxException("构造批量插入语句失败", cause));
  }

  /**
   * 构造 INSERT 语句(多值)
   *
   * @param entityName
   * @param valuesArray
   */
  @SuppressWarnings("unchecked")
  public static Try<BatchInsertStatement> buildBatchInsertStatement(String entityName,
                                                                    Map<String, ?>... valuesArray) {
    var valuesList = Arrays.asList(valuesArray);
    return buildBatchInsertStatement(entityName, valuesList);
  }

  /**
   * 构造 UPDATE 语句
   *
   * @param entityName
   * @param where
   * @param data
   */
  public static Try<UpdateStatement> buildUpdateStatement(String entityName, Map<String, ?> data,
                                                          Map<?, ?> where,
                                                          ClauseNormalizeContext clauseContext) {
    var result = Try.of(() -> {
      // 验证 Entity 名
      Asserts.assertValidEntityName(entityName);
      // 验证值
      Asserts.assertValidValueMap(data);

      // 规范化 Where
      var whereStatement = clauseContext.normalizeCondition(where, "root").getOrElseThrow(e -> e);

      // 正常返回 UPDATE 语句
      return UpdateStatement.of(entityName, data, whereStatement);
    });

    if (result.isSuccess()) {
      return result;
    }

    var cause = result.getCause();
    if (cause instanceof StatementException) {
      return Try.failure(cause);
    }
    return Try.failure(new StatementSyntaxException("构造更新语句时发生未知错误", cause));
  }

  public static Try<UpdateStatement> buildUpdateStatement(String entityName, Map<String, ?> data,
                                                          SExpression<Boolean> where) {
    var result = Try.of(() -> {
      Asserts.assertValidEntityName(entityName);
      Asserts.assertValidValueMap(data);
      return UpdateStatement.of(
          entityName,
          data,
          where == null ? SExpression.empty(Boolean.class) : where);
    });

    if (result.isSuccess()) {
      return result;
    }

    var cause = result.getCause();
    if (cause instanceof StatementException) {
      return Try.failure(cause);
    }
    return Try.failure(new StatementSyntaxException("构造更新语句时发生未知错误", cause));
  }

  /**
   * 构造 UPDATE 语句
   *
   * @param entityName
   * @param where
   * @param data
   */
  public static Try<UpdateStatement> buildUpdateStatement(String entityName, Map<String, ?> data,
                                                          List<?> where,
                                                          ClauseNormalizeContext clauseContext) {
    var result = Try.of(() -> {
      // 验证 Entity 名
      Asserts.assertValidEntityName(entityName);

      // 验证值
      Asserts.assertValidValueMap(data);

      // 规范化 Where
      var whereStatement = clauseContext.normalizeCondition(where, "root").getOrElseThrow(e -> e);
      // 正常返回 UPDATE 语句
      return UpdateStatement.of(entityName, data, whereStatement);
    });

    if (result.isSuccess()) {
      return result;
    }

    var cause = result.getCause();
    if (cause instanceof StatementException) {
      return Try.failure(cause);
    }
    return Try.failure(new StatementSyntaxException("构造更新语句时发生未知错误", cause));
  }

  /**
   * 构造 DELETE 语句
   *
   * @param entityName
   * @param where
   * @return
   */
  public static Try<DeleteStatement> buildDeleteStatement(String entityName, Map<String, ?> where,
                                                          ClauseNormalizeContext clauseContext) {
    var result = Try.of(() -> {
      Asserts.assertValidEntityName(entityName);
      var whereStatement = clauseContext.normalizeCondition(where, "root").getOrElseThrow(e -> e);
      return DeleteStatement.of(entityName, whereStatement);
    });

    if (result.isSuccess()) {
      return result;
    }

    var cause = result.getCause();
    if (cause instanceof StatementException) {
      return Try.failure(cause);
    }
    return Try.failure(new StatementSyntaxException("构造删除语句时发生未知错误", cause));
  }

  public static Try<DeleteStatement> buildDeleteStatement(String entityName, SExpression<Boolean> where) {
    var result = Try.of(() -> {
      Asserts.assertValidEntityName(entityName);
      return DeleteStatement.of(
          entityName,
          where == null ? SExpression.empty(Boolean.class) : where);
    });

    if (result.isSuccess()) {
      return result;
    }

    var cause = result.getCause();
    if (cause instanceof StatementException) {
      return Try.failure(cause);
    }
    return Try.failure(new StatementSyntaxException("构造删除语句时发生未知错误", cause));
  }

  /**
   * 构造 DELETE 语句
   *
   * @param entityName
   * @param where
   * @return
   */
  public static Try<DeleteStatement> buildDeleteStatement(String entityName, List<?> where,
                                                          ClauseNormalizeContext clauseContext) {
    var result = Try.of(() -> {
      Asserts.assertValidEntityName(entityName);
      var whereStatement = clauseContext.normalizeCondition(where, "root").getOrElseThrow(e -> e);
      return DeleteStatement.of(entityName, whereStatement);
    });

    if (result.isSuccess()) {
      return result;
    }

    var cause = result.getCause();
    if (cause instanceof StatementException) {
      return Try.failure(cause);
    }
    return Try.failure(new StatementSyntaxException("构造删除语句时发生未知错误", cause));
  }

  public static Try<DeleteStatement> buildDeleteAllStatement(String entityName) {
    var result = Try.of(() -> {
      Asserts.assertValidEntityName(entityName);
      return DeleteStatement.of(entityName, null);
    });

    if (result.isSuccess()) {
      return result;
    }

    var cause = result.getCause();
    if (cause instanceof StatementException) {
      return Try.failure(cause);
    }
    return Try.failure(new StatementSyntaxException("构造全部删除语句时发生内部错误", cause));
  }

  /**
   * 规范化 DML 语句（INSERT、UPDATE、DELETE）
   *
   * @param statement
   * @return
   */
  public static Try<? extends DMLStatement> normalizeStatement(Map<String, ?> statement,
                                                               ClauseNormalizeContext clauseContext) {
    if (statement instanceof DMLStatement dmlStatement) {
      return Try.success(dmlStatement);
    }

    var keys = MapUtils.emptyIfNull(statement)
        .keySet()
        .stream()
        .map(String::toUpperCase)
        .collect(Collectors.toList());

    Try<? extends DMLStatement> result;
    if (keys.isEmpty()) {
      result = Try.failure(new StatementError("Statement is empty"));
    } else if (keys.contains(Keyword.INSERT.toString())) {
      result = normalizeInsertStatement(statement);
    } else if (keys.contains(Keyword.UPDATE.toString())) {
      result = normalizeUpdateStatement(statement, clauseContext);
    } else if (keys.contains(Keyword.DELETE.toString())) {
      result = normalizeDeleteStatement(statement, clauseContext);
    } else {
      result = Try.failure(new StatementError("Statement is invalid"));
    }

    if (result.isSuccess()) {
      return result;
    }

    var cause = result.getCause();
    if (cause instanceof StatementException) {
      return Try.failure(cause);
    }
    return Try.failure(new StatementSyntaxException("构造DML语句时发生未知错误", cause));
  }

  /**
   * 规范化 INSERT 语句
   *
   * @param statement
   */
  @SuppressWarnings(value = {"unchecked"})
  public static Try<DMLStatement> normalizeInsertStatement(Map<String, ?> statement) {

    // 提取 INSERT 相关字段
    var remappedStatement = DataMaps.remap(statement, String::toUpperCase, Object.class::cast);
    var entityName = String.valueOf(remappedStatement.get(Keyword.INSERT.toString()));
    var values = remappedStatement.get(Keyword.VALUES.toString());
    if ("null".equalsIgnoreCase(entityName) && remappedStatement.get(Keyword.DELETE.toString()) == null) {
      return Try.failure(new InvalidEntityNameException("构造插入语句失败，实体名不能为空", entityName));
    }

    if (values instanceof Map<?, ?> map && DataMaps.isStringKeyMap(map)) {
      return buildInsertStatement(entityName, (Map<String, Object>) map)
          .map(stm -> stm);
    } else if (values instanceof List<?> list && DataLists.isStringKeyMapList(list)) {
      return buildBatchInsertStatement(entityName, (List<Map<String, ?>>) list)
          .map(stm -> stm);
    } else {
      return Try.failure(new InvalidStatementException("构造插入语句失败，不支持的values类型，values只支持 Map 和 List<Map<String, ?>>"));
    }
  }

  /**
   * 规范化 UPDATE 语句
   *
   * @param statement
   */
  @SuppressWarnings(value = {"unchecked"})
  public static Try<UpdateStatement> normalizeUpdateStatement(Map<String, ?> statement,
                                                              ClauseNormalizeContext clauseContext) {
    if (statement instanceof UpdateStatement updateStatement) {
      return Try.success(updateStatement);
    }

    // 提取 UPDATE 相关字段
    var remappedStatement = DataMaps.remap(statement, String::toUpperCase, Object.class::cast);
    var entityName = String.valueOf(remappedStatement.get(Keyword.UPDATE.toString()));
    var values = remappedStatement.get(Keyword.SET.toString());
    if ("null".equalsIgnoreCase(entityName) && remappedStatement.get(Keyword.DELETE.toString()) == null) {
      return Try.failure(new InvalidEntityNameException("构造更新语句失败，实体名不能为空", entityName));
    }
    var whereRaw = remappedStatement.get(Keyword.WHERE.toString());

    // 验证值
    if (!(values instanceof Map<?, ?> valuesMap)) {
      return Try.failure(new InvalidStatementException("构造更新语句失败，不支持的values子句类型，values子句只支持Map类型"));
    }

    if (isEmptyWhere.test(whereRaw)) {
      return Try.success(UpdateStatement.of(entityName, (Map<String, ?>) valuesMap));
    } else if (whereRaw instanceof Map<?, ?> map) {
      return buildUpdateStatement(entityName, (Map<String, ?>) valuesMap, map, clauseContext);
    } else if (whereRaw instanceof List<?> list) {
      return buildUpdateStatement(entityName, (Map<String, ?>) valuesMap, list, clauseContext);
    } else {
      return Try.failure(new InvalidStatementException("构造更新语句失败，不支持的where子句类型，where子句只支持Map<String, ?>和List<Map<String, ?>>类型"));
    }
  }

  /**
   * 规范化 DELETE 语句
   *
   * @param statement
   */
  @SuppressWarnings(value = {"unchecked"})
  public static Try<DeleteStatement> normalizeDeleteStatement(Map<String, ?> statement,
                                                              ClauseNormalizeContext clauseContext) {
    // 如果已经是 DeleteStatement 实例，直接返回
    if (statement instanceof DeleteStatement deleteStatement) {
      return Try.success(deleteStatement);
    }

    // 提取 DELETE 相关字段
    var remappedStatement = DataMaps.remap(statement, String::toUpperCase, Object.class::cast);
    var entityName = String.valueOf(remappedStatement.get(Keyword.DELETE.toString()));
    var where = remappedStatement.get(Keyword.WHERE.toString());
    if ("null".equalsIgnoreCase(entityName) && remappedStatement.get(Keyword.DELETE.toString()) == null) {
      return Try.failure(new InvalidEntityNameException("构造删除语句失败，实体名不能为空", entityName));
    }

    Try<DeleteStatement> result;
    if (isEmptyWhere.test(where)) {
      result = buildDeleteAllStatement(entityName);
    } else if (where instanceof Map<?, ?> map && DataMaps.isStringKeyMap(map)) {
      result = buildDeleteStatement(entityName, (Map<String, ?>) map, clauseContext);
    } else if (where instanceof List<?> list) {
      result = buildDeleteStatement(entityName, list, clauseContext);
    } else {
      result = Try.failure(new InvalidStatementException("Invalid delete statement"));
    }

    if (result.isSuccess()) {
      return result;
    }

    var cause = result.getCause();
    if (cause instanceof StatementException) {
      return Try.failure(cause);
    }
    return Try.failure(new StatementSyntaxException("构造删除语句时发生未知错误", cause));
  }

}
