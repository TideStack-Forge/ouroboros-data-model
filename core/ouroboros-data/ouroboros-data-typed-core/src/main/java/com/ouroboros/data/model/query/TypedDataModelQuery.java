package com.ouroboros.data.model.query;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.query.PopulateSpec;
import com.ouroboros.data.dsl.query.QueryCondition;
import com.ouroboros.data.dsl.query.QueryExpression;
import com.ouroboros.data.model.PluginDescriptor;

/**
 * Runtime-bound typed data model query facade.
 *
 * @param <PK> primary key type
 * @param <M>  model type
 */
public interface TypedDataModelQuery<PK, M> {

  TypedDataModelQuery<PK, M> select(QueryExpression<?>... expressions);

  TypedDataModelQuery<PK, M> select(String select);

  TypedDataModelQuery<PK, M> select(Collection<?> select);

  TypedDataModelQuery<PK, M> select(Object[] select);

  TypedDataModelQuery<PK, M> select(Map<String, ?> select);

  TypedDataModelQuery<PK, M> select(SExpression<?> select);

  TypedDataModelQuery<PK, M> where(QueryCondition... conditions);

  TypedDataModelQuery<PK, M> where(Collection<?> where);

  TypedDataModelQuery<PK, M> where(Map<String, ?> where);

  TypedDataModelQuery<PK, M> where(SExpression<Boolean> where);

  TypedDataModelQuery<PK, M> andWhere(QueryCondition... conditions);

  TypedDataModelQuery<PK, M> andWhere(Collection<?> where);

  TypedDataModelQuery<PK, M> andWhere(Map<String, ?> where);

  TypedDataModelQuery<PK, M> andWhere(SExpression<Boolean> where);

  TypedDataModelQuery<PK, M> orWhere(QueryCondition... conditions);

  TypedDataModelQuery<PK, M> orWhere(Collection<?> where);

  TypedDataModelQuery<PK, M> orWhere(Map<String, ?> where);

  TypedDataModelQuery<PK, M> orWhere(SExpression<Boolean> where);

  TypedDataModelQuery<PK, M> populate(String populate);

  TypedDataModelQuery<PK, M> populate(String... fields);

  TypedDataModelQuery<PK, M> populate(Collection<?> populate);

  TypedDataModelQuery<PK, M> populate(Map<String, ?> populate);

  TypedDataModelQuery<PK, M> populate(PopulateSpec... populates);

  TypedDataModelQuery<PK, M> clause(String clauseName, Object clause);

  TypedDataModelQuery<PK, M> clauses(Map<String, ?> clauses);

  TypedDataModelQuery<PK, M> withPlugins(PluginDescriptor... plugins);

  TypedDataModelQuery<PK, M> withoutPlugins(String... pluginNames);

  Map<String, Object> build();

  Try<List<M>> execute();
}
