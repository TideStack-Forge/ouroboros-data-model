package com.ouroboros.data.model;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.ouroboros.data.util.DataMaps;

/**
 * DataModelMetaPatch 类用于创建和操作数据模型元数据的补丁版本。
 * 它继承自 DataModelMeta 类，允许在不修改原始元数据的情况下进行局部覆盖。
 */
public class DataModelMetaPatch extends DataModelMeta {

  /**
   * 原始未修改的Meta
   */
  private final DataModelMeta original;

  /**
   * 局部覆盖的字段，用于存储差异
   */
  private Map<String, Object> overrides = new HashMap<>();

  /**
   * 字段级 diff，例如 fields、extraProps 等
   */
  private List<DataModelFieldMeta> overrideFields;
  private Map<String, Object> overrideExtraProps;
  private List<String> overridePrimaryKeys;
  private List<DataModelUniqueConstraintMeta> overrideUniqueConstraints;
  private List<PluginDescriptor> overridePluginDescriptors;

  /**
   * 构造函数，创建一个新的 DataModelMetaPatch 实例
   *
   * @param original 原始的 DataModelMeta 对象
   */
  public DataModelMetaPatch(DataModelMeta original) {
    if (original instanceof DataModelMetaPatch patch) {
      // 如果原始对象已经是补丁类型，则创建嵌套补丁
      this.original = patch.original;
      this.overrides = DataMaps.deepClone(patch.overrides);

      // 深度复制覆盖字段
      this.overrideFields = patch.overrideFields != null
          ? patch.overrideFields.stream().map(DataModelFieldMeta::deepCopy).collect(Collectors.toList())
          : null;

      // 深度复制额外属性
      this.overrideExtraProps = patch.overrideExtraProps != null
          ? DataMaps.deepClone(patch.overrideExtraProps)
          : null;

      // 深度复制插件描述符
      this.overridePluginDescriptors = patch.overridePluginDescriptors != null
          ? patch.overridePluginDescriptors.stream().map(PluginDescriptor::deepCopy).collect(Collectors.toList())
          : null;

      // 深度复制唯一约束
      this.overrideUniqueConstraints = patch.overrideUniqueConstraints != null
          ? patch.overrideUniqueConstraints.stream().map(DataModelUniqueConstraintMeta::deepCopy).collect(Collectors.toList())
          : null;

      // 复制主键列表
      this.overridePrimaryKeys = patch.overridePrimaryKeys != null
          ? new ArrayList<>(patch.overridePrimaryKeys)
          : null;
    } else {
      // 如果原始对象不是补丁类型，直接使用原始对象
      this.original = Objects.requireNonNull(original, "original cannot be null");
    }
  }

  // ============= 核心工具方法 =============

  /**
   * 获取覆盖值或原始值
   *
   * @param key            属性键
   * @param originalGetter 原始值获取函数
   * @return 覆盖值或原始值
   */
  @SuppressWarnings("unchecked")
  private <T> T getOverride(String key, Supplier<T> originalGetter) {
    if (overrides.containsKey(key)) {
      return (T) overrides.get(key);
    }
    return originalGetter.get();
  }

  /**
   * 设置覆盖值
   *
   * @param key   属性键
   * @param value 覆盖值
   */
  private void setOverride(String key, Object value) {
    overrides.put(key, value);
  }

  // ============= 基础字段 Override =============

  /**
   * 获取格式版本
   *
   * @return 格式版本字符串
   */
  @Override
  public String getFormatVersion() {
    return getOverride("formatVersion", original::getFormatVersion);
  }

  /**
   * 设置格式版本
   *
   * @param formatVersion 格式版本字符串
   */
  @Override
  public void setFormatVersion(String formatVersion) {
    setOverride("formatVersion", formatVersion);
  }

  /**
   * 获取数据源
   *
   * @return 数据源字符串
   */
  @Override
  public String getSource() {
    return getOverride("source", original::getSource);
  }

  /**
   * 设置数据源
   *
   * @param source 数据源字符串
   */
  @Override
  public void setSource(String source) {
    setOverride("source", source);
  }

  /**
   * 获取命名空间
   *
   * @return 命名空间字符串
   */
  @Override
  public String getNamespace() {
    return getOverride("namespace", original::getNamespace);
  }

