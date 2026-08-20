package com.ouroboros.data.analyze;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.exception.NormalizeException;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.ValueType;
import com.ouroboros.data.model.valuetypes.RelatedValue;

class FieldPathValidatorTest {

  @Test
  void validPathShouldPass() {
    var root = mock(DataModel.class);
    when(root.getName()).thenReturn("Employee");

    var related = mock(DataModel.class);
    when(related.getName()).thenReturn("Department");

    var relationValue = mock(RelatedValue.class);
    when(relationValue.getReferenceModel()).thenReturn(Optional.of(related));

    var relationField = mock(DataModelField.class);
    when(relationField.getName()).thenReturn("department");
    when(relationField.getType()).thenReturn("Model");
    when(relationField.getValueType()).thenReturn((ValueType) relationValue);

    var terminal = mock(DataModelField.class);
    when(terminal.getName()).thenReturn("name");
    when(terminal.getType()).thenReturn("String");

    when(root.getField("department")).thenReturn(Optional.of(relationField));
    when(related.getField("name")).thenReturn(Optional.of(terminal));

    assertDoesNotThrow(() -> FieldPathValidator.validateFieldPath(SExpression.field("department", "name"), root, "WHERE"));
  }

  @Test
  void missingFieldShouldReportClauseAndPath() {
    var root = mock(DataModel.class);
    when(root.getName()).thenReturn("Employee");
    when(root.getField("missing")).thenReturn(Optional.empty());

    var ex = assertThrows(NormalizeException.class,
        () -> FieldPathValidator.validateFieldPath(SExpression.field("missing"), root, "HAVING"));

    assertTrue(ex.getMessage().contains("不存在字段"));
    assertTrue(ex.getMessage().contains("HAVING 子句"));
    assertTrue(ex.getMessage().contains("missing"));
  }

  @Test
  void intermediateNonRelationShouldFail() {
    var root = mock(DataModel.class);
    when(root.getName()).thenReturn("Employee");

    var simple = mock(DataModelField.class);
    when(simple.getName()).thenReturn("name");
    when(simple.getType()).thenReturn("String");
    when(root.getField("name")).thenReturn(Optional.of(simple));

    var ex = assertThrows(NormalizeException.class,
        () -> FieldPathValidator.validateFieldPath(SExpression.field("name", "first"), root, null));

    assertTrue(ex.getMessage().contains("不是关联字段"));
    assertTrue(ex.getMessage().contains("完整路径"));
    assertTrue(ex.getMessage().contains("name.first"));
  }

  @Test
  void relationWithoutReferenceModelShouldFallbackToCurrentModelAndFailIfNextMissing() {
    var root = mock(DataModel.class);
    when(root.getName()).thenReturn("Employee");

    var relationValue = mock(RelatedValue.class);
    when(relationValue.getReferenceModel()).thenReturn(Optional.empty());

    var relationField = mock(DataModelField.class);
    when(relationField.getName()).thenReturn("department");
    when(relationField.getType()).thenReturn("Collection");
    when(relationField.getValueType()).thenReturn((ValueType) relationValue);

    when(root.getField("department")).thenReturn(Optional.of(relationField));
    when(root.getField("id")).thenReturn(Optional.empty());

    var ex = assertThrows(NormalizeException.class,
        () -> FieldPathValidator.validateFieldPath(SExpression.field("department", "id"), root, "WHERE"));

    assertTrue(ex.getMessage().contains("WHERE 子句"));
  }
}
