package com.ouroboros.data.model.decorators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.model.DataModelMeta;
import com.ouroboros.data.model.DataModelMetaDecorator;
import com.ouroboros.data.model.PluginDescriptor;

public class Scd2HistoryDecoratorTest {

  @Test
  public void supportsEnableScd2History() {
    DataModelMeta meta = new DataModelMeta();
    meta.setName("User");
    meta.setExtraProp("enableScd2History", true);
    meta.setExtraProp("scd2HistoryConfig", historyConfig());

    assertTrue(new Scd2HistoryDecorator().supports(meta));
  }

  @Test
  public void decorateShouldInsertScd2HistoryBetweenAccessControlAndLogicalDelete() {
    DataModelMeta meta = new DataModelMeta();
    meta.setName("User");
    meta.setExtraProp("enableScd2History", true);
    meta.setExtraProp("scd2HistoryConfig", historyConfig());
    meta.setPluginDescriptors(Arrays.asList(
        new PluginDescriptor("AccessControl"),
        new PluginDescriptor("LogicalDelete")
    ));

    DataModelMeta decoratedMeta = new Scd2HistoryDecorator().decorate(meta);

    assertEquals(Arrays.asList("AccessControl", "Scd2History", "LogicalDelete"), pluginNames(decoratedMeta));
    assertEquals("demo.UserHistory", decoratedMeta.getPluginDescriptors().get(1).getConfig().get("historyModelFullName"));
  }

  @Test
  public void decorateShouldInsertScd2HistoryBeforeArchiveDelete() {
    DataModelMeta meta = new DataModelMeta();
    meta.setName("User");
    meta.setExtraProp("enableScd2History", true);
    meta.setExtraProp("scd2HistoryConfig", historyConfig());
    meta.setPluginDescriptors(Arrays.asList(
        new PluginDescriptor("ArchiveDelete")
    ));

    DataModelMeta decoratedMeta = new Scd2HistoryDecorator().decorate(meta);

    assertEquals(Arrays.asList("Scd2History", "ArchiveDelete"), pluginNames(decoratedMeta));
  }

  @Test
  public void decorateShouldInsertScd2HistoryBeforeLegacySoftDelete() {
    DataModelMeta meta = new DataModelMeta();
    meta.setName("User");
    meta.setExtraProp("enableScd2History", true);
    meta.setExtraProp("scd2HistoryConfig", historyConfig());
    meta.setPluginDescriptors(Arrays.asList(
        new PluginDescriptor("SoftDelete")
    ));

    DataModelMeta decoratedMeta = new Scd2HistoryDecorator().decorate(meta);

    assertEquals(Arrays.asList("Scd2History", "SoftDelete"), pluginNames(decoratedMeta));
  }

  @Test
  public void applyDecoratorsShouldPlaceScd2HistoryBetweenAuditAndLogicalDelete() {
    DataModelMeta meta = new DataModelMeta();
    meta.setName("User");
    meta.setExtraProp("enableAuditFields", true);
    meta.setExtraProp("enableScd2History", true);
    meta.setExtraProp("enableSoftDelete", true);
    meta.setExtraProp("scd2HistoryConfig", historyConfig());

    DataModelMeta decoratedMeta = DataModelMetaDecorator.applyDecorators(meta);

    assertEquals(Arrays.asList("BasicAudit", "Scd2History", "LogicalDelete"), pluginNames(decoratedMeta));
  }

  private List<String> pluginNames(DataModelMeta meta) {
    return meta.getPluginDescriptors().stream()
        .map(PluginDescriptor::getName)
        .collect(Collectors.toList());
  }

  private Map<String, Object> historyConfig() {
    Map<String, Object> config = new LinkedHashMap<String, Object>();
    config.put("historyModelFullName", "demo.UserHistory");
    return config;
  }
}
