package com.ouroboros.data.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.vavr.control.Try;

import com.ouroboros.data.exception.InvalidStatementException;
import com.ouroboros.data.exception.StatementException;

/**
 * 数据模型按主键构造查询、更新和删除条件的共用工具。
 */
public final class DataModelPredicateBuilder {

  private DataModelPredicateBuilder() {
  }

  public static Try<Map<String, Object>> buildIdPredicate(DataModel model, Object id) {
    var result = Try.of(() -> {
      List<DataModelField> primaryKeys = model.getPrimaryKeys();
      if (primaryKeys.size() > 1) {
        throw new IllegalArgumentException("The model has more than one primary key");
      }
      if (primaryKeys.isEmpty()) {
        throw new IllegalArgumentException("The model has no primary key");
      }
      var pk = primaryKeys.get(0);
      var pkName = pk.getName();
      var pkValue = pk.getValueType().convertToPersistentValue(id);
      if (pkValue == null || (pkValue instanceof String && ((String) pkValue).isEmpty())) {
        throw new IllegalArgumentException("The primary key value is invalid");
      }
      return Collections.singletonMap(pkName, pkValue);
    });

    if (result.isSuccess()) {
      return result;
    }

    var cause = result.getCause();
    if (cause instanceof StatementException) {
      return Try.failure(cause);
    }
    return Try.failure(new InvalidStatementException("构造" + model.getFullName() + "模型的ID条件时发生错误", cause));
  }

  public static Try<Map<String, Object>> buildIdsPredicate(DataModel model, List<?> ids) {
    var result = Try.of(() -> {
      List<DataModelField> primaryKeys = model.getPrimaryKeys();
      if (primaryKeys.size() > 1) {
        throw new IllegalArgumentException("The model has more than one primary key");
      }
      if (primaryKeys.isEmpty()) {
        throw new IllegalArgumentException("The model has no primary key");
      }
      var pk = primaryKeys.get(0);
      var pkName = pk.getName();
      List<Object> pkValues = new ArrayList<>(ids.size());
      for (Object id : ids) {
        pkValues.add(pk.getValueType().convertToPersistentValue(id));
      }
      if (pkValues.stream().anyMatch(v -> v == null || (v instanceof String && ((String) v).isEmpty()))) {
        throw new IllegalArgumentException("The primary key value is invalid");
      }
      return Collections.<String, Object>singletonMap(pkName, pkValues);
    });

    if (result.isSuccess()) {
      return result;
    }

    var cause = result.getCause();
    if (cause instanceof StatementException) {
      return Try.failure(cause);
    }
    return Try.failure(new InvalidStatementException("构造" + model.getFullName() + "模型的ID条件时发生错误", cause));
  }
}
