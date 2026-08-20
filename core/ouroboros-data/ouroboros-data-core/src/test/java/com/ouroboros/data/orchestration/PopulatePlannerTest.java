package com.ouroboros.data.orchestration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.dsl.statement.PopulateClause;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.PopulateContext;
import com.ouroboros.data.model.valuetypes.ModelValue;
import com.ouroboros.data.model.valuetypes.StringValue;
import com.ouroboros.data.orchestration.strategy.PopulateField;

class PopulatePlannerTest {

  @Test
  void shouldIgnoreDottedFieldsWhenDerivingPopulateSelectFields() {
    PopulatePlanner planner = new PopulatePlanner();
    OrchestrationContext context = new OrchestrationContext();
    DataModel rootModel = mock(DataModel.class);
    DataModel relatedModel = mock(DataModel.class);
    DataModelField relationField = mock(DataModelField.class);
    ModelValue modelValue = mock(ModelValue.class);

    DataModelField localKeyField = mock(DataModelField.class);
    when(localKeyField.getName()).thenReturn("cameraId");

    DataModelField remoteKeyField = mock(DataModelField.class);
    when(remoteKeyField.getName()).thenReturn("id");

    DataModelField idField = mock(DataModelField.class);
    when(idField.getName()).thenReturn("id");
    doReturn(new StringValue()).when(idField).getValueType();

    DataModelField nameField = mock(DataModelField.class);
    when(nameField.getName()).thenReturn("name");
    doReturn(new StringValue()).when(nameField).getValueType();

    DataModelField dottedField = mock(DataModelField.class);
    when(dottedField.getName()).thenReturn("owner.id");
    doReturn(new StringValue()).when(dottedField).getValueType();

    when(rootModel.getName()).thenReturn("RootModel");
    when(rootModel.getField("camera")).thenReturn(Optional.of(relationField));
    when(relatedModel.getFields()).thenReturn(Arrays.asList(idField, nameField, dottedField));

    when(relationField.getName()).thenReturn("camera");
    when(relationField.getDataModel()).thenReturn(rootModel);
    doReturn(modelValue).when(relationField).getValueType();

    when(modelValue.getReferenceModel()).thenReturn(Optional.of(relatedModel));
    when(modelValue.getKey()).thenReturn(Optional.of(localKeyField));
    when(modelValue.getReferenceKey()).thenReturn(Optional.of(remoteKeyField));

    context.setMainStatement(QueryStatement.builder()
        .from("root_model")
        .populate("camera")
        .build());

    List<PopulateField> populateFields = planner.extractPopulateFields(context, rootModel);

    assertEquals(1, populateFields.size());
    PopulateField populateField = populateFields.get(0);
    assertNotNull(populateField.selectFields());
    assertEquals(Arrays.asList("id", "name"), populateField.selectFields(),
        "populate 默认补列不应包含点号字段");
  }

  @Test
  void shouldIgnoreDottedFieldsWhenBuildingPopulateContexts() {
    PopulatePlanner planner = new PopulatePlanner();
    DataModel rootModel = mock(DataModel.class);
    DataModel relatedModel = mock(DataModel.class);
    DataModelField relationField = mock(DataModelField.class);
    ModelValue modelValue = mock(ModelValue.class);

    DataModelField idField = mock(DataModelField.class);
    when(idField.getName()).thenReturn("id");
    doReturn(new StringValue()).when(idField).getValueType();

    DataModelField nameField = mock(DataModelField.class);
    when(nameField.getName()).thenReturn("name");
    doReturn(new StringValue()).when(nameField).getValueType();

    DataModelField dottedField = mock(DataModelField.class);
    when(dottedField.getName()).thenReturn("owner.id");
    doReturn(new StringValue()).when(dottedField).getValueType();

    when(rootModel.getField("camera")).thenReturn(Optional.of(relationField));
    when(relatedModel.getFields()).thenReturn(Arrays.asList(idField, nameField, dottedField));

    doReturn(modelValue).when(relationField).getValueType();
    when(modelValue.getReferenceModel()).thenReturn(Optional.of(relatedModel));

    List<PopulateContext> contexts = planner.buildPopulateContexts(
        PopulateClause.fromRaw(Collections.singletonList("camera")),
        rootModel,
        "root_model");

    assertEquals(1, contexts.size());
    assertEquals(Arrays.asList(idField, nameField), contexts.get(0).getPopulateConfig().getSelect(),
        "PopulateContext 默认 select 不应包含点号字段");
  }
}
