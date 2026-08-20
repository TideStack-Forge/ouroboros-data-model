package com.ouroboros.data.model;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.ouroboros.data.exception.MetadataException;
import com.ouroboros.data.util.DataMaps;

/**
 * 模型级唯一约束元数据。
 */
public class DataModelUniqueConstraintMeta implements Cloneable {
  private String name;
  private List<String> fields = new ArrayList<>();
  private UniquenessScope scope;
  private Map<String, Object> extraProps = new LinkedHashMap<>();

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<String> getFields() {
    return fields;
  }

  public void setFields(List<String> fields) {
    if (fields == null) {
      this.fields = Collections.emptyList();
      return;
    }
    this.fields = fields;
  }

  public UniquenessScope getScope() {
    return scope;
  }

  public void setScope(UniquenessScope scope) {
    this.scope = scope;
  }

  @JsonAnyGetter
  public Map<String, Object> getExtraProps() {
    return extraProps;
  }

  public void setExtraProps(Map<String, Object> extraProps) {
    if (extraProps == null) {
      this.extraProps = new LinkedHashMap<>();
      return;
    }
    this.extraProps = extraProps;
  }

  @JsonAnySetter
  public void setExtraProp(String key, Object value) {
    extraProps.put(key, value);
  }

  public Optional<Object> getExtraProp(String propName) {
    return Optional.ofNullable(getExtraProps())
        .map(m -> m.get(propName));
  }

  public <T> Optional<T> getExtraProp(Class<T> clazz, String propName) {
    return getExtraProp(propName)
        .map(prop -> clazz.isInstance(prop) ? clazz.cast(prop) : null);
  }

  public DataModelUniqueConstraintMeta deepCopy() {
    try {
      DataModelUniqueConstraintMeta meta = (DataModelUniqueConstraintMeta) super.clone();
      meta.fields = new ArrayList<>(getFields());
      meta.extraProps = DataMaps.deepClone(getExtraProps());
      return meta;
    } catch (CloneNotSupportedException e) {
      throw new MetadataException("Clone failed", e);
    }
  }

  @Override
  protected Object clone() throws CloneNotSupportedException {
    return deepCopy();
  }
}
