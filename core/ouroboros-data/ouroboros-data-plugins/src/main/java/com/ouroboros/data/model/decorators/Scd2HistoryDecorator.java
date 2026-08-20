package com.ouroboros.data.model.decorators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.annotation.Priority;

import com.ouroboros.data.model.DataModelMeta;
import com.ouroboros.data.model.DataModelMetaDecorator;
import com.ouroboros.data.model.PluginDescriptor;

@Priority(999975)
public class Scd2HistoryDecorator implements DataModelMetaDecorator {

  private static final String ENABLE_SCD2_HISTORY = "enableScd2History";
  private static final String SCD2_HISTORY_CONFIG = "scd2HistoryConfig";
  private static final String PLUGIN_ACCESS_CONTROL = "AccessControl";
  private static final String PLUGIN_SCD2_HISTORY = "Scd2History";
  private static final List<String> DELETE_PLUGINS = Arrays.asList("SoftDelete", "LogicalDelete", "ArchiveDelete");

  @Override
  public boolean supports(DataModelMeta originalMeta) {
    return originalMeta.getExtraProp(Boolean.class, ENABLE_SCD2_HISTORY).orElse(false);
  }

  @Override
  public DataModelMeta decorate(DataModelMeta originalMeta) {
    DataModelMeta decoratedMeta = originalMeta.deepCopy();
    PluginDescriptor historyPlugin = new PluginDescriptor(PLUGIN_SCD2_HISTORY, loadConfig(decoratedMeta));
    decoratedMeta.setPluginDescriptors(insertHistoryPlugin(decoratedMeta.getPluginDescriptors(), historyPlugin));
    return decoratedMeta;
  }

  private List<PluginDescriptor> insertHistoryPlugin(List<PluginDescriptor> existingPlugins, PluginDescriptor historyPlugin) {
    List<PluginDescriptor> plugins = new ArrayList<PluginDescriptor>();
    if (existingPlugins != null) {
      existingPlugins.stream()
          .filter(plugin -> !PLUGIN_SCD2_HISTORY.equalsIgnoreCase(plugin.getName()))
          .forEach(plugins::add);
    }

    int accessControlIndex = findLastPluginIndex(plugins, PLUGIN_ACCESS_CONTROL);
    if (accessControlIndex >= 0) {
      plugins.add(accessControlIndex + 1, historyPlugin);
      return plugins;
    }

    int deletePluginIndex = findFirstPluginIndex(plugins, DELETE_PLUGINS);
    if (deletePluginIndex >= 0) {
      plugins.add(deletePluginIndex, historyPlugin);
      return plugins;
    }

    plugins.add(historyPlugin);
    return plugins;
  }

  private int findLastPluginIndex(List<PluginDescriptor> plugins, String pluginName) {
    for (int i = plugins.size() - 1; i >= 0; i--) {
      if (pluginName.equalsIgnoreCase(plugins.get(i).getName())) {
        return i;
      }
    }
    return -1;
  }

  private int findFirstPluginIndex(List<PluginDescriptor> plugins, List<String> pluginNames) {
    for (int i = 0; i < plugins.size(); i++) {
      String currentPluginName = plugins.get(i).getName();
      if (pluginNames.stream().anyMatch(pluginName -> pluginName.equalsIgnoreCase(currentPluginName))) {
        return i;
      }
    }
    return -1;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> loadConfig(DataModelMeta meta) {
    return meta.getExtraProp(Map.class, SCD2_HISTORY_CONFIG)
        .map(config -> (Map<String, Object>) config)
        .map(Collections::unmodifiableMap)
        .orElseGet(Collections::emptyMap);
  }
}
