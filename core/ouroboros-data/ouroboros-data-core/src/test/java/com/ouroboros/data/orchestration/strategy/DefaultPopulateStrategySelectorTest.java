package com.ouroboros.data.orchestration.strategy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.orchestration.RelationType;
import com.ouroboros.data.station.DataStation;

/**
 * DefaultPopulateStrategySelector 单元测试
 */
class DefaultPopulateStrategySelectorTest {

  private final DefaultPopulateStrategySelector selector = new DefaultPopulateStrategySelector();

  private DataModel mockModel(String stationName) {
    DataModel model = mock(DataModel.class);
    DataStation station = mock(DataStation.class);
    when(station.getName()).thenReturn(stationName);
    when(model.getDataStation()).thenReturn(station);
    return model;
  }

  @Test
  void testCrossSourceReturnsSeparate() {
    DataModel sourceModel = mockModel("stationA");
    DataModel relatedModel = mockModel("stationB");
    PopulateField field = new PopulateField(
        "department", relatedModel, "departmentId", "id",
        null, null, sourceModel, RelationType.TO_ONE);

    PopulateStrategy result = selector.select(field);
    assertInstanceOf(SeparatePopulateStrategy.class, result);
  }

  @Test
  void testHasChildrenReturnsSeparate() {
    DataModel sourceModel = mockModel("stationA");
    DataModel relatedModel = mockModel("stationA");
    PopulateField child = new PopulateField("sub", relatedModel, "subId", "id", null);
    PopulateField field = new PopulateField(
        "department", relatedModel, "departmentId", "id",
        null, Collections.singletonList(child), sourceModel, RelationType.TO_ONE);

    PopulateStrategy result = selector.select(field);
    assertInstanceOf(SeparatePopulateStrategy.class, result);
  }

  @Test
  void testNestedPopulateConfigReturnsSeparate() {
    DataModel sourceModel = mockModel("stationA");
    DataModel relatedModel = mockModel("stationA");
    PopulateField field = new PopulateField(
        "user", relatedModel, "userId", "id",
        null, null, sourceModel, RelationType.TO_ONE,
        null, null, null, "department");

    PopulateStrategy result = selector.select(field);
    assertInstanceOf(SeparatePopulateStrategy.class, result);
  }

  @Test
  void testToManyReturnsSeparate() {
    DataModel sourceModel = mockModel("stationA");
    DataModel relatedModel = mockModel("stationA");
    PopulateField field = new PopulateField(
        "orders", relatedModel, "customerId", "id",
        null, null, sourceModel, RelationType.TO_MANY);

    PopulateStrategy result = selector.select(field);
    assertInstanceOf(SeparatePopulateStrategy.class, result);
  }

  @Test
  void testSameSourceToOneReturnsJoin() {
    DataModel sourceModel = mockModel("stationA");
    DataModel relatedModel = mockModel("stationA");
    PopulateField field = new PopulateField(
        "department", relatedModel, "departmentId", "id",
        null, null, sourceModel, RelationType.TO_ONE);

    PopulateStrategy result = selector.select(field);
    assertInstanceOf(JoinPopulateStrategy.class, result);
  }

  @Test
  void testNullMetadataFallsBackToSeparate() {
    DataModel relatedModel = mockModel("stationA");

    // sourceModel is null
    PopulateField field1 = new PopulateField(
        "department", relatedModel, "departmentId", "id",
        null, null, null, RelationType.TO_ONE);
    assertInstanceOf(SeparatePopulateStrategy.class, selector.select(field1));

    // relationType is null
    DataModel sourceModel = mockModel("stationA");
    PopulateField field2 = new PopulateField(
        "department", relatedModel, "departmentId", "id",
        null, null, sourceModel, null);
    assertInstanceOf(SeparatePopulateStrategy.class, selector.select(field2));
  }
}
