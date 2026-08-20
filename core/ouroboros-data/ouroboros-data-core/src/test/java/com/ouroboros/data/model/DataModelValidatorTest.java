package com.ouroboros.data.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.adapter.DataAdapter;
import com.ouroboros.data.exception.ModelMetadataException;
import com.ouroboros.data.station.DataStation;

class DataModelValidatorTest {

  @Test
  void refreshTimeValidatorRejectsMissingConstraintField() {
    DataModelMeta meta = baseMeta();
    meta.setUniqueConstraints(Collections.singletonList(unique("missing_field", "missing")));

    assertThrows(ModelMetadataException.class, () -> createProxy(meta));
  }

  @Test
  void refreshTimeValidatorRejectsEmptyConstraintFields() {
    DataModelMeta meta = baseMeta();
    DataModelUniqueConstraintMeta constraint = new DataModelUniqueConstraintMeta();
    constraint.setName("empty_fields");
    constraint.setFields(Collections.emptyList());
    meta.setUniqueConstraints(Collections.singletonList(constraint));

    assertThrows(ModelMetadataException.class, () -> createProxy(meta));
  }

  @Test
  void refreshTimeValidatorRejectsDuplicateConstraintFields() {
    DataModelMeta meta = baseMeta();
    meta.setUniqueConstraints(Collections.singletonList(unique("duplicate_fields", "code", "CODE")));

    assertThrows(ModelMetadataException.class, () -> createProxy(meta));
  }

  @Test
  void refreshTimeValidatorRejectsDuplicateExplicitLogicalNames() {
    DataModelMeta meta = baseMeta();
    meta.setUniqueConstraints(Arrays.asList(
        unique("same_name", "projectId", "code"),
        unique(" same_NAME ", "tenantId", "code")
    ));

    assertThrows(ModelMetadataException.class, () -> createProxy(meta));
  }

  @Test
  void refreshTimeValidatorRejectsDuplicateAnonymousStableIds() {
    DataModelMeta meta = baseMeta();
    meta.setUniqueConstraints(Arrays.asList(
        unique("", "projectId", "code"),
        unique(null, "projectId", "code")
    ));

    assertThrows(ModelMetadataException.class, () -> createProxy(meta));
  }

  @Test
  void refreshTimeValidatorRejectsFieldLevelUniqueAndModelLevelSingleFieldDuplicate() {
    DataModelMeta meta = baseMeta();
    meta.getFields().stream()
        .filter(field -> "code".equals(field.getName()))
        .findFirst()
        .orElseThrow(AssertionError::new)
        .setIsUnique(true);
    meta.setUniqueConstraints(Collections.singletonList(unique("code_constraint", "code")));

    assertThrows(ModelMetadataException.class, () -> createProxy(meta));
  }

  @Test
  void rawModelPluginWrappingDoesNotRunRefreshTimeValidators() {
    DataModelMeta meta = baseMeta();
    meta.setUniqueConstraints(Collections.singletonList(unique("missing_field", "missing")));
    DataModel rawModel = new DefaultDataModel(meta, station());

    assertDoesNotThrow(() -> new EnhancedDataModelProxy(rawModel, Collections.emptyList()));
  }

  @Test
  void validUniqueConstraintDefinitionsPassRefreshTimeValidation() {
    DataModelMeta meta = baseMeta();
    meta.setUniqueConstraints(Arrays.asList(
        unique("project_code", "projectId", "code"),
        unique("tenant_code", "tenantId", "code")
    ));

    assertDoesNotThrow(() -> createProxy(meta));
  }

  private EnhancedDataModelProxy createProxy(DataModelMeta meta) {
    DataStation<?> station = station();
    return new EnhancedDataModelProxy(meta, decoratedMeta -> new DefaultDataModel(decoratedMeta, station));
  }

  private DataStation<?> station() {
    DataStation<?> station = mock(DataStation.class);
    when(station.getDataAdapter()).thenReturn(mock(DataAdapter.class));
    return station;
  }

  private DataModelMeta baseMeta() {
    DataModelMeta meta = new DataModelMeta();
    meta.setName("ValidatorModel");
    meta.setRawName("validator_model");
    meta.setFields(Arrays.asList(
        field("id"),
        field("projectId"),
        field("tenantId"),
        field("code")
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

  private DataModelUniqueConstraintMeta unique(String name, String... fields) {
    DataModelUniqueConstraintMeta constraint = new DataModelUniqueConstraintMeta();
    constraint.setName(name);
    constraint.setFields(Arrays.asList(fields));
    return constraint;
  }
}
