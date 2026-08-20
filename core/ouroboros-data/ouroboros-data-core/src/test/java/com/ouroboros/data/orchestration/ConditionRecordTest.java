package com.ouroboros.data.orchestration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.model.DataModel;

/**
 * CrossSourceCondition & SameSourceCondition 单元测试
 */
class ConditionRecordTest {

  @SuppressWarnings("unchecked")
  @Test
  void testCrossSourceConditionAccessors() {
    // Given
    DataModel model = mock(DataModel.class);
    SExpression<Boolean> condition = mock(SExpression.class);

    // When
    CrossSourceCondition csc = new CrossSourceCondition(
        "user.department", "user", model, condition, "departmentId", "id", false);

    // Then
    assertEquals("user.department", csc.fieldPath());
    assertEquals("user", csc.sourceFieldPath());
    assertSame(model, csc.relatedModel());
    assertSame(condition, csc.condition());
    assertEquals("departmentId", csc.localKeyName());
    assertFalse(csc.implicitFieldPath());
  }

  @SuppressWarnings("unchecked")
  @Test
  void testSameSourceConditionAccessors() {
    // Given
    DataModel model = mock(DataModel.class);
    SExpression<Boolean> condition = mock(SExpression.class);

    // When
    SameSourceCondition ssc = new SameSourceCondition(
        "user.department", "user", model, condition, RelationType.TO_ONE, false, "departmentId", "id");

    // Then
    assertEquals("user.department", ssc.fieldPath());
    assertEquals("user", ssc.sourceFieldPath());
    assertSame(model, ssc.relatedModel());
    assertSame(condition, ssc.condition());
    assertEquals(RelationType.TO_ONE, ssc.relationType());
    assertFalse(ssc.requiresLeftJoin());
  }
}
