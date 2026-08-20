package com.ouroboros.data.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import io.vavr.control.Try;

import com.ouroboros.data.model.query.TypedDataModelQueries;
import com.ouroboros.data.model.query.TypedDataModelQuery;

/**
 * @author liansz
 */
public interface TypedDataModel<PK, M> {

  DataModel getDataModel();

  Try<M> insert(M data);

  Try<List<M>> batchInsert(Collection<M> dataList);

  Try<Long> delete(PK id);

  Try<Long> delete(Collection<PK> ids);

  Try<Long> delete(Map<String, Object> where);

  Try<Long> update(PK id, M data);

  Try<Long> update(PK id, Map<String, Object> data);

  Try<Long> update(Collection<PK> ids, Map<String, Object> data);

  Try<Long> update(Map<String, Object> where, Map<String, Object> data);

  Try<Long> count(Map<String, Object> where);

  Try<M> get(PK id);

  Try<M> get(PK id, Map<String, Object> statement);

  Try<List<M>> query(Collection<PK> ids);

  Try<List<M>> query(Map<String, Object> statement);

  default TypedDataModelQuery<PK, M> query() {
    return TypedDataModelQueries.from(this);
  }

  Try<List<M>> query(Collection<String> select, Map<String, Object> where);

  Try<List<M>> query(Collection<String> select, Map<String, Object> where, String orderBy);

  Try<List<M>> query(Collection<String> select, Map<String, Object> where, String orderBy, Integer offset, Integer limit);

  default TypedDataModel<PK, M> withPlugins(PluginDescriptor... plugins) {
    return withPlugins(Arrays.asList(plugins));
  }

  TypedDataModel<PK, M> withPlugins(List<PluginDescriptor> pluginDescriptors);

  default TypedDataModel<PK, M> withoutPlugins(String... plugins) {
    return withoutPlugins(Arrays.asList(plugins));
  }

  TypedDataModel<PK, M> withoutPlugins(List<String> pluginNames);

  TypedDataModel<PK, M> withoutPlugins();

  boolean hasPlugin(String name);
}
