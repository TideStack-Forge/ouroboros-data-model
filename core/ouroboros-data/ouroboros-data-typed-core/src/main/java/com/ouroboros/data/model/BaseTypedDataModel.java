package com.ouroboros.data.model;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.record.Record;
import com.ouroboros.data.record.RecordList;
import com.ouroboros.data.util.DataMaps;

/**
 * @author liansz
 */
public class BaseTypedDataModel<PK, M> implements TypedDataModel<PK, M> {

  private final DataModel dataModel;
  private final Class<M> modelClass;

  public BaseTypedDataModel(DataModel dataModel, Class<M> modelClass) {
    this.dataModel = dataModel;
    this.modelClass = modelClass;
  }

  @Override
  public DataModel getDataModel() {
    return dataModel;
  }

  @Override
  public Try<M> insert(M data) {
    return recordToModel(dataModel.insert(DataMaps.fromBean(data)));
  }

  @Override
  public Try<List<M>> batchInsert(Collection<M> dataList) {
    return recodeListToModelList(dataModel.batchInsert(modelListToMapList(new ArrayList<>(dataList))));
  }

  @Override
  public Try<Long> delete(PK id) {
    return dataModel.delete(id);
  }

  @Override
  public Try<Long> delete(Collection<PK> ids) {
    return dataModel.delete(new ArrayList<>(ids));
  }

  @Override
  public Try<Long> delete(Map<String, Object> where) {
    return dataModel.delete(where);
  }

  @Override
  public Try<Long> update(PK id, M data) {
    var finalData = DataMaps.fromBean(data)
        .entrySet()
        .stream()
        .filter(entry -> entry.getValue() != null)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v2));
    return dataModel.update(id, finalData);
  }

  @Override
  public Try<Long> update(PK id, Map<String, Object> data) {
    return dataModel.update(id, data);
  }

  @Override
  public Try<Long> update(Collection<PK> ids, Map<String, Object> data) {
    return dataModel.update(new ArrayList<>(ids), data);
  }

  @Override
  public Try<Long> update(Map<String, Object> where, Map<String, Object> data) {
    return dataModel.update(where, data);
  }

  @Override
  public Try<Long> count(Map<String, Object> where) {
    return dataModel.count(where);
  }


  @Override
  public Try<M> get(PK id) {
    return recordToModel(dataModel.get(id));
  }

  @Override
  public Try<M> get(PK id, Map<String, Object> statement) {
    return recordToModel(dataModel.get(id, statement));
  }

  @Override
  public Try<List<M>> query(Collection<PK> ids) {
    return recodeListToModelList(dataModel.query(new ArrayList<>(ids)));
  }

  @Override
  public Try<List<M>> query(Map<String, Object> statement) {
    return recodeListToModelList(dataModel.query(statement));
  }

  @Override
  public Try<List<M>> query(Collection<String> select, Map<String, Object> where) {
    return recodeListToModelList(dataModel.query(new ArrayList<>(select), where));
  }

  @Override
  public Try<List<M>> query(Collection<String> select, Map<String, Object> where, String orderBy) {
    var rawQuery = new HashMap<String, Object>();
    rawQuery.put(Keyword.SELECT.toString(), select);
    rawQuery.put(Keyword.WHERE.toString(), where);
    rawQuery.put(Keyword.ORDER.toString(), orderBy);
    return query(rawQuery);
  }

  @Override
  public Try<List<M>> query(Collection<String> select, Map<String, Object> where, String orderBy, Integer offset, Integer limit) {
    var rawQuery = new HashMap<String, Object>();
    rawQuery.put(Keyword.SELECT.toString(), select);
    rawQuery.put(Keyword.WHERE.toString(), where);
    rawQuery.put(Keyword.ORDER.toString(), orderBy);
    rawQuery.put(Keyword.OFFSET.toString(), offset);
    rawQuery.put(Keyword.LIMIT.toString(), limit);
    return query(rawQuery);
  }

  @Override
  public TypedDataModel<PK, M> withPlugins(List<PluginDescriptor> pluginDescriptors) {
    return new BaseTypedDataModel<>(dataModel.withPlugins(pluginDescriptors), modelClass);
  }

  @Override
  public TypedDataModel<PK, M> withoutPlugins() {
    return new BaseTypedDataModel<>(dataModel.withoutPlugins(), modelClass);
  }

  @Override
  public TypedDataModel<PK, M> withoutPlugins(List<String> pluginNames) {
    return new BaseTypedDataModel<>(dataModel.withoutPlugins(pluginNames), modelClass);
  }

  @Override
  public boolean hasPlugin(String name) {
    return dataModel.hasPlugin(name);
  }

  /**
   * modelList转mapList
   *
   * @param dataList
   * @return
   */
  private List<Map<String, Object>> modelListToMapList(List<M> dataList) {
    return dataList.stream().map(DataMaps::fromBean).collect(Collectors.toList());
  }

  /**
   * record转model
   *
   * @param either
   * @return
   */
  private Try<M> recordToModel(Try<Record> either) {
    return either.map(this::recordToModel);
  }

  /**
   * record转model
   *
   * @param either
   * @return
   */
  private Try<List<M>> recodeListToModelList(Try<RecordList> either) {
    return either.map(this::recordToModel);
  }

  /**
   * record转model
   *
   * @param record
   * @return
   */
  private M recordToModel(Record record) {
    return ObjectUtils.isNotEmpty(record) ? record.toBean(getModelClass()) : null;
  }

  /**
   * record转model
   *
   * @param recordList
   * @return
   */
  private List<M> recordToModel(RecordList recordList) {
    return ObjectUtils.isNotEmpty(recordList) ? recordList.toBeanList(getModelClass()) : Collections.emptyList();
  }

  /**
   * 获取模型类型
   *
   * @return
   */
  private Class<M> getModelClass() {
    return modelClass;
  }
}
