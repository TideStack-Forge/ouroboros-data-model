package com.ouroboros.data.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.model.ValueType;
import com.ouroboros.data.model.valuetypes.ModelValue;
import com.ouroboros.data.model.valuetypes.StringValue;

class FieldPathResolverTest {

  @Test
  void resolveShouldReturnTerminalFieldForLegalMultiSegmentPath() {
    DataModel employeeModel = mock(DataModel.class);
    when(employeeModel.getName()).thenReturn("Employee");

    DataModel departmentModel = mock(DataModel.class);
    when(departmentModel.getName()).thenReturn("Department");

    ModelValue departmentValue = mock(ModelValue.class);
    when(departmentValue.getReferenceModel()).thenReturn(Optional.of(departmentModel));

    DataModelField departmentField = mock(DataModelField.class);
    when(departmentField.getName()).thenReturn("department");
    when(departmentField.getType()).thenReturn("Model");
    when(departmentField.getValueType()).thenReturn((ValueType) departmentValue);

    DataModelField nameField = mock(DataModelField.class);
    when(nameField.getName()).thenReturn("name");
    when(nameField.getType()).thenReturn("String");
    when(nameField.getValueType()).thenReturn((ValueType) new StringValue());

    when(employeeModel.getFields()).thenReturn(Collections.singletonList(departmentField));
    when(employeeModel.getField("department")).thenReturn(Optional.of(departmentField));
    when(departmentModel.getFields()).thenReturn(Collections.singletonList(nameField));
    when(departmentModel.getField("name")).thenReturn(Optional.of(nameField));

    Optional<FieldPathResolver.ResolvedFieldPath> resolved = FieldPathResolver.resolve(
        SExpression.field("department", "name"), employeeModel);

    assertTrue(resolved.isPresent());
    assertEquals("department.name", resolved.get().getFullPath());
    assertSame(departmentModel, resolved.get().getTerminalSourceModel());
    assertSame(nameField, resolved.get().getTerminalField());
    assertFalse(resolved.get().getTerminalRelatedModel().isPresent());
  }

  @Test
  void resolveShouldReturnTerminalRelatedModelForRelationPath() {
    DataModel employeeModel = mock(DataModel.class);
    when(employeeModel.getName()).thenReturn("Employee");

    DataModel departmentModel = mock(DataModel.class);
    when(departmentModel.getName()).thenReturn("Department");

    ModelValue departmentValue = mock(ModelValue.class);
    when(departmentValue.getReferenceModel()).thenReturn(Optional.of(departmentModel));

    DataModelField departmentField = mock(DataModelField.class);
    when(departmentField.getName()).thenReturn("department");
    when(departmentField.getType()).thenReturn("Model");
    when(departmentField.getValueType()).thenReturn((ValueType) departmentValue);

    when(employeeModel.getFields()).thenReturn(Collections.singletonList(departmentField));
    when(employeeModel.getField("department")).thenReturn(Optional.of(departmentField));

    Optional<FieldPathResolver.ResolvedFieldPath> resolved = FieldPathResolver.resolve(
        SExpression.field("department"), employeeModel);

    assertTrue(resolved.isPresent());
    assertSame(employeeModel, resolved.get().getTerminalSourceModel());
    assertSame(departmentField, resolved.get().getTerminalField());
    assertEquals(Optional.of(departmentModel), resolved.get().getTerminalRelatedModel());
    assertEquals(Arrays.asList("department"), resolved.get().getPathSegments());
  }

  @Test
  void resolveShouldReturnEmptyForInvalidIntermediateNonRelationField() {
    DataModel employeeModel = mock(DataModel.class);
    when(employeeModel.getName()).thenReturn("Employee");

    DataModelField nameField = mock(DataModelField.class);
    when(nameField.getName()).thenReturn("name");
    when(nameField.getType()).thenReturn("String");
    when(nameField.getValueType()).thenReturn((ValueType) new StringValue());

    when(employeeModel.getFields()).thenReturn(Collections.singletonList(nameField));
    when(employeeModel.getField("name")).thenReturn(Optional.of(nameField));

    Optional<FieldPathResolver.ResolvedFieldPath> resolved = FieldPathResolver.resolve(
        SExpression.field("name", "first"), employeeModel);

    assertFalse(resolved.isPresent());
  }

