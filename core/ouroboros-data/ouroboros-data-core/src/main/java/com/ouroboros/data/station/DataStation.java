package com.ouroboros.data.station;

import java.util.List;
import java.util.Optional;

import com.ouroboros.data.adapter.DataAdapter;
import com.ouroboros.data.model.DataModel;

/**
 * 统一数据源
 *
 * @param <T> 数据源类型
 * @author Song Mingxu
 * @version 1.0.0
 */
@SuppressWarnings("unused")
public interface DataStation<T> {
  String getName();

  DataAdapter getDataAdapter();

  Optional<DataModel> getDataModel(String modelName);

  List<DataModel> getDataModelList();

  T getDataSource();
}