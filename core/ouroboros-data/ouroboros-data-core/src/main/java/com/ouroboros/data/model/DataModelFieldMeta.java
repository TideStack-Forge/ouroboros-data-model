package com.ouroboros.data.model;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.ouroboros.data.exception.FieldMetadataException;
import com.ouroboros.data.util.DataMaps;

/**
 * 字段元数据
 *
 * @author Song Mingxu
 */
@SuppressWarnings("unused")
public class DataModelFieldMeta implements Cloneable {
  private String name = "";
  private String label = "";
  private String description;
  private String rawName;
  private String type;
  private String rawType;
  private Map<String, Object> extraProps = new HashMap<>();
  private Boolean isNullable = true;
  private Boolean isUnsigned = false;
  private Boolean isAutoIncrement = false;
  private Boolean isUnique = false;
  private UniquenessScope uniquenessScope;
  private List<String> rules = new ArrayList<>();
  private String defaultValue;
  private Integer decimalDigits;
  private Integer size;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getRawName() {
    return rawName;
  }

  public void setRawName(String rawName) {
    this.rawName = rawName;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getRawType() {
    return rawType;
  }

  public void setRawType(String rawType) {
    this.rawType = rawType;
  }

  public Integer getDecimalDigits() {
    return decimalDigits;
  }

  public void setDecimalDigits(Integer decimalDigits) {
    this.decimalDigits = decimalDigits;
  }

  public Integer getSize() {
    return size;
  }

  public void setSize(Integer size) {
    this.size = size;
  }

  public Boolean getIsNullable() {
    return this.isNullable;
  }

  public void setIsNullable(Boolean isNullable) {
    this.isNullable = isNullable;
  }

  public Boolean getIsUnsigned() {
    return isUnsigned;
  }

  public void setIsUnsigned(Boolean isUnsigned) {
    this.isUnsigned = isUnsigned;
  }

  public Boolean getIsAutoIncrement() {
    return isAutoIncrement;
  }

  public void setIsAutoIncrement(Boolean isAutoIncrement) {
    this.isAutoIncrement = isAutoIncrement;
  }

  public Boolean getIsUnique() {
    return Optional.ofNullable(isUnique).orElse(false);
  }

  public void setIsUnique(Boolean isUnique) {
    this.isUnique = isUnique;
  }

  public UniquenessScope getUniquenessScope() {
    return uniquenessScope;
  }

  public void setUniquenessScope(UniquenessScope uniquenessScope) {
    this.uniquenessScope = uniquenessScope;
  }

  @JsonAnyGetter
  public Map<String, Object> getExtraProps() {
    return extraProps;
  }

  public void setExtraProps(Map<String, Object> extraProps) {
    this.extraProps = extraProps;
  }

  @JsonAnySetter
  public void setExtraProp(String key, Object value) {
    this.extraProps.put(key, value);
  }

  public List<String> getRules() {
    return rules;
  }

  public void setRules(List<String> rules) {
    this.rules = rules;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public Optional<Object> getExtraProp(String propName) {
    return Optional.ofNullable(getExtraProps())
        .map(m -> m.get(propName));
  }

  public <T> Optional<T> getExtraProp(Class<T> clazz, String propName) {
    return getExtraProp(propName)
        .map(prop -> clazz.isInstance(prop) ? clazz.cast(prop) : null);
  }

  public DataModelFieldMeta deepCopy() {
    try {
      DataModelFieldMeta fieldMeta = (DataModelFieldMeta) super.clone();
      fieldMeta.setExtraProps(DataMaps.deepClone(this.getExtraProps()));
      return fieldMeta;
    } catch (CloneNotSupportedException e) {
      throw new FieldMetadataException("Clone failed", getName(), null, e);
    }
  }

  @Override
  protected Object clone() throws CloneNotSupportedException {
    return deepCopy();
  }
}
