package com.ouroboros.data.model;

import java.util.Collections;
import java.util.Map;

import com.ouroboros.data.exception.MetadataException;
import com.ouroboros.data.util.DataMaps;


/**
 * PluginDescriptor类实现了Cloneable接口，用于描述插件的基本信息和配置
 */
public class PluginDescriptor implements Cloneable {

  // 插件名称
  private String name;

  // 插件配置信息，使用Map存储键值对
  private Map<String, Object> config;

  // 无参构造方法
  public PluginDescriptor() {
  }

  // 带名称参数的构造方法
  public PluginDescriptor(String name) {
    this.name = name;
  }

  // 带名称和配置参数的构造方法
  public PluginDescriptor(String name, Map<String, Object> config) {
    this.name = name;
    this.config = config;
  }

  // 获取插件名称
  public String getName() {
    return name;
  }

  // 设置插件名称
  public void setName(String name) {
    this.name = name;
  }

  // 获取插件配置，如果配置为null则返回空Map
  public Map<String, Object> getConfig() {
    return config != null ? config : Collections.emptyMap();
  }

  // 设置插件配置
  public void setConfig(Map<String, Object> config) {
    this.config = config;
  }

  // 创建插件描述符的深拷贝
  public PluginDescriptor deepCopy() {
    try {
      // 克隆当前对象
      PluginDescriptor pluginDescriptor = (PluginDescriptor) super.clone();
      // 深度克隆配置Map
      pluginDescriptor.setConfig(DataMaps.deepClone(this.getConfig()));
      return pluginDescriptor;
    } catch (CloneNotSupportedException e) {
      // 如果不支持克隆，抛出运行时异常
      throw new MetadataException("Clone failed", e);
    }
  }

  // 重写clone方法，内部调用deepCopy方法
  @Override
  protected Object clone() throws CloneNotSupportedException {
    return deepCopy();
  }
}
