package com.ouroboros.data.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.TreeMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DataModelCenterResolverTest {

  @Test
  void dataModelSnapshotShouldBePublishedWithVolatileVisibility() throws Exception {
    Field field = DataModelCenter.class.getDeclaredField("dataModelMap");

    assertTrue(Modifier.isVolatile(field.getModifiers()));
  }

  @AfterEach
  void resetDataModelMap() throws Exception {
    Field field = DataModelCenter.class.getDeclaredField("dataModelMap");
    field.setAccessible(true);
    field.set(null, null);
  }

  @Test
  void reportsWhetherAPublishedSnapshotIsAvailable() throws Exception {
    assertFalse(DataModelCenter.hasSnapshot());

    var runtimeModel = model("RuntimeModel");
    var dataModels = new TreeMap<String, DataModel>(String.CASE_INSENSITIVE_ORDER);
    dataModels.put(runtimeModel.getFullName(), runtimeModel);

    Field field = DataModelCenter.class.getDeclaredField("dataModelMap");
    field.setAccessible(true);
    field.set(null, dataModels);

    assertTrue(DataModelCenter.hasSnapshot());
    assertFalse(DataModelCenter.getDataModel("DesignModel").isPresent());
    assertSame(runtimeModel, DataModelCenter.getDataModel("RuntimeModel").orElse(null));
  }

  @Test
  void metadataOnlyDataModelExposesMetadataWithoutDataOperations() {
    var model = model("MetadataOnlyModel");

    assertEquals("MetadataOnlyModel", model.getFullName());
    assertEquals("id", model.getPrimaryKeys().get(0).getName());
    assertFalse(model.insert(Collections.emptyMap()).isSuccess());
  }

  private static MetadataOnlyDataModel model(String name) {
    var id = new DataModelFieldMeta();
    id.setName("id");
    id.setType("String");

    var meta = new DataModelMeta();
    meta.setName(name);
    meta.setFields(Collections.singletonList(id));
    meta.setPrimaryKeys(Collections.singletonList("id"));
    return new MetadataOnlyDataModel(meta);
  }
}
