package com.ouroboros.data.model.valuetypes;

import java.util.Optional;

import com.ouroboros.data.model.*;

public abstract class RelatedValue<T> implements ValueType<T> {
  protected DataModelField dataModelField;

  @Override
  public abstract Boolean isPhysical();

  /**
   * 获取关联的模型名称
   */
  public String getReferenceModelName() {
    if (dataModelField == null || dataModelField.getExtraProps() == null) {
      return null;
    }
    var modelName = dataModelField.getExtraProps().get("model");
    return modelName == null ? null : modelName.toString();
  }

  /**
   * 获取关联模型
   */
  public Optional<DataModel> getReferenceModel() {
    return DataModelCenter.getDataModel(getReferenceModelName());
  }

  /**
   * 获取关联模型的外键字段名称
   */
  public abstract Optional<DataModelField> getReferenceKey();

  public abstract Optional<DataModelField> getKey();

  public QueryPatcher getQueryPatch(PopulateContext context) {
    return new QueryPatcher();
  }
}
