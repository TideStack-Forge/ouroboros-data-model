package com.ouroboros.data.model.plugins;

import java.util.Map;
import java.util.Optional;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelPlugin;
import com.ouroboros.data.model.DataModelPluginBuilder;
import com.ouroboros.data.model.DataModelPluginContext;
import com.ouroboros.data.record.RecordList;

public class ProbeDataModelPlugin implements DataModelPlugin {
  public static boolean REPLACE_FROM;

  public static void reset() {
    REPLACE_FROM = false;
  }

  @Override
  public Try<RecordList> query(QueryStatement statement, DataModelPluginContext context) {
    QueryStatement copiedStatement = statement;
    if (REPLACE_FROM) {
      copiedStatement = statement.getBuilder()
          .from(new QueryStatement.TableSource("replacement_table", "replacement"))
          .build();
    }
    return context.getNextPlugin().query(copiedStatement, context.getNextPluginContext());
  }

  public static class Builder implements DataModelPluginBuilder {
    @Override
    public boolean support(String name) {
      return "Probe".equalsIgnoreCase(name);
    }

    @Override
    public Optional<DataModelPlugin> build(DataModel dataModel, Map<String, Object> config) {
      return Optional.of(new ProbeDataModelPlugin());
    }
  }
}