  /**
   * 设置命名空间
   *
   * @param namespace 命名空间字符串
   */
  @Override
  public void setNamespace(String namespace) {
    setOverride("namespace", namespace);
  }

  /**
   * 获取名称
   *
   * @return 名称字符串
   */
  @Override
  public String getName() {
    return getOverride("name", original::getName);
  }

  /**
   * 设置名称
   *
   * @param name 名称字符串
   */
  @Override
  public void setName(String name) {
    setOverride("name", name);
  }

  /**
   * 获取标签
   *
   * @return 标签字符串
   */
  @Override
  public String getLabel() {
    return getOverride("label", original::getLabel);
  }

  /**
   * 设置标签
   *
   * @param label 标签字符串
   */
  @Override
  public void setLabel(String label) {
    setOverride("label", label);
  }

  /**
   * 获取描述
   *
   * @return 描述字符串
   */
  @Override
  public String getDescription() {
    return getOverride("description", original::getDescription);
  }

  /**
   * 设置描述
   *
   * @param description 描述字符串
   */
  @Override
  public void setDescription(String description) {
    setOverride("description", description);
  }

  /**
   * 获取原始名称
   *
   * @return 原始名称字符串
   */
  @Override
  public String getRawName() {
    return getOverride("rawName", original::getRawName);
  }

  /**
   * 设置原始名称
   *
   * @param rawName 原始名称字符串
   */
  @Override
  public void setRawName(String rawName) {
    setOverride("rawName", rawName);
  }

  /**
   * 获取迁移策略
   *
   * @return 迁移策略对象
   */
  @Override
  public MigrationStrategy getMigrationStrategy() {
    return getOverride("migrationStrategy", original::getMigrationStrategy);
  }

  /**
   * 设置迁移策略
   *
   * @param migrationStrategy 迁移策略对象
   */
  @Override
  public void setMigrationStrategy(MigrationStrategy migrationStrategy) {
    setOverride("migrationStrategy", migrationStrategy);
  }

  // ============= Fields 覆盖 =============

  /**
   * 获取字段列表
   *
   * @return 字段元数据列表
   */
  @Override
  public List<DataModelFieldMeta> getFields() {
    return overrideFields != null ? overrideFields : original.getFields();
  }

  /**
   * 设置字段列表
   *
   * @param fields 字段元数据列表
   */
  @Override
  public void setFields(List<DataModelFieldMeta> fields) {
    this.overrideFields = fields;
  }

  // ============= Primary Keys 覆盖 =============

  /**
   * 获取主键列表
   *
   * @return 主键字符串列表
   */
  @Override
  public List<String> getPrimaryKeys() {
    return overridePrimaryKeys != null ? overridePrimaryKeys : original.getPrimaryKeys();
  }

  /**
   * 设置主键列表
   *
   * @param primaryKeys 主键字符串列表
   */
  @Override
  public void setPrimaryKeys(List<String> primaryKeys) {
    this.overridePrimaryKeys = primaryKeys;
  }

  @Override
  public List<DataModelUniqueConstraintMeta> getUniqueConstraints() {
    return overrideUniqueConstraints != null ? overrideUniqueConstraints : original.getUniqueConstraints();
  }

  @Override
  public void setUniqueConstraints(List<DataModelUniqueConstraintMeta> uniqueConstraints) {
    this.overrideUniqueConstraints = uniqueConstraints;
  }

  @Override
  public void addUniqueConstraint(DataModelUniqueConstraintMeta uniqueConstraint) {
    if (overrideUniqueConstraints == null) {
      overrideUniqueConstraints = original.getUniqueConstraints().stream()
          .map(DataModelUniqueConstraintMeta::deepCopy)
          .collect(Collectors.toList());
    }
    overrideUniqueConstraints.add(uniqueConstraint);
  }

  /**
   * 获取主键生成器
   *
   * @return 主键生成器字符串
   */
  @Override
  public String getPrimaryKeyGenerator() {
    return getOverride("primaryKeyGenerator", original::getPrimaryKeyGenerator);
  }

