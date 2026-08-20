package com.ouroboros.data.model.valuetypes;

import java.sql.Clob;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;

import com.ouroboros.data.dsl.JoinType;
import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.model.*;
import com.ouroboros.data.orchestration.JoinCapabilities;
import com.ouroboros.data.record.RecordList;
import com.ouroboros.data.util.DataMaps;

public class ModelValue extends RelatedValue<Object> implements ValueTypeBuilder<Object, ModelValue> {
  final static Logger logger = LoggerFactory.getLogger(ModelValue.class);

  @Override
  public String getName() {
    return "Model";
  }

  @Override
  public String getLabel() {
    return "模型";
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
  public Object convert(Object value) {
    // TODO: 此类型的希望不要一次性将数据全部加载到内存，后期考虑增加一个包裹类实现懒加载，临时处理Clob类型问题
    if (value instanceof Clob clob) {
      return Try.of(() -> clob.getSubString(1, (int) clob.length())).get();
    }

    return value;
  }

  @Override
  public Boolean isPhysical() {
    return dataModelField.getExtraProp(CharSequence.class, "key")
        // 如果有key且key不等于当前字段名，则认为是虚拟字段
        // 否则认为是物理字段
        .map(key -> key.toString().equalsIgnoreCase(dataModelField.getName()))
        .orElse(true);
  }

  @Override
  public Optional<DataModelField> getReferenceKey() {
    var referenceKeyConfig = dataModelField.getExtraProp(CharSequence.class, "referenceKey");
    if (!referenceKeyConfig.isPresent()) {
      referenceKeyConfig = dataModelField.getExtraProp(CharSequence.class, "via");
    }
    if (referenceKeyConfig.isPresent()) {
      return referenceKeyConfig.flatMap(k -> getReferenceModel().flatMap(m -> m.getField(k.toString())));
    }

    // 如果没有明确指定，则取第一个主键
    // OPTIMIZE: 暂不考虑复合主键的情况，有需要的时候再实现
    return getReferenceModel()
        .map(DataModel::getPrimaryKeys)
        .map(list -> list.size() == 0 ? null : list.get(0));
  }

  @Override
  public Optional<DataModelField> getKey() {
    var keyConfig = dataModelField.getExtraProp(CharSequence.class, "key");
    if (keyConfig.isPresent()) {
      return keyConfig.flatMap(k -> this.dataModelField.getDataModel().getField(k.toString()));
    }
    return Optional.ofNullable(this.dataModelField);
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
    var referenceKeyField = getReferenceKey().orElse(null);
    var keyField = getKey().orElse(null);
    var select = context.getPopulateConfig().getSelect();
    var mainModelAlias = context.getMainModelAlias();

    if (referenceModel == null || referenceKeyField == null || keyField == null) {
      return new QueryPatcher();
    }

    // 如果不能通过表连接查询，则不需要补充查询体，但要确保查询结果中包含所有需要的字段
    // 使用 JoinCapabilities 判断是否可以 JOIN
    var sourceModel = this.dataModelField.getDataModel();
    var joinResult = JoinCapabilities.canJoin(sourceModel, referenceModel);
    if (!joinResult.isJoinable()
        || !isEmpty(context.getPopulateConfig().getPopulate())) {
      var selectPatch = Collections.singletonMap(keyField.getName(), mainModelAlias + "." + keyField.getName());
      var populateRecords = new AtomicReference<RecordList>();
      Consumer<RecordList> resultWatcher = (records) -> {
        // 准备查询
        var query = new TreeMap<String, Object>(String::compareToIgnoreCase);
        var where = context.getPopulateConfig().getWhere();
        var populate = context.getPopulateConfig().getPopulate();
        var keyValues = records.stream()
            .map(r -> r.get(keyField.getName()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        var wherePatch = Collections.<String, Object>singletonMap(referenceKeyField.getName(), keyValues);
        query.put(Keyword.WHERE.toString(), DataMaps.merge(where, wherePatch));
        query.put(Keyword.SELECT.toString(), Stream.concat(select.stream().map(DataModelField::getName), Stream.of(referenceKeyField.getName()))
            .distinct()
            .collect(Collectors.toList()));
        query.put("POPULATE", populate);
        var children = keyValues.isEmpty() ? Try.success(RecordList.empty()) : referenceModel.query(query);
        children
            .onSuccess(populateRecords::set)
            .onFailure(e -> logger.error("准备填充 {} 字段时发生错误", populateField.getName()));
      };
      Consumer<Map<String, Object>> recordPatcher = (r) -> {
        var keyValue = r.get(keyField.getName());
        var keyStringValue = keyValue == null ? "" : keyValue.toString();
        var records = populateRecords.get();
        var related = records.stream()
            .filter(record -> record.get(referenceKeyField.getName()).toString().equals(keyStringValue))
            .collect(Collectors.toList());
        r.put(populateField.getName(), related.isEmpty() ? null : related.get(0));
      };
      return new QueryPatcher(Collections.emptyList(), selectPatch, recordPatcher, resultWatcher);
    }

    // join
    // TODO: 将来解决了数据库迁移问题之后再用INNER JOIN
    var joinType = keyField.getIsNullable()
        ? JoinType.LEFTJOIN
        : JoinType.LEFTJOIN;//JoinType.INNERJOIN;
    var joinTable = referenceModel.getFullName();
    var joinAlias = populateField.getName() + "__" + referenceModel.getFullName().replaceAll("\\.", "_");
    var fkField = buildFieldExpression(mainModelAlias, keyField.getName());
    var referenceField = buildFieldExpression(joinAlias, referenceKeyField.getName());
    var joinCondition = SExpression.<Boolean>create(Operators.EQ, fkField, referenceField);
    var joinEntry = new QueryStatement.JoinEntry(joinType, joinTable, joinAlias, joinCondition);

    // 增加填充字段
    var joinPopulateFields = select
        .stream()
        .map(f -> Tuple.of(joinAlias + "__" + f.getName(), f.getName()))
        .collect(Collectors.toList());

    var selectPatch = Stream.concat(
        joinPopulateFields.stream().map(t -> t.map2(f -> joinAlias + "." + f)),
        Stream.of(Tuple.of(keyField.getName(), mainModelAlias + "." + keyField.getName()))
    ).collect(Collectors.toMap(Tuple2::_1, Tuple2::_2, (v1, v2) -> v2));

    Consumer<Map<String, Object>> rowPatcher = (row) -> {
      var referenceValue = row.remove(keyField.getName());

      var populateData = new LinkedHashMap<String, Object>();
      joinPopulateFields.forEach(f -> populateData.put(f._2, row.remove(f._1)));
      if (referenceValue == null || populateData.entrySet().stream().allMatch(e -> e.getValue() == null)) {
        row.put(populateField.getName(), null);
      } else {
        row.put(populateField.getName(), populateData);
      }
    };
    return new QueryPatcher(joinEntry, selectPatch, rowPatcher);
  }

  private SExpression<?> buildFieldExpression(String path, String leaf) {
    String[] segments = path.split("\\.");
    Object[] params = new Object[segments.length + 1];
    System.arraycopy(segments, 0, params, 0, segments.length);
    params[segments.length] = leaf;
    return SExpression.create(Operators.FIELD, params);
  }

  private Boolean isEmpty(Object obj) {
    return obj == null || obj instanceof CharSequence str && str.toString().trim().isEmpty()
        || obj instanceof Map<?, ?> map && map.isEmpty()
        || obj instanceof List<?> list && list.isEmpty();
  }


  @Override
  public ModelValue build(DataModelField field) {
    var newValue = new ModelValue();
    newValue.dataModelField = field;
    return newValue;
  }

  @Override
  public Object toPersistentValue(Object value) {
    if (value instanceof Map<?, ?> map) {
      var refKey = getReferenceKey().map(DataModelField::getName).orElse(null);
      if (refKey == null) {
        logger.warn("Cannot get reference key of field {}", this.dataModelField.getName());
      }
      return map.get(refKey);
    }
    return value;
  }
}
