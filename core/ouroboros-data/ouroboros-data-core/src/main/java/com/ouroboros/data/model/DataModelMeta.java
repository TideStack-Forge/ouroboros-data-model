package com.ouroboros.data.model;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.ouroboros.data.exception.ModelMetadataException;
import com.ouroboros.data.util.DataMaps;

/**
 * 模型元数据
 *
 * @author Song Mingxu
 */
@SuppressWarnings("unused")
public class DataModelMeta implements Cloneable {
  private String formatVersion;
  private String source;
  private String dataStation;
  private String name;
  private String namespace;
  private String label;
  private String description;
  private String rawName;
  private MigrationStrategy migrationStrategy = MigrationStrategy.AUTO;
  private List<PluginDescriptor> pluginDescriptors = new ArrayList<>();
  private Map<String, Object> extraProps = new LinkedHashMap<>();
  private List<DataModelFieldMeta> fields = new ArrayList<>();
  private List<String> primaryKeys = new ArrayList<>();
  private List<DataModelUniqueConstraintMeta> uniqueConstraints = new ArrayList<>();
  private String primaryKeyGenerator = null;

  public String getFormatVersion() {
    return formatVersion;
  }

  public void setFormatVersion(String formatVersion) {
    this.formatVersion = formatVersion;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getFullName() {
    return StringUtils.isBlank(namespace) ? name : namespace + "." + name;
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

  public String getDataStation() {
    return dataStation;
  }

  public void setDataStation(String dataStation) {
    this.dataStation = dataStation;
  }

  public MigrationStrategy getMigrationStrategy() {
    return migrationStrategy;
  }

  public void setMigrationStrategy(MigrationStrategy migrationStrategy) {
    this.migrationStrategy = migrationStrategy;
  }

  public List<PluginDescriptor> getPluginDescriptors() {
    return pluginDescriptors;
  }

  public void setPluginDescriptors(List<PluginDescriptor> pluginDescriptors) {
    this.pluginDescriptors = pluginDescriptors;
  }

  public void addPluginDescriptor(PluginDescriptor pluginDescriptor) {
    if (pluginDescriptors == null) {
      pluginDescriptors = new ArrayList<>();
    }
    pluginDescriptors.add(pluginDescriptor);
  }

  public void removePluginDescriptor(String pluginName) {
    if (pluginDescriptors != null) {
      pluginDescriptors.removeIf(pluginDescriptor -> pluginName.equalsIgnoreCase(pluginDescriptor.getName()));
    }
  }

  @JsonAnyGetter
  public Map<String, Object> getExtraProps() {
    return extraProps;
  }

  public void setExtraProps(Map<String, Object> extraProps) {
    if (extraProps == null) {
      this.extraProps = new LinkedHashMap<>();
    } else {
      this.extraProps = extraProps;
    }
  }

  public List<DataModelFieldMeta> getFields() {
    return fields;
  }

  public void setFields(List<DataModelFieldMeta> fields) {
    if (fields == null) {
      this.fields = Collections.emptyList();
      return;
    }
    this.fields = fields;
  }

  public String getPrimaryKeyGenerator() {
    return primaryKeyGenerator;
  }

  public void setPrimaryKeyGenerator(String primaryKeyGenerator) {
    this.primaryKeyGenerator = primaryKeyGenerator;
  }

  public List<String> getPrimaryKeys() {
    return primaryKeys;
  }

  public void setPrimaryKeys(List<String> primaryKeys) {
    if (primaryKeys == null) {
      this.primaryKeys = Collections.emptyList();
      return;
    }
    this.primaryKeys = primaryKeys;
  }

  public List<DataModelUniqueConstraintMeta> getUniqueConstraints() {
    return uniqueConstraints;
  }

  public void setUniqueConstraints(List<DataModelUniqueConstraintMeta> uniqueConstraints) {
    if (uniqueConstraints == null) {
      this.uniqueConstraints = Collections.emptyList();
      return;
    }
    this.uniqueConstraints = uniqueConstraints;
  }

  public void addUniqueConstraint(DataModelUniqueConstraintMeta uniqueConstraint) {
    if (uniqueConstraints == null) {
      uniqueConstraints = new ArrayList<>();
    }
    uniqueConstraints.add(uniqueConstraint);
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

  public DataModelMeta deepCopy() {
    try {
      var meta = (DataModelMeta) super.clone();
      meta.extraProps = DataMaps.deepClone(getExtraProps());
      meta.primaryKeys = new ArrayList<>(getPrimaryKeys());
      meta.uniqueConstraints = getUniqueConstraints().stream()
          .map(DataModelUniqueConstraintMeta::deepCopy)
          .collect(Collectors.toList());
      meta.fields = getFields().stream()
          .map(DataModelFieldMeta::deepCopy)
          .collect(Collectors.toList());
      meta.pluginDescriptors = getPluginDescriptors().stream()
          .map(PluginDescriptor::deepCopy)
          .collect(Collectors.toList());
      return meta;
    } catch (CloneNotSupportedException e) {
      throw new ModelMetadataException("Clone failed", getFullName(), e);
    }
  }

  @Override
  protected Object clone() throws CloneNotSupportedException {
    return deepCopy();
  }
}
