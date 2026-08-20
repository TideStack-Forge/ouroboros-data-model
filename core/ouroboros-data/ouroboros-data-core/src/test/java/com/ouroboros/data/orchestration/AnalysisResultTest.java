package com.ouroboros.data.orchestration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.model.DataModel;

/**
 * AnalysisResult 单元测试
 */
class AnalysisResultTest {

  @Test
  void testConstructorDefensiveCopy() {
    // Given: Mutable lists
    List<CrossSourceCondition> crossSource = new ArrayList<>();
    crossSource.add(new CrossSourceCondition("path", null, mock(DataModel.class), null, "localField", "id", false));
    List<SameSourceCondition> toOne = new ArrayList<>();
    List<SameSourceCondition> toMany = new ArrayList<>();

    AnalysisResult result = new AnalysisResult(crossSource, toOne, toMany);

    // When: Modify original list
    crossSource.clear();

    // Then: AnalysisResult's list is unchanged
    assertEquals(1, result.crossSourceConditions().size());
  }

  @Test
  void testAccessorsReturnUnmodifiableLists() {
    // Given
    AnalysisResult result = new AnalysisResult(
        Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

    // Then: All accessors return unmodifiable lists
    assertThrows(UnsupportedOperationException.class,
        () -> result.crossSourceConditions().add(null));
    assertThrows(UnsupportedOperationException.class,
        () -> result.sameSourceToOneConditions().add(null));
    assertThrows(UnsupportedOperationException.class,
        () -> result.sameSourceToManyConditions().add(null));
  }

  @Test
  void testHasConditionsTrue() {
    // Given: Non-empty cross-source list
    CrossSourceCondition csc = new CrossSourceCondition("path", null, mock(DataModel.class), null, "localField", "id", false);
    List<CrossSourceCondition> crossSource = Collections.singletonList(csc);
    AnalysisResult result = new AnalysisResult(
        crossSource, Collections.emptyList(), Collections.emptyList());

    // Then
    assertTrue(result.hasConditions());
  }

  @Test
  void testHasConditionsFalse() {
    // Given: All empty lists
    AnalysisResult result = new AnalysisResult(
        Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

    // Then
    assertFalse(result.hasConditions());
  }
}
