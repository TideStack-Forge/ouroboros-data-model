package com.ouroboros.data.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.adapter.DataAdapter;
import com.ouroboros.data.station.DataStation;

class DataModelUniqueConstraintsTest {

  @Test
  void resolveProjectsFieldLevelUniquesAsIndependentConstraints() {
    DataModelMeta meta = baseMeta();
    field(meta, "code").setIsUnique(true);
    field(meta, "name").setIsUnique(true);

    List<DataModelUniqueConstraint> constraints = DataModelUniqueConstraints.resolve(createModel(meta));

    assertEquals(2, constraints.size());
    assertEquals(DataModelUniqueConstraint.Source.FIELD, constraints.get(0).getSource());
    assertEquals(Collections.singletonList("code"), constraints.get(0).getFields());
    assertEquals(DataModelUniqueConstraint.Source.FIELD, constraints.get(1).getSource());
    assertEquals(Collections.singletonList("name"), constraints.get(1).getFields());
  }

  @Test
  void resolveKeepsModelLevelCompoundConstraintAsSingleAndConstraint() {
    DataModelMeta meta = baseMeta();
    meta.setUniqueConstraints(Collections.singletonList(unique("project_code", "projectId", "code")));

    List<DataModelUniqueConstraint> constraints = DataModelUniqueConstraints.resolve(createModel(meta));

    assertEquals(1, constraints.size());
    assertEquals("project_code", constraints.get(0).getName());
    assertEquals(DataModelUniqueConstraint.Source.MODEL, constraints.get(0).getSource());
    assertEquals(Arrays.asList("projectId", "code"), constraints.get(0).getFields());
  }

  @Test
  void resolveUsesFieldScopeBeforeModelDefaultScope() {
    DataModelMeta meta = baseMeta();
    meta.setExtraProp(UniquenessScope.EXTRA_PROP_NAME, UniquenessScope.ACTIVE_RECORDS);
    field(meta, "code").setIsUnique(true);
    field(meta, "name").setIsUnique(true);
    field(meta, "name").setUniquenessScope(UniquenessScope.ALL_RECORDS);

    List<DataModelUniqueConstraint> constraints = DataModelUniqueConstraints.resolve(createModel(meta));

    assertEquals(UniquenessScope.ACTIVE_RECORDS, constraints.get(0).getScope());
    assertEquals(UniquenessScope.ALL_RECORDS, constraints.get(1).getScope());
  }

  @Test
  void resolveUsesModelDefaultAndExplicitModelConstraintScope() {
    DataModelMeta meta = baseMeta();
    meta.setExtraProp(UniquenessScope.EXTRA_PROP_NAME, UniquenessScope.ACTIVE_RECORDS);
    DataModelUniqueConstraintMeta defaultScope = unique("project_code", "projectId", "code");
    DataModelUniqueConstraintMeta explicitScope = unique("tenant_code", "tenantId", "code");
    explicitScope.setScope(UniquenessScope.ALL_RECORDS);
    meta.setUniqueConstraints(Arrays.asList(defaultScope, explicitScope));

    List<DataModelUniqueConstraint> constraints = DataModelUniqueConstraints.resolve(createModel(meta));

    assertEquals(UniquenessScope.ACTIVE_RECORDS, constraints.get(0).getScope());
    assertEquals(UniquenessScope.ALL_RECORDS, constraints.get(1).getScope());
  }

  @Test
  void resolveBuildsStableLogicalIdForAnonymousConstraint() {
    DataModelMeta meta = baseMeta();
    meta.setExtraProp(UniquenessScope.EXTRA_PROP_NAME, UniquenessScope.ACTIVE_RECORDS);
    meta.setUniqueConstraints(Collections.singletonList(unique("", "projectId", "code")));

    List<DataModelUniqueConstraint> constraints = DataModelUniqueConstraints.resolve(createModel(meta));

    assertEquals("model:projectid,code:active_records", constraints.get(0).getName());
  }

  @Test
  void resolveKeepsModelLevelSingleFieldConstraintAsModelSource() {
    DataModelMeta meta = baseMeta();
    meta.setUniqueConstraints(Collections.singletonList(unique("model_code", "code")));

    List<DataModelUniqueConstraint> constraints = DataModelUniqueConstraints.resolve(createModel(meta));

    assertEquals(1, constraints.size());
    assertEquals(DataModelUniqueConstraint.Source.MODEL, constraints.get(0).getSource());
    assertEquals(Collections.singletonList("code"), constraints.get(0).getFields());
  }

  @Test
  void resolveDoesNotProjectPrimaryKeyFieldLevelUnique() {
    DataModelMeta meta = baseMeta();
    field(meta, "id").setIsUnique(true);

    List<DataModelUniqueConstraint> constraints = DataModelUniqueConstraints.resolve(createModel(meta));

    assertEquals(0, constraints.size());
  }

  @Test
  void resolveReturnsImmutableConstraintCopies() {
    DataModelMeta meta = baseMeta();
    field(meta, "code").setIsUnique(true);

    List<DataModelUniqueConstraint> constraints = DataModelUniqueConstraints.resolve(createModel(meta));

    assertThrows(UnsupportedOperationException.class, constraints::clear);
    assertThrows(UnsupportedOperationException.class, () -> constraints.get(0).getFields().add("name"));
  }

  private DataModel createModel(DataModelMeta meta) {
    DataStation<?> station = mock(DataStation.class);
    when(station.getDataAdapter()).thenReturn(mock(DataAdapter.class));
    return new EnhancedDataModelProxy(meta, decoratedMeta -> new DefaultDataModel(decoratedMeta, station));
  }

  private DataModelMeta baseMeta() {
    DataModelMeta meta = new DataModelMeta();
    meta.setName("UniqueResolverModel");
    meta.setRawName("unique_resolver_model");
    meta.setFields(Arrays.asList(
        field("id"),
        field("projectId"),
        field("tenantId"),
        field("code"),
        field("name")
    ));
    meta.setPrimaryKeys(Collections.singletonList("id"));
    return meta;
  }

  private DataModelFieldMeta field(String name) {
    DataModelFieldMeta field = new DataModelFieldMeta();
    field.setName(name);
    field.setRawName(name);
    return field;
  }

  private DataModelFieldMeta field(DataModelMeta meta, String name) {
    return meta.getFields().stream()
        .filter(field -> name.equals(field.getName()))
        .findFirst()
        .orElseThrow(AssertionError::new);
  }

  private DataModelUniqueConstraintMeta unique(String name, String... fields) {
    DataModelUniqueConstraintMeta constraint = new DataModelUniqueConstraintMeta();
    constraint.setName(name);
    constraint.setFields(Arrays.asList(fields));
    return constraint;
  }
}
