package com.ouroboros.data.model;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.ouroboros.data.util.DataMaps;


/**
 * 不可变的数据模型元数据类，继承自DataModelMeta
 * 该类实现了不可变性设计模式，所有字段都是final类型
 * 提供了深拷贝功能，确保数据的安全性和一致性
 */
public class ImmutableDataModelMeta extends DataModelMeta {
  // ============= Immutable Stored Fields =============
  private final String formatVersion;
  private final String source;
  private final String namespace;
  private final String name;
  private final String label;
  private final String description;
  private final String rawName;
  private final MigrationStrategy migrationStrategy;
  private final String dataStation;

  private final List<DataModelFieldMeta> fields;
  private final List<String> primaryKeys;
  private final List<DataModelUniqueConstraintMeta> uniqueConstraints;
  private final String primaryKeyGenerator;

  private final List<PluginDescriptor> pluginDescriptors;
  private final Map<String, Object> extraProps;

  public ImmutableDataModelMeta(DataModelMeta original) {
    Objects.requireNonNull(original);

    this.formatVersion = original.getFormatVersion();
    this.source = original.getSource();
    this.namespace = original.getNamespace();
    this.name = original.getName();
    this.label = original.getLabel();
    this.description = original.getDescription();
    this.rawName = original.getRawName();
    this.migrationStrategy = original.getMigrationStrategy();
    this.dataStation = original.getDataStation();
    this.primaryKeyGenerator = original.getPrimaryKeyGenerator();

    List<DataModelFieldMeta> fs = original.getFields();
    this.fields = ObjectUtils.isEmpty(fs)
        ? Collections.emptyList()
        : Collections.unmodifiableList(
        fs.stream()
            .filter(Objects::nonNull)
            .map(ImmutableFieldMeta::new)
            .collect(Collectors.toList()));

    List<String> ks = original.getPrimaryKeys();
    this.primaryKeys = ObjectUtils.isEmpty(ks)
        ? Collections.emptyList()
        : Collections.unmodifiableList(new ArrayList<>(ks));

    List<DataModelUniqueConstraintMeta> ucs = original.getUniqueConstraints();
    this.uniqueConstraints = ObjectUtils.isEmpty(ucs)
        ? Collections.emptyList()
        : Collections.unmodifiableList(
        ucs.stream()
            .filter(Objects::nonNull)
            .map(ImmutableUniqueConstraintMeta::new)
            .collect(Collectors.toList()));

    List<PluginDescriptor> ps = original.getPluginDescriptors();
    this.pluginDescriptors = ObjectUtils.isEmpty(ps)
        ? Collections.emptyList()
        : Collections.unmodifiableList(
        ps.stream()
            .filter(Objects::nonNull)
            .map(ImmutablePluginDescriptor::new)
            .collect(Collectors.toList()));

    Map<String, Object> ep = original.getExtraProps();
    this.extraProps = ObjectUtils.isEmpty(ep)
        ? Collections.emptyMap()
        : Collections.unmodifiableMap(DataMaps.deepClone(ep));
  }

  private static UnsupportedOperationException err() {
    return new UnsupportedOperationException("ImmutableDataModelMeta is read-only");
  }

  // ============= Getters =============
  @Override
  public String getFormatVersion() {
    return formatVersion;
  }

  @Override
  public void setFormatVersion(String v) {
    throw err();
  }

  @Override
  public String getSource() {
    return source;
  }

  @Override
  public void setSource(String s) {
    throw err();
  }

  @Override
  public String getNamespace() {
    return namespace;
  }

  @Override
  public void setNamespace(String s) {
    throw err();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String s) {
    throw err();
  }

  @Override
  public String getFullName() {
    return StringUtils.isBlank(namespace) ? name : namespace + "." + name;
  }

  @Override
  public String getLabel() {
    return label;
  }

