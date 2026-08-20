package com.ouroboros.data.model.plugins;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.ModelQueryStatement;
import com.ouroboros.data.dsl.statement.ModelQueryStatementBuilder;
import com.ouroboros.data.dsl.statement.OmitClause;
import com.ouroboros.data.dsl.statement.PopulateClause;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelPlugin;
import com.ouroboros.data.model.DataModelPluginBuilder;
import com.ouroboros.data.model.DataModelPluginContext;
import com.ouroboros.data.record.Record;
import com.ouroboros.data.record.RecordList;

public class ProbeDataModelPlugin implements DataModelPlugin {
  public static final List<String> CALLS = new ArrayList<String>();
  public static Map<String, Object> incomingQueryStatement;
  public static Map<String, Object> lastQueryStatement;
  public static boolean ADD_OMIT;
  public static boolean ADD_WHERE_FILTER;
  public static boolean ADD_QUERY_FACADE_DECORATIONS;
  public static boolean MUTATE_WHERE_IN_PLACE;
  public static boolean MUTATE_ROOT_SUBQUERY_IN_PLACE;
  public static boolean MUTATE_ROOT_MODEL_SUBQUERY_IN_PLACE;
  public static boolean MUTATE_NESTED_MODEL_SUBQUERY_IN_PLACE;
  public static boolean REPLACE_ARRAY_CONSTANT_WITH_LIST;
  public static boolean MUTATE_NUMBER_IN_PLACE;
  public static boolean MUTATE_STABLE_TEXT_NUMBER_IN_PLACE;
  public static boolean REPLACE_FROM;
  public static DataModel NESTED_RAW_QUERY_MODEL;
  public static Map<String, Object> NESTED_RAW_QUERY;

  public static void reset() {
    CALLS.clear();
    incomingQueryStatement = null;
    lastQueryStatement = null;
    ADD_OMIT = false;
    ADD_WHERE_FILTER = false;
    ADD_QUERY_FACADE_DECORATIONS = false;
    MUTATE_WHERE_IN_PLACE = false;
    MUTATE_ROOT_SUBQUERY_IN_PLACE = false;
    MUTATE_ROOT_MODEL_SUBQUERY_IN_PLACE = false;
    MUTATE_NESTED_MODEL_SUBQUERY_IN_PLACE = false;
    REPLACE_ARRAY_CONSTANT_WITH_LIST = false;
    MUTATE_NUMBER_IN_PLACE = false;
    MUTATE_STABLE_TEXT_NUMBER_IN_PLACE = false;
    REPLACE_FROM = false;
    NESTED_RAW_QUERY_MODEL = null;
    NESTED_RAW_QUERY = null;
  }

  @Override
  public Try<Record> insert(Map<String, Object> data, DataModelPluginContext context) {
    CALLS.add("insert");
    return context.getNextPlugin().insert(data, context.getNextPluginContext());
  }

  @Override
  public Try<RecordList> batchInsert(List<Map<String, Object>> dataList, DataModelPluginContext context) {
    CALLS.add("batchInsert");
    return context.getNextPlugin().batchInsert(dataList, context.getNextPluginContext());
  }

  @Override
  public Try<Long> update(SExpression<Boolean> where, Map<String, Object> data, DataModelPluginContext context) {
    CALLS.add("updateWhere");
    return context.getNextPlugin().update(where, data, context.getNextPluginContext());
  }

  @Override
  public Try<Long> delete(SExpression<Boolean> where, DataModelPluginContext context) {
    CALLS.add("deleteWhere");
    return context.getNextPlugin().delete(where, context.getNextPluginContext());
  }

