package com.ouroboros.data.model;

import static com.ouroboros.data.dsl.query.Query.field;
import static com.ouroboros.data.dsl.query.Query.populate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.record.RecordList;

class DataModelQueryFacadeTest {

  @Test
  void executeShouldDelegateToRawMapQueryPath() {
    DataModel dataModel = mock(DataModel.class, CALLS_REAL_METHODS);
    when(dataModel.query(anyMap())).thenReturn(Try.success(RecordList.empty()));

    Try<RecordList> result = dataModel.query()
        .where(field("status").eq("ENABLED"))
        .execute();

    assertTrue(result.isSuccess());

    ArgumentCaptor<Map<String, Object>> statementCaptor = ArgumentCaptor.forClass(Map.class);
    verify(dataModel).query(statementCaptor.capture());
    verify(dataModel, never()).query(any(QueryStatement.class));
    assertEquals(Map.of("WHERE", Map.of("status", "ENABLED")), statementCaptor.getValue());
  }

  @Test
  void buildShouldValidateDynamicFieldsWhenMetadataIsResolved() {
    DataModel dataModel = mock(DataModel.class, CALLS_REAL_METHODS);
    DataModelField id = fieldMeta("id");
    DataModelField status = fieldMeta("status");
    when(dataModel.getFields()).thenReturn(List.of(id, status));
    when(dataModel.getName()).thenReturn("User");
    when(dataModel.getRawName()).thenReturn("user");
    when(dataModel.getFullName()).thenReturn("test.User");

    assertDoesNotThrow(() -> dataModel.query()
        .select(field("user", "id"))
        .where(field("status").eq("ENABLED"))
        .build());

    IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> dataModel.query()
        .where(field("missing").eq("value"))
        .build());
    assertTrue(error.getMessage().contains("missing"));
  }

  @Test
  void buildShouldKeepDynamicFieldsWhenMetadataIsUnresolved() {
    DataModel dataModel = mock(DataModel.class, CALLS_REAL_METHODS);
    when(dataModel.getFields()).thenReturn(Collections.emptyList());

    Map<String, Object> rawMap = dataModel.query()
        .where(field("platformOnlyField").eq("value"))
        .build();

    assertEquals(Map.of("platformOnlyField", "value"), rawMap.get("WHERE"));
  }

  @Test
  void buildShouldValidateOnlyTopLevelPopulateFieldsWhenMetadataIsResolved() {
    DataModel dataModel = mock(DataModel.class, CALLS_REAL_METHODS);
    DataModelField user = fieldMeta("user");
    when(dataModel.getFields()).thenReturn(List.of(user));
    when(dataModel.getName()).thenReturn("Order");
    when(dataModel.getRawName()).thenReturn("order");
    when(dataModel.getFullName()).thenReturn("test.Order");

    assertDoesNotThrow(() -> dataModel.query()
        .populate(populate("user")
            .select("id", "name")
            .where(Map.of("status", "active"))
            .populate(populate("department")
                .where(Map.of("code", "TECH"))))
        .build());

    IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> dataModel.query()
        .populate(populate("missing").select("id"))
        .build());
    assertTrue(error.getMessage().contains("missing"));
  }

  @Test
  void buildShouldValidateSelectAndWhereClauseFormsWithoutRejectingAliases() {
    DataModel dataModel = mock(DataModel.class, CALLS_REAL_METHODS);
    DataModelField id = fieldMeta("id");
    DataModelField name = fieldMeta("name");
    DataModelField status = fieldMeta("status");
    DataModelField age = fieldMeta("age");
    when(dataModel.getFields()).thenReturn(List.of(id, name, status, age));
    when(dataModel.getName()).thenReturn("User");
    when(dataModel.getRawName()).thenReturn("user");
    when(dataModel.getFullName()).thenReturn("test.User");

    assertDoesNotThrow(() -> dataModel.query()
        .select("id, name as userName")
        .where(Map.of("or", List.of(
            Map.of("status", "ENABLED"),
            Map.of("age", Map.of("$gte", 18))
        )))
        .build());

    IllegalArgumentException selectError = assertThrows(IllegalArgumentException.class, () -> dataModel.query()
        .select("missing as missingName")
        .build());
    assertTrue(selectError.getMessage().contains("missing"));

    IllegalArgumentException whereError = assertThrows(IllegalArgumentException.class, () -> dataModel.query()
        .where(Map.of("missing", "value"))
        .build());
    assertTrue(whereError.getMessage().contains("missing"));
  }

  private DataModelField fieldMeta(String name) {
    DataModelField field = mock(DataModelField.class);
    when(field.getName()).thenReturn(name);
    return field;
  }
}
