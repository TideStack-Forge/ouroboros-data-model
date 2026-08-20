package com.ouroboros.data.model;

public interface DataModelPluginContext {
  DataModel getDataModel();

  DataModel getCoreDataModel();

  DataModelPlugin getNextPlugin();

  DataModelPluginContext getNextPluginContext();
}
