package com.ouroboros.data.dsl.query;

import java.util.Collection;
import java.util.Map;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.model.PluginDescriptor;
import com.ouroboros.data.record.RecordList;

/**
 * Runtime-bound data model query facade.
 */
public interface DataModelQuery {

  DataModelQuery select(QueryExpression<?>... expressions);

  DataModelQuery select(String select);

  DataModelQuery select(Collection<?> select);

  DataModelQuery select(Object[] select);

  DataModelQuery select(Map<String, ?> select);

  DataModelQuery select(SExpression<?> select);

  DataModelQuery where(QueryCondition... conditions);

  DataModelQuery where(Collection<?> where);

  DataModelQuery where(Map<String, ?> where);

  DataModelQuery where(SExpression<Boolean> where);

  DataModelQuery andWhere(QueryCondition... conditions);

  DataModelQuery andWhere(Collection<?> where);

  DataModelQuery andWhere(Map<String, ?> where);

  DataModelQuery andWhere(SExpression<Boolean> where);

  DataModelQuery orWhere(QueryCondition... conditions);

  DataModelQuery orWhere(Collection<?> where);

  DataModelQuery orWhere(Map<String, ?> where);

  DataModelQuery orWhere(SExpression<Boolean> where);

  DataModelQuery populate(String populate);

  DataModelQuery populate(String... fields);

  DataModelQuery populate(Collection<?> populate);

  DataModelQuery populate(Map<String, ?> populate);

  DataModelQuery populate(PopulateSpec... populates);

  DataModelQuery clause(String clauseName, Object clause);

  DataModelQuery clauses(Map<String, ?> clauses);

  DataModelQuery withPlugins(PluginDescriptor... plugins);

  DataModelQuery withoutPlugins(String... pluginNames);

  Map<String, Object> build();

  Try<RecordList> execute();
}
