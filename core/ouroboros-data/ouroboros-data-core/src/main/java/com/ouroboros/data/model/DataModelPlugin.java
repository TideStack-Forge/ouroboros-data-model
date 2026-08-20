package com.ouroboros.data.model;

import java.util.List;
import java.util.Map;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.record.Record;
import com.ouroboros.data.record.RecordList;

public interface DataModelPlugin {
  default Try<Record> insert(Map<String, Object> data, DataModelPluginContext context) {
    return context.getNextPlugin().insert(data, context.getNextPluginContext());
  }

  default Try<RecordList> batchInsert(List<Map<String, Object>> dataList, DataModelPluginContext context) {
    return context.getNextPlugin().batchInsert(dataList, context.getNextPluginContext());
  }

  default Try<Long> update(SExpression<Boolean> where, Map<String, Object> data, DataModelPluginContext context) {
    return context.getNextPlugin().update(where, data, context.getNextPluginContext());
  }

  default Try<Long> delete(SExpression<Boolean> where, DataModelPluginContext context) {
    return context.getNextPlugin().delete(where, context.getNextPluginContext());
  }

  default Try<Long> count(QueryStatement statement, DataModelPluginContext context) {
    return context.getNextPlugin().count(statement, context.getNextPluginContext());
  }

  default Try<RecordList> query(QueryStatement statement, DataModelPluginContext context) {
    return context.getNextPlugin().query(statement, context.getNextPluginContext());
  }
}
