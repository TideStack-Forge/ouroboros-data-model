package com.ouroboros.data.model.valuetypes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.PopulateContext;
import com.ouroboros.data.model.QueryPatcher;
import com.ouroboros.data.model.ValueType;
import com.ouroboros.data.station.DataStation;

class ModelValueTest {

  @Test
  void getQueryPatchShouldUseSegmentedFieldParamsInJoinCondition() {
    DataStation<?> station = mock(DataStation.class);
    when(station.getName()).thenReturn("main");

    DataModel rootModel = mock(DataModel.class);
    DataModel referenceModel = mock(DataModel.class);
    doReturn(station).when(rootModel).getDataStation();
    doReturn(station).when(referenceModel).getDataStation();
    when(referenceModel.getFullName()).thenReturn("demo.department");

    ValueType<?> physicalType = mock(ValueType.class);
    when(physicalType.isPhysical()).thenReturn(true);

    DataModelField selectField = mock(DataModelField.class);
    when(selectField.getName()).thenReturn("name");
    when(selectField.getValueType()).thenReturn((ValueType) physicalType);
    when(referenceModel.getFields()).thenReturn(Collections.singletonList(selectField));

    DataModelField keyField = mock(DataModelField.class);
    when(keyField.getName()).thenReturn("departmentId");
    when(keyField.getIsNullable()).thenReturn(false);

    DataModelField referenceKeyField = mock(DataModelField.class);
    when(referenceKeyField.getName()).thenReturn("id");

    DataModelField populateField = mock(DataModelField.class);
    when(populateField.getName()).thenReturn("department");
    when(populateField.getDataModel()).thenReturn(rootModel);

    ModelValue value = spy(new ModelValue().build(populateField));
    doReturn((ValueType) value).when(populateField).getValueType();
    when(rootModel.getField("department")).thenReturn(Optional.of(populateField));

    doReturn(Optional.of(referenceModel)).when(value).getReferenceModel();
    doReturn(Optional.of(referenceKeyField)).when(value).getReferenceKey();
    doReturn(Optional.of(keyField)).when(value).getKey();

    PopulateContext context = new PopulateContext(rootModel, "user", "department");

    QueryPatcher patcher = value.getQueryPatch(context);

    assertEquals(1, patcher.getJoinEntries().size());

    SExpression<Boolean> joinCondition = patcher.getJoinEntries().get(0).getOn();
    assertEquals(Operators.EQ, joinCondition.getOperator());

    SExpression<?> leftField = joinCondition.getParamAsSExpression(0);
    assertEquals(Operators.FIELD, leftField.getOperator());
    assertEquals(2, leftField.getParams().size());
    assertEquals("user", leftField.getParam(0));
    assertEquals("departmentId", leftField.getParam(1));

    SExpression<?> rightField = joinCondition.getParamAsSExpression(1);
    assertEquals(Operators.FIELD, rightField.getOperator());
    assertEquals(2, rightField.getParams().size());
    assertEquals("department__demo_department", rightField.getParam(0));
    assertEquals("id", rightField.getParam(1));
  }
}
