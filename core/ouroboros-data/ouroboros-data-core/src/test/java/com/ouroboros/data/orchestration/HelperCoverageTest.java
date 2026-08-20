package com.ouroboros.data.orchestration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.ValueType;
import com.ouroboros.data.query.DefaultProjectionFieldSupport;

class HelperCoverageTest {

  @Test
  void joinCapabilityResultFactoriesExposeExpectedState() {
    var joinable = JoinCapabilityResult.joinable();
    var notJoinable = JoinCapabilityResult.notJoinable("cross station");

    assertTrue(joinable.isJoinable());
    assertEquals(JoinStrategy.NATIVE, joinable.getStrategy());
    assertEquals(null, joinable.getReason());

    assertFalse(notJoinable.isJoinable());
    assertEquals(JoinStrategy.SEPARATE, notJoinable.getStrategy());
    assertEquals("cross station", notJoinable.getReason());
  }

  @Test
  void relationRewriteMetadataAttachesReadsAndRemovesPath() {
    var statement = new QueryStatement(new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER));

    assertFalse(RelationRewriteMetadata.getRelationFieldPath(null).isPresent());
    assertFalse(RelationRewriteMetadata.getRelationFieldPath(statement).isPresent());

    var attached = RelationRewriteMetadata.attachRelationFieldPath(statement, "order.customer");
    assertEquals("order.customer", RelationRewriteMetadata.getRelationFieldPath(attached).get());

    var removed = RelationRewriteMetadata.attachRelationFieldPath(attached, "");
    assertFalse(RelationRewriteMetadata.getRelationFieldPath(removed).isPresent());
  }

  @Test
  void defaultProjectionFieldSupportHandlesDirectAndNestedFields() {
    var directPhysicalField = mock(DataModelField.class);
    var physicalType = mock(ValueType.class);
    when(directPhysicalField.getValueType()).thenReturn((ValueType) physicalType);
    when(physicalType.isPhysical()).thenReturn(true);
    when(directPhysicalField.getName()).thenReturn("name");
    when(directPhysicalField.getRawName()).thenReturn("name");

    var nestedField = mock(DataModelField.class);
    when(nestedField.getValueType()).thenReturn((ValueType) physicalType);
    when(nestedField.getName()).thenReturn("user.name");
    when(nestedField.getRawName()).thenReturn("name");

    var virtualType = mock(ValueType.class);
    var virtualField = mock(DataModelField.class);
    when(virtualField.getValueType()).thenReturn((ValueType) virtualType);
    when(virtualType.isPhysical()).thenReturn(false);
    when(virtualField.getName()).thenReturn("name");
    when(virtualField.getRawName()).thenReturn("name");

    assertTrue(DefaultProjectionFieldSupport.isDirectFieldName("name"));
    assertFalse(DefaultProjectionFieldSupport.isDirectFieldName("user.name"));
    assertFalse(DefaultProjectionFieldSupport.isDirectFieldName(null));

    assertTrue(DefaultProjectionFieldSupport.isDirectDefaultProjectionField(directPhysicalField));
    assertFalse(DefaultProjectionFieldSupport.isDirectDefaultProjectionField(nestedField));
    assertFalse(DefaultProjectionFieldSupport.isDirectDefaultProjectionField(virtualField));
    assertFalse(DefaultProjectionFieldSupport.isDirectDefaultProjectionField(null));

    assertTrue(DefaultProjectionFieldSupport.isDirectDefaultProjectionPath(com.ouroboros.data.dsl.ModelFieldPath.of(String.class, directPhysicalField)));
    assertFalse(DefaultProjectionFieldSupport.isDirectDefaultProjectionPath(com.ouroboros.data.dsl.ModelFieldPath.of(String.class, nestedField)));
    assertSame(false, DefaultProjectionFieldSupport.isDirectDefaultProjectionPath(null));
  }
}