  /**
   * 设置主键生成器
   *
   * @param g 主键生成器字符串
   */
  @Override
  public void setPrimaryKeyGenerator(String g) {
    setOverride("primaryKeyGenerator", g);
  }

  // ============= Plugin Descriptors 覆盖 =============

  /**
   * 获取插件描述符列表
   *
   * @return 插件描述符列表
   */
  @Override
  public List<PluginDescriptor> getPluginDescriptors() {
    return overridePluginDescriptors != null ? overridePluginDescriptors : original.getPluginDescriptors();
  }

  /**
   * 设置插件描述符列表
   *
   * @param pluginDescriptors 插件描述符列表
   */
  @Override
  public void setPluginDescriptors(List<PluginDescriptor> pluginDescriptors) {
    this.overridePluginDescriptors = pluginDescriptors;
  }

  /**
   * 添加插件描述符
   *
   * @param pluginDescriptor 要添加的插件描述符
   */
  @Override
  public void addPluginDescriptor(PluginDescriptor pluginDescriptor) {
    if (overridePluginDescriptors == null) {
      overridePluginDescriptors = new ArrayList<>(original.getPluginDescriptors());
    }
    overridePluginDescriptors.add(pluginDescriptor);
  }

  /**
   * 移除插件描述符
   *
   * @param pluginName 要移除的插件名称
   */
  @Override
  public void removePluginDescriptor(String pluginName) {
    if (overridePluginDescriptors == null) {
      overridePluginDescriptors = new ArrayList<>(original.getPluginDescriptors());
    }
    overridePluginDescriptors.removeIf(p -> pluginName.equalsIgnoreCase(p.getName()));
  }

  // ============= Extra Props 覆盖 =============

  /**
   * 获取额外属性
   *
   * @return 额外属性映射表
   */
  @Override
  public Map<String, Object> getExtraProps() {
    if (overrideExtraProps == null) {
      return original.getExtraProps();
    }
    return overrideExtraProps;
  }

  /**
   * 设置额外属性
   *
   * @param extraProps 额外属性映射表
   */
  @Override
  public void setExtraProps(Map<String, Object> extraProps) {
    this.overrideExtraProps = extraProps;
  }

  /**
   * 设置单个额外属性
   *
   * @param key   属性键
   * @param value 属性值
   */
  @Override
  public void setExtraProp(String key, Object value) {
    if (overrideExtraProps == null) {
      overrideExtraProps = new LinkedHashMap<>(original.getExtraProps());
    }
    overrideExtraProps.put(key, value);
  }

  /**
   * 创建对象的深度副本
   *
   * @return 新的 DataModelMetaPatch 实例
   */
  @Override
  public DataModelMetaPatch deepCopy() {
    DataModelMetaPatch copy = new DataModelMetaPatch(this.original);

    // 深度复制覆盖映射表
    copy.overrides = DataMaps.deepClone(this.overrides);

    // 深度复制字段列表
    copy.overrideFields = this.overrideFields != null
        ? this.overrideFields.stream().map(DataModelFieldMeta::deepCopy).collect(Collectors.toList())
        : null;

    // 深度复制额外属性
    copy.overrideExtraProps = this.overrideExtraProps != null
        ? DataMaps.deepClone(this.overrideExtraProps)
        : null;

    // 深度复制插件描述符
    copy.overridePluginDescriptors = this.overridePluginDescriptors != null
        ? this.overridePluginDescriptors.stream().map(PluginDescriptor::deepCopy).collect(Collectors.toList())
        : null;

    // 深度复制唯一约束
    copy.overrideUniqueConstraints = this.overrideUniqueConstraints != null
        ? this.overrideUniqueConstraints.stream().map(DataModelUniqueConstraintMeta::deepCopy).collect(Collectors.toList())
        : null;

    // 复制主键列表
    copy.overridePrimaryKeys = this.overridePrimaryKeys != null
        ? new ArrayList<>(this.overridePrimaryKeys)
        : null;

    return copy;
  }

  /**
   * 克隆对象
   *
   * @return 克隆的对象
   * @throws CloneNotSupportedException 如果对象不支持克隆
   */
  @Override
  public Object clone() throws CloneNotSupportedException {
    return deepCopy();
  }
}