  @Override
  public void setLabel(String s) {
    throw err();
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public void setDescription(String s) {
    throw err();
  }

  @Override
  public String getRawName() {
    return rawName;
  }

  @Override
  public void setRawName(String s) {
    throw err();
  }

  @Override
  public String getDataStation() {
    return dataStation;
  }

  @Override
  public void setDataStation(String s) {
    throw err();
  }

  @Override
  public MigrationStrategy getMigrationStrategy() {
    return migrationStrategy;
  }

  @Override
  public void setMigrationStrategy(MigrationStrategy m) {
    throw err();
  }

  @Override
  public List<DataModelFieldMeta> getFields() {
    return fields;
  }

  @Override
  public void setFields(List<DataModelFieldMeta> f) {
    throw err();
  }

  @Override
  public List<String> getPrimaryKeys() {
    return primaryKeys;
  }

  @Override
  public void setPrimaryKeys(List<String> pk) {
    throw err();
  }

  @Override
  public List<DataModelUniqueConstraintMeta> getUniqueConstraints() {
    return uniqueConstraints;
  }

  @Override
  public void setUniqueConstraints(List<DataModelUniqueConstraintMeta> uniqueConstraints) {
    throw err();
  }

  @Override
  public void addUniqueConstraint(DataModelUniqueConstraintMeta uniqueConstraint) {
    throw err();
  }

  @Override
  public String getPrimaryKeyGenerator() {
    return primaryKeyGenerator;
  }

  @Override
  public void setPrimaryKeyGenerator(String g) {
    throw err();
  }

  @Override
  public List<PluginDescriptor> getPluginDescriptors() {
    return pluginDescriptors;
  }

  @Override
  public void setPluginDescriptors(List<PluginDescriptor> p) {
    throw err();
  }

  @Override
  public void addPluginDescriptor(PluginDescriptor p) {
    throw err();
  }

  @Override
  public void removePluginDescriptor(String n) {
    throw err();
  }

  @Override
  public Map<String, Object> getExtraProps() {
    return extraProps;
  }

  @Override
  public void setExtraProps(Map<String, Object> m) {
    throw err();
  }

  @Override
  public void setExtraProp(String k, Object v) {
    throw err();
  }

  // ============= Copy & Clone =============
  @Override
  public ImmutableDataModelMeta deepCopy() {
    return new ImmutableDataModelMeta(this);
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    return deepCopy();
  }

  // ============= Immutable Inner Classes =============
  public static final class ImmutableFieldMeta extends DataModelFieldMeta {
    private final String name;
    private final String label;
    private final String description;
    private final String rawName;
    private final String type;
    private final String rawType;
    private final Map<String, Object> extraProps;
    private final Boolean isNullable;
    private final Boolean isUnsigned;
    private final Boolean isAutoIncrement;
    private final Boolean isUnique;
    private final UniquenessScope uniquenessScope;
    private final List<String> rules;
    private final String defaultValue;
    private final Integer decimalDigits;
    private final Integer size;

    public ImmutableFieldMeta(DataModelFieldMeta src) {
      this.name = src.getName();
      this.label = src.getLabel();
      this.description = src.getDescription();
      this.rawName = src.getRawName();
      this.type = src.getType();
      this.rawType = src.getRawType();
      this.decimalDigits = src.getDecimalDigits();
      this.size = src.getSize();
      this.isNullable = src.getIsNullable();
      this.isUnsigned = src.getIsUnsigned();
      this.isAutoIncrement = src.getIsAutoIncrement();
      this.isUnique = src.getIsUnique();
      this.uniquenessScope = src.getUniquenessScope();
      this.defaultValue = src.getDefaultValue();

      List<String> rs = src.getRules();
      this.rules = ObjectUtils.isEmpty(rs)
          ? Collections.emptyList()
          : Collections.unmodifiableList(
          rs.stream()
              .filter(Objects::nonNull)
              .collect(Collectors.toList()));

      Map<String, Object> ep = src.getExtraProps();
      this.extraProps = ObjectUtils.isEmpty(ep)
          ? Collections.emptyMap()
          : Collections.unmodifiableMap(DataMaps.deepClone(ep));
    }

    private static UnsupportedOperationException err() {
      return new UnsupportedOperationException("ImmutableFieldMeta is read-only");
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public void setName(String v) {
      throw err();
    }

    @Override
    public String getLabel() {
      return label;
    }

    @Override
    public void setLabel(String v) {
      throw err();
    }

    @Override
    public String getDescription() {
      return description;
    }

    @Override
    public void setDescription(String v) {
      throw err();
    }

    @Override
    public String getRawName() {
      return rawName;
    }

    @Override
    public void setRawName(String v) {
      throw err();
    }

    @Override
    public String getType() {
      return type;
    }

    @Override
    public void setType(String v) {
      throw err();
    }

    @Override
    public String getRawType() {
      return rawType;
    }

    @Override
    public void setRawType(String v) {
      throw err();
    }

    @Override
    public Integer getDecimalDigits() {
      return decimalDigits;
    }

    @Override
    public void setDecimalDigits(Integer v) {
      throw err();
    }

    @Override
    public Integer getSize() {
      return size;
    }

    @Override
    public void setSize(Integer v) {
      throw err();
    }

    @Override
    public Boolean getIsNullable() {
      return isNullable;
    }

    @Override
    public void setIsNullable(Boolean v) {
      throw err();
    }

    @Override
    public Boolean getIsUnsigned() {
      return isUnsigned;
    }

    @Override
    public void setIsUnsigned(Boolean v) {
      throw err();
    }

    @Override
    public Boolean getIsAutoIncrement() {
      return isAutoIncrement;
    }

    @Override
    public void setIsAutoIncrement(Boolean v) {
      throw err();
    }

    @Override
    public Boolean getIsUnique() {
      return isUnique;
    }

    @Override
    public void setIsUnique(Boolean v) {
      throw err();
    }

    @Override
    public UniquenessScope getUniquenessScope() {
      return uniquenessScope;
    }

    @Override
    public void setUniquenessScope(UniquenessScope v) {
      throw err();
    }

    @Override
    public List<String> getRules() {
      return rules;
    }

    @Override
    public void setRules(List<String> v) {
      throw err();
    }

    @Override
    public String getDefaultValue() {
      return defaultValue;
    }

    @Override
    public void setDefaultValue(String v) {
      throw err();
    }

    @Override
    public Map<String, Object> getExtraProps() {
      return extraProps;
    }

    @Override
    public void setExtraProps(Map<String, Object> m) {
      throw err();
    }

    @Override
    public void setExtraProp(String k, Object v) {
      throw err();
    }

    @Override
    public ImmutableFieldMeta deepCopy() {
      return new ImmutableFieldMeta(this);
    }
  }

  public static final class ImmutableUniqueConstraintMeta extends DataModelUniqueConstraintMeta {
    private final String name;
    private final List<String> fields;
    private final UniquenessScope scope;
    private final Map<String, Object> extraProps;

    public ImmutableUniqueConstraintMeta(DataModelUniqueConstraintMeta src) {
      this.name = src.getName();
      List<String> fs = src.getFields();
      this.fields = ObjectUtils.isEmpty(fs)
          ? Collections.emptyList()
          : Collections.unmodifiableList(
          fs.stream()
              .filter(Objects::nonNull)
              .collect(Collectors.toList()));
      this.scope = src.getScope();
      Map<String, Object> ep = src.getExtraProps();
      this.extraProps = ObjectUtils.isEmpty(ep)
          ? Collections.emptyMap()
          : Collections.unmodifiableMap(DataMaps.deepClone(ep));
    }

    private static UnsupportedOperationException err() {
      return new UnsupportedOperationException("ImmutableUniqueConstraintMeta is read-only");
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public void setName(String v) {
      throw err();
    }

    @Override
    public List<String> getFields() {
      return fields;
    }

    @Override
    public void setFields(List<String> v) {
      throw err();
    }

    @Override
    public UniquenessScope getScope() {
      return scope;
    }

    @Override
    public void setScope(UniquenessScope v) {
      throw err();
    }

    @Override
    public Map<String, Object> getExtraProps() {
      return extraProps;
    }

    @Override
    public void setExtraProps(Map<String, Object> v) {
      throw err();
    }

    @Override
    public void setExtraProp(String k, Object v) {
      throw err();
    }

    @Override
    public ImmutableUniqueConstraintMeta deepCopy() {
      return new ImmutableUniqueConstraintMeta(this);
    }
  }

  public static final class ImmutablePluginDescriptor extends PluginDescriptor {
    private final String name;
    private final Map<String, Object> config;

    public ImmutablePluginDescriptor(PluginDescriptor src) {
      this.name = src.getName();
      Map<String, Object> cfg = src.getConfig();
      this.config = ObjectUtils.isEmpty(cfg)
          ? Collections.emptyMap()
          : Collections.unmodifiableMap(DataMaps.deepClone(cfg));
    }

    private static UnsupportedOperationException err() {
      return new UnsupportedOperationException("ImmutablePluginDescriptor is read-only");
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public void setName(String v) {
      throw err();
    }

    @Override
    public Map<String, Object> getConfig() {
      return config;
    }

    @Override
    public void setConfig(Map<String, Object> m) {
      throw err();
    }

    @Override
    public ImmutablePluginDescriptor deepCopy() {
      return new ImmutablePluginDescriptor(this);
    }
  }
}
