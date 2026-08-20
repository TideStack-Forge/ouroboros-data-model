package com.ouroboros.data.station;

import java.util.*;
import java.util.stream.Collectors;

import com.ouroboros.data.adapter.DataAdapter;
import com.ouroboros.data.adapter.DataAdapterBuilder;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelMeta;
import com.ouroboros.data.model.DefaultDataModel;
import com.ouroboros.data.model.EnhancedDataModelProxy;

@SuppressWarnings({"unused"})
public abstract class AbstractDataStation<T> implements DataStation<T> {
  private DataAdapter adapter;
  private Map<String, DataModel> dataModelMap = new LinkedHashMap<>();
  private List<DataModel> dataModels = new ArrayList<>();
  private List<DataModelMeta> dataModelMetas;
  private String name;

  public AbstractDataStation() {
  }

  @Override
  public DataAdapter getDataAdapter() {
    if (adapter == null) {
      adapter = DataAdapterBuilder.getDataAdapter(this).orElse(null);
    }
    return adapter;
  }

  protected void setDataModels(Collection<DataModel> dataModels) {
    this.dataModels = Collections.unmodifiableList(dataModels instanceof List
        ? Collections.unmodifiableList((List<? extends DataModel>) dataModels)
        : new ArrayList<>(dataModels));
    dataModelMap = dataModels.stream()
        .collect(() -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER), (map, model) -> map.put(model.getFullName(), model), Map::putAll);
  }

  protected List<? extends DataModelMeta> getModelMetas() {
    return dataModelMetas;
  }

  protected void setModelMetas(Collection<DataModelMeta> modelMetas) {
    this.dataModelMetas = Collections.unmodifiableList(modelMetas instanceof List
        ? (List<DataModelMeta>) modelMetas
        : new ArrayList<>(modelMetas));
    var dataModels = modelMetas.stream()
        .<DataModel>map(meta -> new EnhancedDataModelProxy(meta, (decoratedMeta) -> new DefaultDataModel(decoratedMeta, this)))
        .collect(Collectors.toList());
    setDataModels(dataModels);
  }

  @Override
  public Optional<DataModel> getDataModel(String modelName) {
    return Optional.ofNullable(dataModelMap.get(modelName.toUpperCase()));
  }

  @Override
  public List<DataModel> getDataModelList() {
    return dataModels;
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

}
