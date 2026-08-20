package com.ouroboros.data.transpile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelCenter;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.station.DataStation;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.Expressions;

@DisplayName("BaseTranspileContext Test")
class BaseTranspileContextTest {

  private Field dataModelMapField;
  private Object originalDataModelMap;

  @BeforeEach
  void setUp() throws Exception {
    dataModelMapField = DataModelCenter.class.getDeclaredField("dataModelMap");
    dataModelMapField.setAccessible(true);
    originalDataModelMap = dataModelMapField.get(null);
  }

  @AfterEach
  void tearDown() throws IllegalAccessException {
    dataModelMapField.set(null, originalDataModelMap);
  }

  @Test
  @DisplayName("应将同名不同实例的 DataStation 视为同站点")
  @SuppressWarnings("rawtypes")
  void shouldResolveRelatedModelWhenDataStationNamesMatchAcrossInstances() throws Exception {
    DataStation mainStation = mock(DataStation.class);
    when(mainStation.getName()).thenReturn("test");
    DataStation relatedStation = mock(DataStation.class);
    when(relatedStation.getName()).thenReturn("test");

    DataModel mainModel = mock(DataModel.class);
    doReturn(mainStation).when(mainModel).getDataStation();

    DataModelField departmentIdField = mockField("id", "id");
    DataModel relatedModel = mock(DataModel.class);
    doReturn(relatedStation).when(relatedModel).getDataStation();
    when(relatedModel.getRawName()).thenReturn("department");
    when(relatedModel.getField("id")).thenReturn(Optional.of(departmentIdField));

    setDataModelMap(Collections.singletonMap("Department", relatedModel));

    BaseTranspileContext context = new BaseTranspileContext(mockMainTable(mainModel), "u");

    Optional<FieldSource> table = context.resolveTable("Department");
    Optional<Path<?>> field = context.resolve("Department", "id");

    assertTrue(table.isPresent(), "same-name stations should allow related model resolution");
    assertTrue(field.isPresent(), "same-name stations should allow related field resolution");
    assertEquals("id", field.get().getMetadata().getName());
  }

  @Test
  @DisplayName("不同 DataStation 名称不应被误判为同站点")
  @SuppressWarnings("rawtypes")
  void shouldNotResolveRelatedModelWhenDataStationNamesDiffer() throws Exception {
    DataStation mainStation = mock(DataStation.class);
    when(mainStation.getName()).thenReturn("test");
    DataStation relatedStation = mock(DataStation.class);
    when(relatedStation.getName()).thenReturn("archive");

    DataModel mainModel = mock(DataModel.class);
    doReturn(mainStation).when(mainModel).getDataStation();

    DataModel relatedModel = mock(DataModel.class);
    doReturn(relatedStation).when(relatedModel).getDataStation();

    setDataModelMap(Collections.singletonMap("Department", relatedModel));

    BaseTranspileContext context = new BaseTranspileContext(mockMainTable(mainModel), "u");

    assertFalse(context.resolveTable("Department").isPresent());
    assertFalse(context.resolve("Department", "id").isPresent());
  }

  private void setDataModelMap(Map<String, DataModel> models) throws IllegalAccessException {
    Map<String, DataModel> modelMap = new TreeMap<String, DataModel>(String::compareToIgnoreCase);
    modelMap.putAll(models);
    dataModelMapField.set(null, modelMap);
  }

  private FieldSource mockMainTable(DataModel mainModel) {
    FieldSource mainTable = mock(FieldSource.class);
    doReturn(Optional.of(mainModel)).when(mainTable).getDataModel();
    doReturn("user").when(mainTable).getName();
    doReturn(Expressions.path(Object.class, "user")).when(mainTable).getSelfPath();
    return mainTable;
  }

  private DataModelField mockField(String name, String rawName) {
    DataModelField field = mock(DataModelField.class);
    when(field.getName()).thenReturn(name);
    when(field.getRawName()).thenReturn(rawName);
    return field;
  }
}
