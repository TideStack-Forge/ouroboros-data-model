package com.ouroboros.data.station;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

@SuppressWarnings("unused")
public class DataStationDefine {
  String name;
  String label;
  String description;
  String type;
  Map<String, Object> properties = new HashMap<String, Object>();

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

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @JsonAnyGetter
  public Map<String, Object> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, Object> properties) {
    if (properties == null) {
      this.properties = Collections.emptyMap();
      return;
    }
    this.properties = properties;
  }

  @JsonAnySetter
  public void setProperty(String name, Object value) {
    properties.put(name, value);
  }

  @JsonIgnore
  public Optional<Object> getProperty(String name) {
    return Optional.ofNullable(properties.get(name));
  }

  @JsonIgnore
  public <T> Optional<T> getProperty(Class<T> clazz, String name) {
    return getProperty(name)
        .map(v -> {
          if (clazz.isInstance(v)) {
            return clazz.cast(v);
          } else {
            return null;
          }
        });
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof DataStationDefine other) {
      return Objects.equals(getName(), other.getName())
          && Objects.equals(getLabel(), other.getLabel())
          && Objects.equals(getDescription(), other.getDescription())
          && Objects.equals(getType(), other.getType())
          && Objects.equals(getProperties(), other.getProperties());
    }
    return false;
  }
}
