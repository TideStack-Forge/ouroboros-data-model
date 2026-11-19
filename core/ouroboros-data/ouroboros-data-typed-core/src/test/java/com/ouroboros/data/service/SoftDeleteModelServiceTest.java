package com.ouroboros.data.service;

import com.ouroboros.common.service.SoftDelete;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.TypedDataModel;

import io.vavr.control.Try;

class SoftDeleteModelServiceTest {
  @Mock
  TypedDataModel<String, Item> dataModel;
  @Mock
  DataModel untypedModel;

  private AutoCloseable mocks;
  private final Item active = new Item("active", false);
  private final Item deleted = new Item("deleted", true);
  private SoftDeleteModelServiceImpl<String, Item, Item> service;

  @BeforeEach
  void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    service = new SoftDeleteModelServiceImpl<>() {
      @Override
      protected TypedDataModel<String, Item> getDataModel() {
        return dataModel;
      }

      @Override
      protected Try<String> getDeletedFieldName() {
        return Try.success("deleted");
      }
    };
    when(dataModel.getDataModel()).thenReturn(untypedModel);
    when(untypedModel.getFields()).thenReturn(List.of());
    when(dataModel.query(anyMap())).thenAnswer(call -> Try.success(matching(where(call.getArgument(0)))));
    when(dataModel.count(anyMap())).thenAnswer(call -> Try.success((long) matching(call.getArgument(0)).size()));
    when(dataModel.get(anyString(), anyMap())).thenAnswer(call -> {
      String name = call.getArgument(0);
      return Try.success(matching(where(call.getArgument(1))).stream()
          .filter(item -> item.name.equals(name)).findFirst().orElse(null));
    });
  }

  @AfterEach
  void closeMocks() throws Exception {
    mocks.close();
  }

  @Test
  void listWithoutWhereExcludesDeletedRecords() {
    assertEquals(List.of(active), service.searchList(Map.of()));
  }

  @Test
  void nullWhereIsSupportedWithoutChangingTheStatement() {
    Map<String, Object> statement = Collections.singletonMap("WHERE", null);
    assertEquals(List.of(active), service.searchList(statement));
    assertNull(statement.get("WHERE"));
    assertFalse(statement.containsKey("where"));
  }

  @Test
  void callerConditionsArePreservedAndImmutableParametersAreAccepted() {
    Map<String, Object> conditions = Map.of("name", "deleted");
    Map<String, Object> statement = Map.of("where", conditions);
    assertEquals(List.of(), service.searchList(statement));
    assertEquals(Map.of("name", "deleted"), conditions);
  }

  @Test
  void singleResultQueryUsesTheSameActiveRecordFilter() {
    assertEquals(active, service.searchOne(Map.of()));
  }

  @Test
  void nullStatementAndSearchAllReturnActiveRecords() {
    assertEquals(List.of(active), service.searchList(null));
    assertEquals(List.of(active), service.searchAll());
  }

  @Test
  void countAcceptsNullAndImmutableConditions() {
    assertEquals(1L, service.count(null));
    assertEquals(1L, service.count(Map.of()));
  }

  @Test
  void countingDoesNotMutateCallerConditions() {
    Map<String, Object> conditions = new HashMap<>(Map.of("name", "active"));
    assertEquals(1L, service.count(conditions));
    assertEquals(Map.of("name", "active"), conditions);
  }

  @Test
  void primaryKeyQueriesHideDeletedRecordsWithBothOverloads() {
    assertNull(service.getByPrimaryKey("deleted"));
    assertNull(service.getByPrimaryKey("deleted", Map.of()));
    assertEquals(active, service.getByPrimaryKey("active"));
    assertEquals(active, service.getByPrimaryKey("active", Map.of()));
  }

  private Map<?, ?> where(Map<String, Object> statement) {
    return statement.entrySet().stream()
        .filter(entry -> entry.getKey().equalsIgnoreCase("where"))
        .map(Map.Entry::getValue)
        .filter(Map.class::isInstance)
        .map(value -> (Map<?, ?>) value)
        .findFirst().orElse(Map.of());
  }

  private List<Item> matching(Map<?, ?> conditions) {
    return List.of(active, deleted).stream()
        .filter(item -> !conditions.containsKey("deleted")
            || Objects.equals(conditions.get("deleted"), item.isDeleted()))
        .filter(item -> !conditions.containsKey("name")
            || Objects.equals(conditions.get("name"), item.name))
        .toList();
  }

  private static final class Item implements SoftDelete {
    private final String name;
    private boolean deleted;

    private Item(String name, boolean deleted) {
      this.name = name;
      this.deleted = deleted;
    }

    @Override
    public boolean isDeleted() {
      return deleted;
    }

    @Override
    public void setDeleted(boolean deleted) {
      this.deleted = deleted;
    }
  }
}
