package com.ouroboros.data.model;

import static com.ouroboros.data.dsl.query.Query.field;
import static com.ouroboros.data.dsl.query.Query.populate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
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
import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.record.RecordList;

class DataModelQueryFacadeTest {

  @Test
  void primaryKeyReadCombinesAdditionalConditionsWithoutReplacingTheId() {
    AbstractDataModel model = mock(AbstractDataModel.class, CALLS_REAL_METHODS);
    doReturn(Try.success(Map.of("id", 2L))).when(model).buildIdPredicate(2L);
    List<Map<String, Object>> rows = List.of(
        Map.of("id", 1L, "deleted", false),
        Map.of("id", 2L, "deleted", true));
    when(model.query(anyMap())).thenAnswer(call -> {
      Map<String, Object> statement = call.getArgument(0);
      return Try.success(RecordList.of(rows.stream()
          .filter(row -> matches(row, Keyword.WHERE.findIn(statement))).toList()));
    });

    for (String key : List.of("where", "WHERE", "Where")) {
      Map<String, Object> input = Map.of(key, Map.of("deleted", false));
      assertNull(model.get(2L, input).get());
      assertEquals(Map.of(key, Map.of("deleted", false)), input);
      assertNull(model.get(2L, Map.of(key, Map.of("id", 1L))).get());
    }
    assertEquals(2L, model.get(2L, null).get().get("id"));
    assertEquals(2L, model.get(2L).get().get("id"));
  }

  private boolean matches(Map<String, Object> row, Object predicate) {
    if (!(predicate instanceof Map<?, ?> conditions)) {
      return false;
    }
    if (conditions.get("$and") instanceof List<?> conjunction) {
      return conjunction.stream().allMatch(condition -> matches(row, condition));
    }
    return conditions.entrySet().stream()
        .allMatch(entry -> java.util.Objects.equals(row.get(entry.getKey()), entry.getValue()));
  }

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
