package com.ouroboros.data.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;

import org.junit.jupiter.api.Test;

class DataModelMetaPatchCoverageTest {

  @Test
  void patchOverridesCollectionsAndClonePaths() throws CloneNotSupportedException {
    var base = new DataModelMeta();
    base.setFormatVersion("v1");
    base.setSource("db");
    base.setNamespace("ns");
    base.setName("user");
    base.setLabel("User");
    base.setDescription("desc");
    base.setRawName("t_user");
    base.setMigrationStrategy(MigrationStrategy.DISABLED);
    base.setPrimaryKeyGenerator("snowflake");
    base.setFields(new ArrayList<DataModelFieldMeta>(Collections.singletonList(field("id"))));
    base.setPrimaryKeys(new ArrayList<String>(Collections.singletonList("id")));
    base.setPluginDescriptors(new ArrayList<PluginDescriptor>(Collections.singletonList(new PluginDescriptor("audit"))));
    base.setUniqueConstraints(new ArrayList<DataModelUniqueConstraintMeta>(
        Collections.singletonList(uniqueConstraint("base_code", "code"))));
    base.setExtraProps(new LinkedHashMap<String, Object>());
    base.setExtraProp("base", 1);

    var patch = new DataModelMetaPatch(base);
    patch.setFormatVersion("v2");
    patch.setSource("db2");
    patch.setNamespace("ns2");
    patch.setName("user2");
    patch.setLabel("User2");
    patch.setDescription("desc2");
    patch.setRawName("t_user2");
    patch.setMigrationStrategy(MigrationStrategy.AUTO);
    patch.setPrimaryKeyGenerator("uuid");
    patch.setFields(new ArrayList<DataModelFieldMeta>(Collections.singletonList(field("name"))));
    patch.setPrimaryKeys(new ArrayList<String>(Arrays.asList("id", "tenantId")));
    patch.setUniqueConstraints(new ArrayList<DataModelUniqueConstraintMeta>(
        Collections.singletonList(uniqueConstraint("tenant_code", "tenantId", "code"))));
    patch.addUniqueConstraint(uniqueConstraint("parent_code", "parentId", "code"));
    patch.addPluginDescriptor(new PluginDescriptor("history"));
    patch.removePluginDescriptor("audit");
    patch.setExtraProps(new LinkedHashMap<String, Object>());
    patch.setExtraProp("k", "v");

    assertEquals("v2", patch.getFormatVersion());
    assertEquals("db2", patch.getSource());
    assertEquals("ns2", patch.getNamespace());
    assertEquals("user2", patch.getName());
    assertEquals("User2", patch.getLabel());
    assertEquals("desc2", patch.getDescription());
    assertEquals("t_user2", patch.getRawName());
    assertEquals(MigrationStrategy.AUTO, patch.getMigrationStrategy());
    assertEquals("uuid", patch.getPrimaryKeyGenerator());
    assertEquals(1, patch.getFields().size());
    assertEquals(2, patch.getPrimaryKeys().size());
    assertEquals(2, patch.getUniqueConstraints().size());
    assertEquals("tenant_code", patch.getUniqueConstraints().get(0).getName());
    assertEquals(1, patch.getPluginDescriptors().size());
    assertEquals("history", patch.getPluginDescriptors().get(0).getName());
    assertEquals("v", patch.getExtraProps().get("k"));

    var nestedPatch = new DataModelMetaPatch(patch);
    var deepCopy = nestedPatch.deepCopy();
    var cloned = (DataModelMetaPatch) nestedPatch.clone();

    deepCopy.setExtraProp("k", "v2");
    deepCopy.getPrimaryKeys().add("orgId");
    deepCopy.getFields().get(0).setLabel("changed");
    deepCopy.getUniqueConstraints().get(0).setName("changed_code");
    deepCopy.addUniqueConstraint(uniqueConstraint("copy_code", "copyId", "code"));
    deepCopy.addPluginDescriptor(new PluginDescriptor("extra"));

    assertEquals("v", nestedPatch.getExtraProps().get("k"));
    assertEquals(2, nestedPatch.getPrimaryKeys().size());
    assertFalse("changed".equals(nestedPatch.getFields().get(0).getLabel()));
    assertEquals("tenant_code", nestedPatch.getUniqueConstraints().get(0).getName());
    assertEquals(2, nestedPatch.getUniqueConstraints().size());
    assertEquals(1, nestedPatch.getPluginDescriptors().size());
    assertNotSame(nestedPatch.getExtraProps(), deepCopy.getExtraProps());
    assertTrue(cloned.getPrimaryKeys().contains("id"));
    assertEquals(2, cloned.getUniqueConstraints().size());
  }

  private static DataModelFieldMeta field(String name) {
    var f = new DataModelFieldMeta();
    f.setName(name);
    f.setType("String");
    return f;
  }

  private static DataModelUniqueConstraintMeta uniqueConstraint(String name, String... fields) {
    var constraint = new DataModelUniqueConstraintMeta();
    constraint.setName(name);
    constraint.setFields(new ArrayList<String>(Arrays.asList(fields)));
    return constraint;
  }
}