  @Test
  void resolveWithStatementShouldTreatRootAliasBeforeRelationField() {
    DataModel employeeModel = mock(DataModel.class);
    when(employeeModel.getName()).thenReturn("Employee");

    DataModel departmentModel = mock(DataModel.class);
    when(departmentModel.getName()).thenReturn("Department");

    ModelValue departmentValue = mock(ModelValue.class);
    when(departmentValue.getReferenceModel()).thenReturn(Optional.of(departmentModel));

    DataModelField departmentField = mock(DataModelField.class);
    when(departmentField.getName()).thenReturn("department");
    when(departmentField.getType()).thenReturn("Model");
    when(departmentField.getValueType()).thenReturn((ValueType) departmentValue);

    DataModelField employeeNameField = mock(DataModelField.class);
    when(employeeNameField.getName()).thenReturn("name");
    when(employeeNameField.getType()).thenReturn("String");
    when(employeeNameField.getValueType()).thenReturn((ValueType) new StringValue());

    DataModelField departmentNameField = mock(DataModelField.class);
    when(departmentNameField.getName()).thenReturn("name");
    when(departmentNameField.getType()).thenReturn("String");
    when(departmentNameField.getValueType()).thenReturn((ValueType) new StringValue());

    when(employeeModel.getField("department")).thenReturn(Optional.of(departmentField));
    when(employeeModel.getField("name")).thenReturn(Optional.of(employeeNameField));
    when(departmentModel.getField("name")).thenReturn(Optional.of(departmentNameField));

    QueryStatement statement = QueryStatement.builder()
        .from("employee_table", "department")
        .build();

    Optional<FieldPathResolver.ResolvedFieldPath> resolved = FieldPathResolver.resolve(
        SExpression.field("department", "name"), employeeModel, statement);

    assertTrue(resolved.isPresent());
    assertEquals(Collections.singletonList("name"), resolved.get().getPathSegments());
    assertSame(employeeModel, resolved.get().getTerminalSourceModel());
    assertSame(employeeNameField, resolved.get().getTerminalField());
  }

  @Test
  void resolveWithStatementShouldTreatRootTableNameBeforeRelationField() {
    DataModel employeeModel = mock(DataModel.class);
    when(employeeModel.getName()).thenReturn("Employee");

    DataModel departmentModel = mock(DataModel.class);
    when(departmentModel.getName()).thenReturn("Department");

    ModelValue departmentValue = mock(ModelValue.class);
    when(departmentValue.getReferenceModel()).thenReturn(Optional.of(departmentModel));

    DataModelField departmentField = mock(DataModelField.class);
    when(departmentField.getName()).thenReturn("department");
    when(departmentField.getType()).thenReturn("Model");
    when(departmentField.getValueType()).thenReturn((ValueType) departmentValue);

    DataModelField departmentNameField = mock(DataModelField.class);
    when(departmentNameField.getName()).thenReturn("name");
    when(departmentNameField.getType()).thenReturn("String");
    when(departmentNameField.getValueType()).thenReturn((ValueType) new StringValue());

    when(employeeModel.getField("department")).thenReturn(Optional.of(departmentField));
    when(departmentModel.getField("name")).thenReturn(Optional.of(departmentNameField));

    QueryStatement statement = QueryStatement.builder()
        .from("employee_table", "e")
        .build();

    Optional<FieldPathResolver.ResolvedFieldPath> resolved = FieldPathResolver.resolve(
        SExpression.field("employee_table", "department", "name"), employeeModel, statement);

    assertTrue(resolved.isPresent());
    assertEquals(Arrays.asList("department", "name"), resolved.get().getPathSegments());
    assertSame(departmentModel, resolved.get().getTerminalSourceModel());
    assertSame(departmentNameField, resolved.get().getTerminalField());
  }
}
