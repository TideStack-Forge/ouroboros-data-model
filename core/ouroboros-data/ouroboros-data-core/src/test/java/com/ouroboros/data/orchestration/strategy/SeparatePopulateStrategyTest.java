package com.ouroboros.data.orchestration.strategy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.orchestration.step.PopulateStep;
import com.ouroboros.data.orchestration.step.QueryStep;

/**
 * SeparatePopulateStrategy 单元测试
 *
 * @author Claude Code
 */
class SeparatePopulateStrategyTest {

  @Test
  void testCreateSteps() {
    // Given: PopulateField
    DataModel mockDataModel = mock(DataModel.class);

    PopulateField field = new PopulateField(
        "department",
        mockDataModel,
        "departmentId",
        "id",
        null,
        null
    );

    // When: 创建 Steps
    SeparatePopulateStrategy strategy = new SeparatePopulateStrategy();
    List<QueryStep> steps = strategy.createSteps(field, "main");

    // Then: 返回一个 PopulateStep
    assertEquals(1, steps.size());
    assertTrue(steps.get(0) instanceof PopulateStep);
    assertEquals("populate_department", steps.get(0).getName());
  }
}
