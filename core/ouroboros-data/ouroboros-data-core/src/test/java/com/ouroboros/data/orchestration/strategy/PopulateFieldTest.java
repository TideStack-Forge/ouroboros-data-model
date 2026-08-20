package com.ouroboros.data.orchestration.strategy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.model.DataModel;

/**
 * PopulateField 单元测试
 */
class PopulateFieldTest {

  @Test
  void testFullConstructor() {
    // Given
    DataModel model = mock(DataModel.class);
    PopulateField child = new PopulateField("child", model, "childId", "id", null, null);

    // When
    PopulateField field = new PopulateField(
        "department", model, "departmentId", "id",
        Arrays.asList("id", "name"), Collections.singletonList(child));

    // Then: All accessors return correct values
    assertEquals("department", field.fieldName());
    assertSame(model, field.relatedModel());
    assertEquals("departmentId", field.localForeignKey());
    assertEquals("id", field.remotePrimaryKey());
    assertEquals(2, field.selectFields().size());
    assertEquals(1, field.children().size());
  }

  @Test
  void testConvenienceConstructor() {
    // Given
    DataModel model = mock(DataModel.class);

    // When: Use 5-arg constructor (no children)
    PopulateField field = new PopulateField(
        "department", model, "departmentId", "id",
        Arrays.asList("id", "name"));

    // Then: children() returns empty list
    assertNotNull(field.children());
    assertTrue(field.children().isEmpty());
  }

  @Test
  void testNullNormalization() {
    // Given/When: null selectFields and null children
    DataModel model = mock(DataModel.class);
    PopulateField field = new PopulateField(
        "department", model, "departmentId", "id", null, null);

    // Then: Both return empty list, not null
    assertNotNull(field.selectFields());
    assertTrue(field.selectFields().isEmpty());
    assertNotNull(field.children());
    assertTrue(field.children().isEmpty());
  }
}
