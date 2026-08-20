package com.ouroboros.data.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ModelMetaAndDescriptorCoverageTest {

  @Test
  void pluginDescriptorCoversConstructorsConfigFallbackAndDeepCopy() {
    var empty = new PluginDescriptor();
    assertEquals(Collections.emptyMap(), empty.getConfig());

    var named = new PluginDescriptor("audit");
    assertEquals("audit", named.getName());
    assertEquals(Collections.emptyMap(), named.getConfig());

    var config = new LinkedHashMap<String, Object>();
    config.put("enabled", true);
    var descriptor = new PluginDescriptor("plugin", config);
    assertEquals("plugin", descriptor.getName());
    assertEquals(Boolean.TRUE, descriptor.getConfig().get("enabled"));

    descriptor.setName("renamed");
    assertEquals("renamed", descriptor.getName());

    var copied = descriptor.deepCopy();
    assertEquals("renamed", copied.getName());
    assertEquals(Boolean.TRUE, copied.getConfig().get("enabled"));

    copied.getConfig().put("enabled", false);
    assertEquals(Boolean.TRUE, descriptor.getConfig().get("enabled"));
    assertEquals(Boolean.FALSE, copied.getConfig().get("enabled"));
  }

  @Test
  void dataModelFieldMetaCoversSettersExtraPropsOptionalAndDeepCopy() {
    var fieldMeta = new DataModelFieldMeta();
    fieldMeta.setName("id");
    fieldMeta.setLabel("ID");
    fieldMeta.setDescription("identifier");
    fieldMeta.setRawName("id_col");
    fieldMeta.setType("Long");
    fieldMeta.setRawType("bigint");
    fieldMeta.setDecimalDigits(2);
    fieldMeta.setSize(64);
    fieldMeta.setIsNullable(false);
    fieldMeta.setIsUnsigned(true);
    fieldMeta.setIsAutoIncrement(true);
    fieldMeta.setIsUnique(null);
    fieldMeta.setUniquenessScope(UniquenessScope.ACTIVE_RECORDS);
    fieldMeta.setRules(Arrays.asList("NotNull", "NotEmpty"));
    fieldMeta.setDefaultValue("1");

    var extra = new HashMap<String, Object>();
    extra.put("x", 1);
    fieldMeta.setExtraProps(extra);
    fieldMeta.setExtraProp("y", "z");

    assertEquals("id", fieldMeta.getName());
    assertEquals("ID", fieldMeta.getLabel());
    assertEquals("identifier", fieldMeta.getDescription());
    assertEquals("id_col", fieldMeta.getRawName());
    assertEquals("Long", fieldMeta.getType());
    assertEquals("bigint", fieldMeta.getRawType());
    assertEquals(Integer.valueOf(2), fieldMeta.getDecimalDigits());
    assertEquals(Integer.valueOf(64), fieldMeta.getSize());
    assertFalse(fieldMeta.getIsNullable());
    assertTrue(fieldMeta.getIsUnsigned());
    assertTrue(fieldMeta.getIsAutoIncrement());
    assertFalse(fieldMeta.getIsUnique());
    assertEquals(UniquenessScope.ACTIVE_RECORDS, fieldMeta.getUniquenessScope());
    assertEquals(Arrays.asList("NotNull", "NotEmpty"), fieldMeta.getRules());
    assertEquals("1", fieldMeta.getDefaultValue());
    assertEquals(Integer.valueOf(1), fieldMeta.getExtraProp(Integer.class, "x").orElse(null));
    assertEquals("z", fieldMeta.getExtraProp(String.class, "y").orElse(null));
    assertFalse(fieldMeta.getExtraProp(String.class, "x").isPresent());

    var copied = fieldMeta.deepCopy();
    copied.setExtraProp("x", 2);
    assertEquals(Integer.valueOf(1), fieldMeta.getExtraProp(Integer.class, "x").orElse(null));
    assertEquals(Integer.valueOf(2), copied.getExtraProp(Integer.class, "x").orElse(null));
    assertEquals(UniquenessScope.ACTIVE_RECORDS, copied.getUniquenessScope());
  }

  @Test
  void dataModelMetaCoversNullBranchesCollectionsAndDeepCopy() {
    var meta = new DataModelMeta();
    meta.setFormatVersion("v1");
    meta.setSource("db");
    meta.setNamespace("ns");
    meta.setName("user");
    meta.setLabel("User");
    meta.setDescription("desc");
    meta.setRawName("t_user");
    meta.setDataStation("default");
    meta.setMigrationStrategy(MigrationStrategy.DISABLED);
    meta.setPrimaryKeyGenerator("snowflake");

    assertEquals("ns.user", meta.getFullName());

    meta.setNamespace(" ");
    assertEquals("user", meta.getFullName());

    var plugins = new ArrayList<PluginDescriptor>();
    plugins.add(new PluginDescriptor("A", new LinkedHashMap<String, Object>(Collections.singletonMap("k", "v"))));
    meta.setPluginDescriptors(plugins);
    meta.addPluginDescriptor(new PluginDescriptor("B"));
    meta.removePluginDescriptor("a");
    assertEquals(1, meta.getPluginDescriptors().size());
    assertEquals("B", meta.getPluginDescriptors().get(0).getName());

    meta.setPluginDescriptors(null);
    meta.addPluginDescriptor(new PluginDescriptor("C"));
    assertEquals(1, meta.getPluginDescriptors().size());

    var extra = new LinkedHashMap<String, Object>();
    extra.put("flag", true);
    meta.setExtraProps(extra);
    meta.setExtraProp("count", 1);
    assertEquals(Boolean.TRUE, meta.getExtraProp(Boolean.class, "flag").orElse(null));
    assertEquals(Integer.valueOf(1), meta.getExtraProp(Integer.class, "count").orElse(null));
    assertFalse(meta.getExtraProp(String.class, "count").isPresent());

    meta.setExtraProps(null);
    assertNotNull(meta.getExtraProps());
    assertTrue(meta.getExtraProps().isEmpty());

    var field = new DataModelFieldMeta();
    field.setName("id");
    field.setType("Long");
    field.setExtraProp("x", 1);
    meta.setFields(new ArrayList<DataModelFieldMeta>(Collections.singletonList(field)));

    meta.setPrimaryKeys(new ArrayList<String>(Collections.singletonList("id")));
    assertEquals(Collections.singletonList("id"), meta.getPrimaryKeys());

    var uniqueConstraint = uniqueConstraint("project_code", "projectId", "code");
    uniqueConstraint.setScope(UniquenessScope.ACTIVE_RECORDS);
    uniqueConstraint.setExtraProp("message", "duplicate");
    meta.setUniqueConstraints(new ArrayList<DataModelUniqueConstraintMeta>(Collections.singletonList(uniqueConstraint)));
    meta.addUniqueConstraint(uniqueConstraint("tenant_code", "tenantId", "code"));
    assertEquals(2, meta.getUniqueConstraints().size());

    var copied = meta.deepCopy();
    copied.setExtraProp("copied", true);
    copied.getPrimaryKeys().add("id2");
    copied.getFields().get(0).setExtraProp("x", 2);
    copied.getPluginDescriptors().add(new PluginDescriptor("D"));
    copied.getUniqueConstraints().get(0).setExtraProp("message", "changed");
    copied.addUniqueConstraint(uniqueConstraint("org_code", "orgId", "code"));

    assertFalse(meta.getExtraProp(Boolean.class, "copied").isPresent());
    assertEquals(1, meta.getPrimaryKeys().size());
    assertEquals(Integer.valueOf(1), meta.getFields().get(0).getExtraProp(Integer.class, "x").orElse(null));
    assertEquals(1, meta.getPluginDescriptors().size());
    assertEquals("duplicate", meta.getUniqueConstraints().get(0).getExtraProp(String.class, "message").orElse(null));
    assertEquals(2, meta.getUniqueConstraints().size());

    meta.setFields(null);
    assertEquals(Collections.emptyList(), meta.getFields());

    meta.setPrimaryKeys(null);
    assertEquals(Collections.emptyList(), meta.getPrimaryKeys());

    meta.setUniqueConstraints(null);
    assertEquals(Collections.emptyList(), meta.getUniqueConstraints());

    assertEquals("v1", meta.getFormatVersion());
    assertEquals("db", meta.getSource());
    assertEquals("user", meta.getName());
    assertEquals("User", meta.getLabel());
    assertEquals("desc", meta.getDescription());
    assertEquals("t_user", meta.getRawName());
    assertEquals("default", meta.getDataStation());
    assertEquals(MigrationStrategy.DISABLED, meta.getMigrationStrategy());
    assertEquals("snowflake", meta.getPrimaryKeyGenerator());
  }

  @Test
  void uniqueConstraintMetaCoversCollectionsExtraPropsAndDeepCopy() {
    var constraint = new DataModelUniqueConstraintMeta();
    constraint.setName("project_code");
    constraint.setFields(new ArrayList<String>(Arrays.asList("projectId", "code")));
    constraint.setScope(UniquenessScope.ACTIVE_RECORDS);
    constraint.setExtraProps(new LinkedHashMap<String, Object>(Collections.singletonMap("owner", "runtime")));
    constraint.setExtraProp("message", "duplicate");

    assertEquals("project_code", constraint.getName());
    assertEquals(Arrays.asList("projectId", "code"), constraint.getFields());
    assertEquals(UniquenessScope.ACTIVE_RECORDS, constraint.getScope());
    assertEquals("runtime", constraint.getExtraProp(String.class, "owner").orElse(null));
    assertEquals("duplicate", constraint.getExtraProp(String.class, "message").orElse(null));
    assertFalse(constraint.getExtraProp(Integer.class, "owner").isPresent());

    var copied = constraint.deepCopy();
    copied.getFields().add("tenantId");
    copied.setExtraProp("owner", "copy");

    assertEquals(Arrays.asList("projectId", "code"), constraint.getFields());
    assertEquals("runtime", constraint.getExtraProp(String.class, "owner").orElse(null));
    assertEquals("copy", copied.getExtraProp(String.class, "owner").orElse(null));

    constraint.setFields(null);
    constraint.setExtraProps(null);
    assertEquals(Collections.emptyList(), constraint.getFields());
    assertTrue(constraint.getExtraProps().isEmpty());
  }

  @Test
  void immutableMetaIncludesUniqueConstraintsAndFieldUniquenessScope() {
    var field = new DataModelFieldMeta();
    field.setName("code");
    field.setType("String");
    field.setUniquenessScope(UniquenessScope.ALL_RECORDS);

    var meta = new DataModelMeta();
    meta.setName("Model");
    meta.setFields(new ArrayList<DataModelFieldMeta>(Collections.singletonList(field)));
    meta.setUniqueConstraints(new ArrayList<DataModelUniqueConstraintMeta>(
        Collections.singletonList(uniqueConstraint("project_code", "projectId", "code"))));

    var immutable = new ImmutableDataModelMeta(meta);

    assertEquals(1, immutable.getUniqueConstraints().size());
    assertEquals("project_code", immutable.getUniqueConstraints().get(0).getName());
    assertEquals(UniquenessScope.ALL_RECORDS, immutable.getFields().get(0).getUniquenessScope());
    assertThrows(UnsupportedOperationException.class,
        () -> immutable.getUniqueConstraints().add(uniqueConstraint("tenant_code", "tenantId", "code")));
    assertThrows(UnsupportedOperationException.class,
        () -> immutable.getUniqueConstraints().get(0).setName("changed"));
    assertThrows(UnsupportedOperationException.class,
        () -> immutable.getFields().get(0).setUniquenessScope(UniquenessScope.ACTIVE_RECORDS));
  }

  @Test
  void dataModelMetaSupportsNullableFieldsAndGetters() {
    var meta = new DataModelMeta();
    meta.setName("n");
    meta.setNamespace(null);
    meta.setLabel(null);
    meta.setDescription(null);
    meta.setRawName(null);
    meta.setDataStation(null);
    meta.setPrimaryKeyGenerator(null);

    assertEquals("n", meta.getFullName());
    assertNull(meta.getLabel());
    assertNull(meta.getDescription());
    assertNull(meta.getRawName());
    assertNull(meta.getDataStation());
    assertNull(meta.getPrimaryKeyGenerator());
  }

  private static DataModelUniqueConstraintMeta uniqueConstraint(String name, String... fields) {
    var constraint = new DataModelUniqueConstraintMeta();
    constraint.setName(name);
    constraint.setFields(new ArrayList<String>(Arrays.asList(fields)));
    return constraint;
  }
}
