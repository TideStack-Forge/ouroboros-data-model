package com.ouroboros.data.model;

import java.util.List;
import java.util.Map;

import io.vavr.control.Try;

import com.ouroboros.data.adapter.DataAdapter;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.record.Record;
import com.ouroboros.data.record.RecordList;
import com.ouroboros.data.station.DataStation;

public class MetadataOnlyDataModel extends AbstractDataModel {

  public MetadataOnlyDataModel(DataModelMeta meta) {
    super(meta);
  }

  @Override
  public DataAdapter getAdapter() {
    return null;
  }

  @Override
  public DataStation<?> getDataStation() {
    return null;
  }

  @Override
  public Try<Record> insert(Map<String, Object> data) {
    return unsupported();
  }

  @Override
  public Try<RecordList> batchInsert(List<Map<String, Object>> dataList) {
    return unsupported();
  }

  @Override
  public Try<Long> update(Map<String, Object> where, Map<String, Object> data) {
    return unsupported();
  }

  @Override
  public Try<Long> delete(Map<String, Object> where) {
    return unsupported();
  }

  @Override
  public Try<Long> count(Map<String, Object> where) {
    return unsupported();
  }

  @Override
  public Try<RecordList> query(QueryStatement statement) {
    return unsupported();
  }

  @Override
  public Try<RecordList> query(Map<String, Object> statement) {
    return unsupported();
  }

  @Override
  public DataModel withPlugins(java.util.Collection<PluginDescriptor> pluginDescriptors) {
    return new EnhancedDataModelProxy(this, pluginDescriptors);
  }

  @Override
  public DataModel withoutPlugins(java.util.Collection<String> pluginNames) {
    return this;
  }

  @Override
  public DataModel withoutPlugins() {
    return this;
  }

  @Override
  public boolean hasPlugin(String name) {
    return false;
  }

  private <T> Try<T> unsupported() {
    return Try.failure(new UnsupportedOperationException("metadata-only data model does not support data operations"));
  }
}
