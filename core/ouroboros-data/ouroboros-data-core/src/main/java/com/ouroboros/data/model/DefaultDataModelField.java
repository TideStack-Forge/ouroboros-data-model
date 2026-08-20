package com.ouroboros.data.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ouroboros.data.validation.Rule;
import com.ouroboros.data.validation.RuleFactory;
import com.ouroboros.data.validation.rules.NotEmpty;
import com.ouroboros.data.validation.rules.NotNull;
import com.ouroboros.data.expression.DataExpressionContext;
import com.ouroboros.data.expression.DataExpressionEvaluator;
import com.ouroboros.data.util.DataStrings;

public class DefaultDataModelField implements DataModelField {
  final static Logger logger = LoggerFactory.getLogger(DefaultDataModelField.class);
  private final DataModel dataModel;
  private final DataModelFieldMeta fieldMeta;
  final private List<Rule> rules;
  final private ValueType<?> valueType;

  public DefaultDataModelField(DataModel dataModel, DataModelFieldMeta fieldMeta) {
    this.dataModel = dataModel;
    this.fieldMeta = fieldMeta;
    this.rules = fieldMeta.getRules().stream()
        .map(RuleFactory::getRule)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
    if (!this.getIsNullable() && this.rules.stream().noneMatch(r -> r instanceof NotNull || r instanceof NotEmpty)) {
      this.rules.add(new NotNull());
    }
    this.valueType = ValueTypeFactory
        .getValueType(this)
        .orElse(new DefaultValueTypeFactory.UnknownValueType());
  }

  @Override
  public String getName() {
    return fieldMeta.getName();
  }

  @Override
  public String getLabel() {
    return fieldMeta.getLabel();
  }

  @Override
  public String getDescription() {
    return fieldMeta.getDescription();
  }

  @Override
  public String getRawName() {
    return StringUtils.isEmpty(fieldMeta.getRawName())
        ? DataStrings.toSnakeCase(getName())
        : fieldMeta.getRawName();
  }

  @Override
  public String getType() {
    return fieldMeta.getType();
  }

  @Override
  public String getRawType() {
    return fieldMeta.getRawType();
  }

  @Override
  public ValueType<?> getValueType() {
    return valueType;
  }

  @Override
  public Object getDefaultValue(Map<String, Object> context) {
    return Optional.ofNullable(getDefaultValue())
        .filter(StringUtils::isNotBlank)
        .flatMap(expr -> {
          var wrappedContext = DataExpressionContext.wrap(context, Collections.singletonMap("$record", Collections.unmodifiableMap(context)));
          return DataExpressionEvaluator.eval(DataExpressionEvaluator.wrap(expr), wrappedContext)
              .peekLeft(e -> logger.warn("计算模型{}字段{}默认值的时候发生错误{}", getDataModel().getFullName(), getName(), e.getMessage(), e))
              .toJavaOptional();
        })
        .map(o -> getValueType().convert(o))
        .orElse(null);
  }

  @Override
  public Integer getDecimalDigits() {
    return fieldMeta.getDecimalDigits();
  }

  @Override
  public Integer getSize() {
    return fieldMeta.getSize();
  }

  @Override
  public Boolean getIsNullable() {
    return ObjectUtils.isNotEmpty(fieldMeta.getIsNullable()) ? fieldMeta.getIsNullable() : true;
  }

  @Override
  public Boolean getIsUnsigned() {
    return ObjectUtils.isNotEmpty(fieldMeta.getIsUnsigned()) ? fieldMeta.getIsUnsigned() : false;
  }

  @Override
  public Boolean getIsAutoIncrement() {
    return ObjectUtils.isNotEmpty(fieldMeta.getIsAutoIncrement()) ? fieldMeta.getIsAutoIncrement() : false;
  }

  @Override
  public Boolean getIsUnique() {
    return fieldMeta.getIsUnique();
  }

  @Override
  public UniquenessScope getUniquenessScope() {
    return fieldMeta.getUniquenessScope();
  }

  @Override
  public Map<String, Object> getExtraProps() {
    return fieldMeta.getExtraProps();
  }

  @Override
  public List<Rule> getRules() {
    return rules;
  }

  public String getDefaultValue() {
    return StringUtils.isBlank(fieldMeta.getDefaultValue()) ? null : DataExpressionEvaluator.wrap(fieldMeta.getDefaultValue());
  }

  @Override
  public Optional<Object> getExtraProp(String propName) {
    return fieldMeta.getExtraProp(propName);
  }

  @Override
  public <T> Optional<T> getExtraProp(Class<T> clazz, String propName) {
    return fieldMeta.getExtraProp(clazz, propName);
  }

  @Override
  public DataModel getDataModel() {
    return dataModel;
  }
}