  @Override
  public Try<RecordList> query(QueryStatement statement, DataModelPluginContext context) {
    CALLS.add("query");
    QueryStatement.QueryStatementBuilder builder = statement.getBuilder();
    incomingQueryStatement = statement;
    if (ADD_OMIT) {
      if (!(builder instanceof ModelQueryStatementBuilder)) {
        builder = new ModelQueryStatementBuilder(statement);
      }
      if (builder instanceof ModelQueryStatementBuilder modelBuilder) {
        modelBuilder.omitClause(OmitClause.fromRaw(java.util.Collections.singletonList("secret")));
      }
    }
    if (ADD_WHERE_FILTER) {
      builder.where(SExpression.create(Operators.EQ, SExpression.field("tenantId"), SExpression.constant("tenant-1")));
    }
    if (ADD_QUERY_FACADE_DECORATIONS) {
      if (!(builder instanceof ModelQueryStatementBuilder)) {
        builder = new ModelQueryStatementBuilder(builder.build());
      }
      builder.replaceSelect(java.util.Collections.singletonList(SExpression.field("pluginName")));
      builder.where(SExpression.create(Operators.EQ, SExpression.field("tenantId"), SExpression.constant("tenant-1")));
      if (builder instanceof ModelQueryStatementBuilder modelBuilder) {
        modelBuilder.populateClause(PopulateClause.fromRaw(java.util.Collections.singletonList(
            java.util.Collections.singletonMap("department",
                java.util.Collections.singletonMap("SELECT", java.util.Collections.singletonList("name"))))));
      } else {
        builder.putRawPopulate(java.util.Collections.singletonMap("department",
            java.util.Collections.singletonMap("SELECT", java.util.Collections.singletonList("name"))));
      }
    }
    QueryStatement copiedStatement = builder.build();
    if (MUTATE_WHERE_IN_PLACE) {
      SExpression<Boolean> where = copiedStatement.getWhere();
      if (!where.isEmpty()) {
        where.setParam(1, SExpression.constant("mutated"));
      }
    }
    if (MUTATE_ROOT_SUBQUERY_IN_PLACE) {
      Object from = copiedStatement.get(Keyword.FROM.toString());
      if (from instanceof QueryStatement.TableSource tableSource && tableSource.isSubQuery()) {
        SExpression<Boolean> subqueryWhere = tableSource.getSubQuery().getWhere();
        if (!subqueryWhere.isEmpty()) {
          subqueryWhere.setParam(1, SExpression.constant(Boolean.FALSE));
        }
      }
    }
    if (MUTATE_ROOT_MODEL_SUBQUERY_IN_PLACE) {
      Object from = copiedStatement.get(Keyword.FROM.toString());
      if (from instanceof QueryStatement.TableSource tableSource
          && tableSource.getSubQuery() instanceof ModelQueryStatement modelSubquery) {
        clearModelClauses(modelSubquery);
      }
    }
    if (MUTATE_NESTED_MODEL_SUBQUERY_IN_PLACE) {
      Object from = copiedStatement.get(Keyword.FROM.toString());
      if (from instanceof QueryStatement.TableSource tableSource && tableSource.isSubQuery()) {
        QueryStatement.TableSource nestedFrom = tableSource.getSubQuery().getFrom();
        if (nestedFrom != null
            && nestedFrom.getSubQuery() instanceof ModelQueryStatement modelSubquery) {
          clearModelClauses(modelSubquery);
        }
      }
    }
    if (REPLACE_ARRAY_CONSTANT_WITH_LIST
        || MUTATE_NUMBER_IN_PLACE
        || MUTATE_STABLE_TEXT_NUMBER_IN_PLACE) {
      SExpression<Boolean> expression = copiedStatement.getWhere();
      if (!expression.isEmpty()) {
        SExpression<?> constant = expression.getParamAsSExpression(1);
        if (REPLACE_ARRAY_CONSTANT_WITH_LIST) {
          expression.setParam(1, SExpression.constant(Arrays.asList((byte) 1, (byte) 2)));
        } else if (constant.getParam(0) instanceof AtomicInteger number) {
          number.set(2);
        } else if (MUTATE_STABLE_TEXT_NUMBER_IN_PLACE
            && constant.getParam(0) instanceof StableTextNumber number) {
          number.set(2);
        }
      }
    }
    if (REPLACE_FROM) {
      copiedStatement = copiedStatement.getBuilder()
          .from(new QueryStatement.TableSource("replacement_table", "replacement"))
          .build();
    }
    if (NESTED_RAW_QUERY_MODEL != null) {
      DataModel nestedModel = NESTED_RAW_QUERY_MODEL;
      Map<String, Object> nestedStatement = NESTED_RAW_QUERY;
      NESTED_RAW_QUERY_MODEL = null;
      NESTED_RAW_QUERY = null;
      Try<RecordList> nestedResult = nestedModel.query(nestedStatement);
      if (nestedResult.isFailure()) {
        return nestedResult;
      }
    }
    lastQueryStatement = copiedStatement;
    return context.getNextPlugin().query(copiedStatement, context.getNextPluginContext());
  }

  private void clearModelClauses(ModelQueryStatement statement) {
    statement.getPopulateClause().getEntries().clear();
    statement.getOmitClause().getFields().clear();
  }

  public static final class StableTextNumber extends Number {
    private static final long serialVersionUID = 1L;

    private int value;

    public StableTextNumber(int value) {
      this.value = value;
    }

    public void set(int value) {
      this.value = value;
    }

    @Override
    public int intValue() {
      return value;
    }

    @Override
    public long longValue() {
      return value;
    }

    @Override
    public float floatValue() {
      return value;
    }

    @Override
    public double doubleValue() {
      return value;
    }

    @Override
    public String toString() {
      return "stable";
    }
  }

  public static class Builder implements DataModelPluginBuilder {
    @Override
    public boolean support(String name) {
      return "Probe".equalsIgnoreCase(name);
    }

    @Override
    public Optional<com.ouroboros.data.model.DataModelPlugin> build(DataModel dataModel, Map<String, Object> config) {
      return Optional.<com.ouroboros.data.model.DataModelPlugin>of(new ProbeDataModelPlugin());
    }
  }
}
