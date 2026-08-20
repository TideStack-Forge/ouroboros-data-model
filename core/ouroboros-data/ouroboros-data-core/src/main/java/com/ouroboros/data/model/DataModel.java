package com.ouroboros.data.model;

import java.util.*;

import io.vavr.control.Try;

import com.ouroboros.data.adapter.DataAdapter;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.query.DataModelQueries;
import com.ouroboros.data.dsl.query.DataModelQuery;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.record.Record;
import com.ouroboros.data.record.RecordList;
import com.ouroboros.data.station.DataStation;

/**
 * 实现对模型数据访问方法的封装
 *
 * @author Song Mingxu
 */
@SuppressWarnings("unused")
public interface DataModel {

  String getFormatVersion();

  String getSource();

  String getNamespace();

  String getName();

  String getFullName();

  String getLabel();

  String getDescription();

  String getRawName();

  MigrationStrategy getMigrationStrategy();

  Map<String, Object> getExtraProps();

  Optional<Object> getExtraProp(String name);

  default <T> Optional<T> getExtraProp(Class<T> type, String name) {
    return getExtraProp(name).map(value -> {
      if (type.isInstance(value)) {
        return type.cast(value);
      } else {
        return null;
      }
    });
  }

  List<DataModelField> getFields();

  Optional<DataModelField> getField(String name);

  List<DataModelField> getPrimaryKeys();

  default List<DataModelUniqueConstraintMeta> getUniqueConstraints() {
    return Collections.emptyList();
  }

  PrimaryKeyGenerator<?> getPrimaryKeyGenerator();

  DataAdapter getAdapter();

  DataStation<?> getDataStation();

  Try<Record> insert(Map<String, Object> data);

  Try<Record> insertOrUpdate(Map<String, Object> data);

  Try<RecordList> batchInsert(List<Map<String, Object>> dataList);

  Try<RecordList> batchInsertOrUpdate(List<Map<String, Object>> dataList);

  Try<Long> update(Object id, Map<String, Object> data);

  Try<Long> update(List<?> ids, Map<String, Object> data);

  Try<Long> update(Map<String, Object> where, Map<String, Object> data);

  default Try<Long> update(SExpression<Boolean> where, Map<String, Object> data) {
    return unsupportedNormalizedOperation("update(SExpression, Map)");
  }

  Try<Long> delete(Object id);

  Try<Long> delete(List<?> ids);

  Try<Long> delete(Map<String, Object> where);

  default Try<Long> delete(SExpression<Boolean> where) {
    return unsupportedNormalizedOperation("delete(SExpression)");
  }

  Try<Long> count(Map<String, Object> where);

  default Try<Long> count(QueryStatement statement) {
    return unsupportedNormalizedOperation("count(QueryStatement)");
  }

  Try<Record> get(Object id);

  Try<Record> get(Object id, Map<String, Object> statement);

  Try<RecordList> query(List<?> ids);

  Try<RecordList> query(QueryStatement statement);

  Try<RecordList> query(Map<String, Object> statement);

  default DataModelQuery query() {
    return DataModelQueries.from(this);
  }

  Try<RecordList> query(List<String> select, Map<String, Object> where);

  Try<RecordList> query(List<String> select, Map<String, Object> where, Map<String, Object> orderBy);

  Try<RecordList> query(List<String> select, Map<String, Object> where, Map<String, Object> orderBy,
                        Integer offset, Integer limit);

  default DataModel withPlugins(PluginDescriptor... plugins) {
    return withPlugins(Arrays.asList(plugins));
  }

  DataModel withPlugins(Collection<PluginDescriptor> pluginDescriptors);

  default DataModel withoutPlugins(String... plugins) {
    return withoutPlugins(Arrays.asList(plugins));
  }

  DataModel withoutPlugins(Collection<String> pluginNames);

  DataModel withoutPlugins();

  boolean hasPlugin(String name);

  private static <T> Try<T> unsupportedNormalizedOperation(String operation) {
    return Try.failure(new UnsupportedOperationException(
        operation + " is not supported by this data model"));
  }
}
