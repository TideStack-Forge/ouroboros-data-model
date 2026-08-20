package com.ouroboros.data.model;
import static io.vavr.API.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.model.valuetypes.RelatedValue;
import com.ouroboros.data.query.DefaultProjectionFieldSupport;
import com.ouroboros.data.station.DataStation;
import com.ouroboros.data.util.DataMaps;

public class PopulateContext {
  private final DataModel dataModel;
  private final Map<String, Object> attributes = new HashMap<>();
  private final PopulateConfig populateConfig = new PopulateConfig();
  private final String mainModelAlias;
  private DataModelField populateField;

  public PopulateContext(DataModel dataModel, String mainModelAlias, String populateName) {
    this.dataModel = dataModel;
    this.mainModelAlias = mainModelAlias;
    var field = dataModel.getField(populateName);
    if (!field.isPresent()) {
      return;
    }
    var fieldMeta = field.get();
    if (fieldMeta.getValueType() instanceof RelatedValue<?> relatedValue && relatedValue.getReferenceModel().isPresent()) {
      this.populateField = fieldMeta;
      var referenceModel = relatedValue.getReferenceModel().get();
      var select = referenceModel.getFields().stream()
          .filter(DefaultProjectionFieldSupport::isDirectDefaultProjectionField)
          .collect(Collectors.toList());
      populateConfig.setSelect(select);
    }
  }

  public PopulateContext(DataModel dataModel, String mainModelAlias, String populateName, Object populateConfig) {
    this(dataModel, mainModelAlias, populateName);
    if (populateConfig instanceof Map<?, ?> populateMap) {
      applyMap(DataMaps.remap(populateMap, Object::toString, Object.class::cast));
      return;
    }
    if (populateConfig instanceof String || populateConfig instanceof Collection<?>) {
      applyMap(Collections.singletonMap(Keyword.SELECT.toString(), populateConfig));
    }
  }

  private void applyMap(Map<String, Object> map) {
    var populateMap = DataMaps.remap(map, (Function<String, String>) String::toUpperCase);
    var select = getFieldNames(populateMap.get(Keyword.SELECT.toString()), Collectors.toSet(), String::toUpperCase);
    var omit = getFieldNames(populateMap.get(Keyword.OMIT.toString()), Collectors.toSet(), String::toUpperCase);
    var where = populateMap.get(Keyword.WHERE.toString());
    var limit = populateMap.get(Keyword.LIMIT.toString());
    var offset = populateMap.get(Keyword.OFFSET.toString());
    var populate = populateMap.get(Keyword.POPULATE.toString());

    var filterSelect = populateConfig.getSelect().stream()
        .filter(field -> {
          var fieldName = field.getName().toUpperCase();
          return select.isEmpty() ? !omit.contains(fieldName) : select.contains(fieldName);
        })
        .collect(Collectors.toList());

    populateConfig.setSelect(filterSelect);

    if (where instanceof Map<?, ?> whereMap) {
      populateConfig.where.putAll(DataMaps.remap(whereMap, Object::toString, Object.class::cast));
    }
    if (limit instanceof Integer limitInteger) {
      populateConfig.setLimit(limitInteger);
    }
    if (offset instanceof Integer offsetInteger) {
      populateConfig.setOffset(offsetInteger);
    }
    populateConfig.setPopulate(populate);
  }

  public DataStation<?> getDataStation() {
    return dataModel.getDataStation();
  }

  public Object getAttribute(String key) {
    return attributes.get(key);
  }

  public void setAttribute(String key, Object value) {
    attributes.put(key, value);
  }

  public void clearAttributes() {
    attributes.clear();
  }

  public PopulateConfig getPopulateConfig() {
    return populateConfig;
  }

  public DataModelField getPopulateField() {
    return populateField;
  }

  public String getMainModelAlias() {
    return mainModelAlias;
  }

  /**
   * 获取主数据模型
   *
   * @return 主数据模型
   */
  public DataModel getDataModel() {
    return dataModel;
  }

  /**
   * 获取目标模型（关联模型）
   *
   * @return 目标模型，如果 populateField 不是关联字段则返回 null
   */
  public DataModel getTargetModel() {
    if (populateField == null) {
      return null;
    }
    if (populateField.getValueType() instanceof RelatedValue<?> relatedValue) {
      return relatedValue.getReferenceModel().orElse(null);
    }
    return null;
  }

  /**
   * 获取关联键字段（目标模型中的外键字段）
   *
   * @return 关联键字段，如果不存在则返回 null
   */
  public DataModelField getReferenceKeyField() {
    if (populateField == null) {
      return null;
    }
    if (populateField.getValueType() instanceof RelatedValue<?> relatedValue) {
      return relatedValue.getReferenceKey().orElse(null);
    }
    return null;
  }

  @SafeVarargs
  private <CT> CT getFieldNames(Object fields, Collector<String, ?, CT> collector, Function<String, String>... mappers) {
    var fieldStream = Match(fields).of(
        Case($(CharSequence.class::isInstance), str -> Arrays.stream(String.valueOf(str).split(","))),
        Case($(Collection.class::isInstance), (Collection<?> coll) -> coll.stream()),
        Case($(), Stream::empty)
    );
    return fieldStream
        .map(value -> applyMappers(value.toString().trim(), mappers))
        .collect(collector);
  }

  private String applyMappers(String value, Function<String, String>[] mappers) {
    var result = value;
    if (mappers != null) {
      for (var mapper : mappers) {
        result = mapper.apply(result);
      }
    }
    return result;
  }

  static public class PopulateConfig {
    List<DataModelField> select;
    Map<String, Object> where = new HashMap<>();
    Integer limit;
    Integer offset;
    Object populate;

    public List<DataModelField> getSelect() {
      return select;
    }

    public void setSelect(List<DataModelField> select) {
      this.select = select;
    }

    public Map<String, Object> getWhere() {
      return where;
    }

    public void setWhere(Map<String, Object> where) {
      this.where = where;
    }

    public Integer getLimit() {
      return limit;
    }

    public void setLimit(Integer limit) {
      this.limit = limit;
    }

    public Integer getOffset() {
      return offset;
    }

    public void setOffset(Integer offset) {
      this.offset = offset;
    }

    public Object getPopulate() {
      return populate;
    }

    public void setPopulate(Object populate) {
      this.populate = populate;
    }
  }
}
