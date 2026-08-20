package com.ouroboros.data.model;

import java.util.Map;
import java.util.Optional;

/**
 * @author liansz
 **/
public interface DataModelPluginBuilder {

  boolean support(String name);

  Optional<DataModelPlugin> build(DataModel dataModel, Map<String, Object> config);
}
