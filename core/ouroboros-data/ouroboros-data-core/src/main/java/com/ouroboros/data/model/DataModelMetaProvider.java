package com.ouroboros.data.model;

import java.util.Map;
import java.util.Optional;

/**
 * 模型元数据工厂
 *
 * @author Song Mingxu
 */
public interface DataModelMetaProvider {
  Optional<DataModelMeta> getDataModelMeta(String name);

  Map<String, DataModelMeta> getDataModelMetaMap();

}