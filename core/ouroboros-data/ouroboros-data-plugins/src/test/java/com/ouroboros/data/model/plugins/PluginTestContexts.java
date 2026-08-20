package com.ouroboros.data.model.plugins;

import java.util.List;
import java.util.Map;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelPlugin;
import com.ouroboros.data.model.DataModelPluginContext;
import com.ouroboros.data.record.Record;
import com.ouroboros.data.record.RecordList;

final class PluginTestContexts {
  private PluginTestContexts() {}

  static DataModelPluginContext withNext(DataModelPlugin nextPlugin) {
    return withNext(null, null, nextPlugin);
  }

  static DataModelPluginContext withNext(DataModel dataModel, DataModelPlugin nextPlugin) {
    return withNext(dataModel, dataModel, nextPlugin);
  }

  static DataModelPluginContext withNext(DataModel dataModel, DataModel coreDataModel, DataModelPlugin nextPlugin) {
    return new SimpleContext(dataModel, coreDataModel, nextPlugin, terminal(dataModel, coreDataModel));
  }

  private static DataModelPluginContext terminal(DataModel dataModel, DataModel coreDataModel) {
    return new SimpleContext(dataModel, coreDataModel, new TerminalPlugin(), null);
  }

  private record SimpleContext(
      DataModel dataModel,
      DataModel coreDataModel,
      DataModelPlugin nextPlugin,
      DataModelPluginContext nextPluginContext) implements DataModelPluginContext {
    @Override
    public DataModel getDataModel() {
      return dataModel;
    }

    @Override
    public DataModel getCoreDataModel() {
      return coreDataModel;
    }

    @Override
    public DataModelPlugin getNextPlugin() {
      return nextPlugin;
    }

    @Override
    public DataModelPluginContext getNextPluginContext() {
      return nextPluginContext;
    }
  }

  private static final class TerminalPlugin implements DataModelPlugin {
    @Override
    public Try<Record> insert(Map<String, Object> data, DataModelPluginContext context) {
      return unsupported();
    }

    @Override
    public Try<RecordList> batchInsert(List<Map<String, Object>> dataList, DataModelPluginContext context) {
      return unsupported();
    }

    @Override
    public Try<Long> update(SExpression<Boolean> where, Map<String, Object> data, DataModelPluginContext context) {
      return unsupported();
    }

    @Override
    public Try<Long> delete(SExpression<Boolean> where, DataModelPluginContext context) {
      return unsupported();
    }

    @Override
    public Try<Long> count(QueryStatement statement, DataModelPluginContext context) {
      return unsupported();
    }

    @Override
    public Try<RecordList> query(QueryStatement statement, DataModelPluginContext context) {
      return unsupported();
    }

    private <T> Try<T> unsupported() {
      return Try.failure(new UnsupportedOperationException("No next plugin configured for test context"));
    }
  }
}
