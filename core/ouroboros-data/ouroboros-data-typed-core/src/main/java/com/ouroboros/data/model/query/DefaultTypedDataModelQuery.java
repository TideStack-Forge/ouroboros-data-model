package com.ouroboros.data.model.query;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.query.DataModelQueries;
import com.ouroboros.data.dsl.query.DataModelQuery;
import com.ouroboros.data.dsl.query.PopulateSpec;
import com.ouroboros.data.dsl.query.QueryCondition;
import com.ouroboros.data.dsl.query.QueryExpression;
import com.ouroboros.data.model.PluginDescriptor;
import com.ouroboros.data.model.TypedDataModel;

final class DefaultTypedDataModelQuery<PK, M> implements TypedDataModelQuery<PK, M> {
  private TypedDataModel<PK, M> typedDataModel;
  private final DataModelQuery query;

  DefaultTypedDataModelQuery(TypedDataModel<PK, M> typedDataModel) {
    this.typedDataModel = typedDataModel;
    this.query = DataModelQueries.from(typedDataModel.getDataModel());
  }

  @Override
  public TypedDataModelQuery<PK, M> select(QueryExpression<?>... expressions) {
    query.select(expressions);
    return this;
  }

  @Override
  public TypedDataModelQuery<PK, M> select(String select) {
    query.select(select);
    return this;
  }

  @Override
  public TypedDataModelQuery<PK, M> select(Collection<?> select) {
    query.select(select);
    return this;
  }

  @Override
  public TypedDataModelQuery<PK, M> select(Object[] select) {
    query.select(select);
    return this;
  }

  @Override
  public TypedDataModelQuery<PK, M> select(Map<String, ?> select) {
    query.select(select);
    return this;
  }

  @Override
  public TypedDataModelQuery<PK, M> select(SExpression<?> select) {
    query.select(select);
    return this;
  }

  @Override
  public TypedDataModelQuery<PK, M> where(QueryCondition... conditions) {
    query.where(conditions);
    return this;
  }

  @Override
  public TypedDataModelQuery<PK, M> where(Collection<?> where) {
    query.where(where);
    return this;
  }

  @Override
  public TypedDataModelQuery<PK, M> where(Map<String, ?> where) {
    query.where(where);
    return this;
  }

  @Override
  public TypedDataModelQuery<PK, M> where(SExpression<Boolean> where) {
    query.where(where);
    return this;
  }

  @Override
  public TypedDataModelQuery<PK, M> andWhere(QueryCondition... conditions) {
    query.andWhere(conditions);
    return this;
  }

  @Override
  public TypedDataModelQuery<PK, M> andWhere(Collection<?> where) {
    query.andWhere(where);
    return this;
  }

  @Override
  public TypedDataModelQuery<PK, M> andWhere(Map<String, ?> where) {
    query.andWhere(where);
    return this;
  }

  @Override
  public TypedDataModelQuery<PK, M> andWhere(SExpression<Boolean> where) {
    query.andWhere(where);
    return this;
  }

  @Override
  public TypedDataModelQuery<PK, M> orWhere(QueryCondition... conditions) {
    query.orWhere(conditions);
    return this;
  }

  @Override
  public TypedDataModelQuery<PK, M> orWhere(Collection<?> where) {
    query.orWhere(where);
    return this;
  }

  @Override
  public TypedDataModelQuery<PK, M> orWhere(Map<String, ?> where) {
    query.orWhere(where);
    return this;
  }

  @Override
  public TypedDataModelQuery<PK, M> orWhere(SExpression<Boolean> where) {
    query.orWhere(where);
    return this;
  }

  @Override
  public TypedDataModelQuery<PK, M> populate(String populate) {
    query.populate(populate);
    return this;
  }

  @Override
  public TypedDataModelQuery<PK, M> populate(String... fields) {
    query.populate(fields);
    return this;
  }

  @Override
  public TypedDataModelQuery<PK, M> populate(Collection<?> populate) {
    query.populate(populate);
    return this;
  }

  @Override
  public TypedDataModelQuery<PK, M> populate(Map<String, ?> populate) {
    query.populate(populate);
    return this;
  }

  @Override
  public TypedDataModelQuery<PK, M> populate(PopulateSpec... populates) {
    query.populate(populates);
    return this;
  }

  @Override
  public TypedDataModelQuery<PK, M> clause(String clauseName, Object clause) {
    query.clause(clauseName, clause);
    return this;
  }

  @Override
  public TypedDataModelQuery<PK, M> clauses(Map<String, ?> clauses) {
    query.clauses(clauses);
    return this;
  }

  @Override
  public TypedDataModelQuery<PK, M> withPlugins(PluginDescriptor... plugins) {
    typedDataModel = typedDataModel.withPlugins(plugins);
    return this;
  }

  @Override
  public TypedDataModelQuery<PK, M> withoutPlugins(String... pluginNames) {
    typedDataModel = typedDataModel.withoutPlugins(pluginNames);
    return this;
  }

  @Override
  public Map<String, Object> build() {
    return query.build();
  }

  @Override
  public Try<List<M>> execute() {
    return typedDataModel.query(build());
  }
}
