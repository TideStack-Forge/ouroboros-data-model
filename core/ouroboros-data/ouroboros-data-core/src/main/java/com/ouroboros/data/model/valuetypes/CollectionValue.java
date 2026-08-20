package com.ouroboros.data.model.valuetypes;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.PopulateContext;
import com.ouroboros.data.model.QueryPatcher;
import com.ouroboros.data.model.ValueTypeBuilder;
import com.ouroboros.data.record.RecordList;
import com.ouroboros.data.util.DataLists;
import com.ouroboros.data.util.DataMaps;

public class CollectionValue extends RelatedValue<Object> implements ValueTypeBuilder<Object, CollectionValue> {
  final static Logger logger = LoggerFactory.getLogger(CollectionValue.class);

  @Override
  public String getName() {
    return "Collection";
  }

  @Override
  public String getLabel() {
    return "集合";
  }

  @Override
  public Class<Object> getType() {
    return Object.class;
  }

  @Override
  public Boolean canPopulate() {
    return true;
  }

  @Override
  public Boolean isPhysical() {
    return false;
  }

  @Override
  public Object convert(Object value) {
    return value;
  }

  @Override
  public Optional<DataModelField> getReferenceKey() {
    // 先检查是否有明确指定的外键
    var referenceKeyConfig = dataModelField.getExtraProp(CharSequence.class, "referenceKey");
    if (!referenceKeyConfig.isPresent()) {
      referenceKeyConfig = dataModelField.getExtraProp(CharSequence.class, "via");
    }
    if (referenceKeyConfig.isPresent()) {
      return referenceKeyConfig.flatMap(k -> getReferenceModel()
          .flatMap(m -> m.getField(k.toString())));
    }

    // 如果没有指定外键，则在关联模型中查找与当前模型名相关的字段
    return getReferenceModel()
        .flatMap(m -> m.getFields()
            .stream()
            .filter(f -> f.getName().toUpperCase().startsWith(dataModelField.getDataModel().getName().toUpperCase()))
            .findFirst());
  }

  @Override
  public Optional<DataModelField> getKey() {
    // 先检查是否有明确指定的外键
    var fkConfig = dataModelField.getExtraProp(CharSequence.class, "key");

    if (fkConfig.isPresent()) {
      return fkConfig.flatMap(fk -> dataModelField.getDataModel().getField(fk.toString()));
    }

    // 没有的话取本模型的主键
    var fk = DataLists.getValue(dataModelField.getDataModel().getPrimaryKeys(), 0);
    if (fk != null) {
      return Optional.of(fk);
    }

    // 若没有设置主键，则尝试取id字段
    return dataModelField.getDataModel().getField("id");
  }

  /**
   * @deprecated 该方法已废弃，populate 逻辑已迁移到 Orchestration 层。
   * 此方法仅作为兼容层保留，未来版本将移除。
   */
  @Deprecated
  @Override
  public QueryPatcher getQueryPatch(PopulateContext context) {
    var populateField = this.dataModelField;
    var referenceModel = getReferenceModel().orElse(null);
    var keyField = getKey().orElse(null);
    var referenceKeyField = getReferenceKey().orElse(null);
    var select = context.getPopulateConfig().getSelect();
    var populate = context.getPopulateConfig().getPopulate();
    var where = context.getPopulateConfig().getWhere();
    var mainModelAlias = context.getMainModelAlias();

    if (referenceModel == null || referenceKeyField == null || keyField == null) {
      return new QueryPatcher();
    }

    var finalSelect = Stream.concat(select.stream().map(DataModelField::getName), Stream.of(referenceKeyField.getName()))
        .distinct()
        .collect(Collectors.toList());
    var populateRecords = new AtomicReference<>(RecordList.empty());
    Consumer<RecordList> watcher = data -> {
      // 准备查询
      var query = new TreeMap<>(String::compareToIgnoreCase);
      var keyValues = data.stream()
          .map(r -> r.get(keyField.getName()))
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
      var wherePatch = Collections.<String, Object>singletonMap(referenceKeyField.getName(), keyValues);
      query.put(Keyword.WHERE.toString(), DataMaps.merge(where, wherePatch));
      query.put(Keyword.SELECT.toString(), finalSelect);
      query.put("POPULATE", populate);

      // 填充查询结果
      var children = keyValues.isEmpty() ? Try.success(RecordList.empty()) : referenceModel.query(query);
      children
          .peek(populateRecords::set)
          .onFailure(e -> logger.error("准备填充 {} 字段时发生错误", populateField.getName()));
    };
    Consumer<Map<String, Object>> rowPatcher = r -> {
      var keyValue = r.get(keyField.getName());
      var keyStringValue = keyValue == null ? "" : keyValue.toString();
      var related = populateRecords.get().stream()
          .filter(record -> record.get(referenceKeyField.getName()).toString().equals(keyStringValue))
          .collect(Collectors.toList());
      r.put(populateField.getName(), RecordList.of(related));
    };
    return new QueryPatcher(Collections.emptyList(),
        Collections.singletonMap(keyField.getName(), mainModelAlias + "." + keyField.getName()),
        rowPatcher,
        watcher);
  }

  @Override
  public CollectionValue build(DataModelField field) {
    var newValue = new CollectionValue();
    newValue.dataModelField = field;
    return newValue;
  }
}
