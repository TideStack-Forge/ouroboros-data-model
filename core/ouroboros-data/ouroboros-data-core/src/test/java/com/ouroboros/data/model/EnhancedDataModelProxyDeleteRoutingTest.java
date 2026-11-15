package com.ouroboros.data.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.exception.InvalidStatementException;
import com.ouroboros.data.exception.DataModelException;
import com.ouroboros.data.record.RecordList;
import com.ouroboros.data.model.valuetypes.StringValue;
import com.ouroboros.data.record.Record;
import com.ouroboros.data.validation.Rule;

public class EnhancedDataModelProxyDeleteRoutingTest {

  @Test
  public void testDeleteByIdShouldRouteThroughDeleteWhere() {
    DataModel coreDataModel = mock(DataModel.class);
    when(coreDataModel.getPrimaryKeys()).thenReturn(Collections.singletonList(primaryKeyField()));
    when(coreDataModel.delete("A1")).thenReturn(Try.success(11L));
    SExpression<Boolean> expectedWhere = SExpression.create(
        Operators.EQ,
        SExpression.field("id"),
        SExpression.constant("A1"));
    when(coreDataModel.delete(expectedWhere)).thenReturn(Try.success(22L));

    DataModel proxy = new EnhancedDataModelProxy(coreDataModel, Collections.emptyList());

    Try<Long> result = proxy.delete("A1");

    assertTrue(result.isSuccess());
    assertEquals(22L, result.get());
    verify(coreDataModel).delete(expectedWhere);
    verify(coreDataModel, never()).delete("A1");
  }

  @Test
  public void testDeleteByIdsShouldRouteThroughDeleteWhere() {
    DataModel coreDataModel = mock(DataModel.class);
    List<String> ids = Arrays.asList("A1", "A2");
    when(coreDataModel.getPrimaryKeys()).thenReturn(Collections.singletonList(primaryKeyField()));
    when(coreDataModel.delete(ids)).thenReturn(Try.success(13L));
    SExpression<Boolean> expectedWhere = SExpression.create(
        Operators.IN,
        SExpression.field("id"),
        SExpression.constant(ids));
    when(coreDataModel.delete(expectedWhere)).thenReturn(Try.success(24L));

    DataModel proxy = new EnhancedDataModelProxy(coreDataModel, Collections.emptyList());

    Try<Long> result = proxy.delete(ids);

    assertTrue(result.isSuccess());
    assertEquals(24L, result.get());
    verify(coreDataModel).delete(expectedWhere);
    verify(coreDataModel, never()).delete(ids);
  }

  @Test
  public void testDeleteByInvalidIdShouldFailWithSameValidationSemantics() {
    DataModel coreDataModel = mock(DataModel.class);
    when(coreDataModel.getPrimaryKeys()).thenReturn(Collections.singletonList(primaryKeyField()));
    when(coreDataModel.getFullName()).thenReturn("demo.TestModel");
    when(coreDataModel.delete("")).thenReturn(Try.success(1L));

    DataModel proxy = new EnhancedDataModelProxy(coreDataModel, Collections.emptyList());

    Try<Long> result = proxy.delete("");

    assertTrue(result.isFailure());
    assertInstanceOf(InvalidStatementException.class, result.getCause());
  }

  @Test
  public void readOnlyExecutionContextRejectsWritesBeforeReachingTheCoreModel() {
    DataModel coreDataModel = mock(DataModel.class);
    when(coreDataModel.getFullName()).thenReturn("demo.TestModel");
    DataModel proxy = new EnhancedDataModelProxy(coreDataModel, Collections.emptyList());

    Try<?> result = DataModelExecutionContext.withReadOnly(true, () -> proxy.insert(Map.of("name", "blocked")));

    assertTrue(result.isFailure());
    assertInstanceOf(DataModelException.class, result.getCause());
    verify(coreDataModel, never()).insert(Map.of("name", "blocked"));
  }

  @Test
  public void successfulInsertReportsItsGeneratedObjectIdToTheCurrentExecution() {
    DataModel coreDataModel = mock(DataModel.class);
    when(coreDataModel.getFullName()).thenReturn("demo.TestModel");
    when(coreDataModel.getPrimaryKeys()).thenReturn(Collections.singletonList(primaryKeyField()));
    when(coreDataModel.insert(Map.of("name", "created")))
        .thenReturn(Try.success(Record.of(Map.of("id", "created-1", "name", "created"))));
    DataModel proxy = new EnhancedDataModelProxy(coreDataModel, Collections.emptyList());
    java.util.ArrayList<DataModelMutation> mutations = new java.util.ArrayList<>();

    DataModelExecutionContext.withExecution(false, mutations::add,
        () -> proxy.insert(Map.of("name", "created")));

    assertEquals(1, mutations.size());
    assertEquals("demo.TestModel", mutations.get(0).modelFullName());
    assertEquals("INSERT", mutations.get(0).operation());
    assertEquals(List.of("created-1"), mutations.get(0).objectIds());
    assertEquals(1L, mutations.get(0).affectedRows());
  }

  @Test
  public void conditionalDeleteLocksAndRecordsTheExactAffectedObjectIds() {
    DataModel coreDataModel = mock(DataModel.class);
    when(coreDataModel.getFullName()).thenReturn("demo.TestModel");
    when(coreDataModel.getRawName()).thenReturn("t_demo_test_model");
    when(coreDataModel.getPrimaryKeys()).thenReturn(Collections.singletonList(primaryKeyField()));
    SExpression<Boolean> condition = SExpression.create(
        Operators.EQ,
        SExpression.field("status"),
        SExpression.constant("archived"));
    when(coreDataModel.queryForUpdate(any(QueryStatement.class))).thenReturn(Try.success(RecordList.of(List.of(
        Map.of("id", "A1"), Map.of("id", "A2")))));
    when(coreDataModel.delete(any(SExpression.class))).thenReturn(Try.success(2L));
    DataModel proxy = new EnhancedDataModelProxy(coreDataModel, Collections.emptyList());
    ArrayList<DataModelMutation> mutations = new ArrayList<>();

    Try<Long> result = DataModelExecutionContext.withExecution(false, mutations::add,
        () -> proxy.delete(condition));

    assertTrue(result.isSuccess());
    assertEquals(1, mutations.size());
    assertEquals("DELETE", mutations.get(0).operation());
    assertEquals(List.of("A1", "A2"), mutations.get(0).objectIds());
    assertEquals(2L, mutations.get(0).affectedRows());
    verify(coreDataModel, times(2)).queryForUpdate(any(QueryStatement.class));
    verify(coreDataModel, never()).delete(condition);
  }

  @Test
  public void conditionalUpdateLocksAndRecordsTheExactAffectedObjectIds() {
    DataModel coreDataModel = mock(DataModel.class);
    when(coreDataModel.getFullName()).thenReturn("demo.TestModel");
    when(coreDataModel.getRawName()).thenReturn("t_demo_test_model");
    when(coreDataModel.getPrimaryKeys()).thenReturn(Collections.singletonList(primaryKeyField()));
    SExpression<Boolean> condition = SExpression.create(
        Operators.EQ,
        SExpression.field("status"),
        SExpression.constant("archived"));
    when(coreDataModel.queryForUpdate(any(QueryStatement.class))).thenReturn(Try.success(RecordList.of(List.of(
        Map.of("id", "A1"), Map.of("id", "A2")))));
    when(coreDataModel.update(any(SExpression.class), org.mockito.ArgumentMatchers.eq(Map.of("status", "deleted"))))
        .thenReturn(Try.success(2L));
    DataModel proxy = new EnhancedDataModelProxy(coreDataModel, Collections.emptyList());
    ArrayList<DataModelMutation> mutations = new ArrayList<>();

    Try<Long> result = DataModelExecutionContext.withExecution(false, mutations::add,
        () -> proxy.update(condition, Map.of("status", "deleted")));

    assertTrue(result.isSuccess());
    assertEquals(2L, result.get());
    assertEquals(1, mutations.size());
    assertEquals("UPDATE", mutations.get(0).operation());
    assertEquals(List.of("A1", "A2"), mutations.get(0).objectIds());
    assertEquals(2L, mutations.get(0).affectedRows());
    verify(coreDataModel, times(2)).queryForUpdate(any(QueryStatement.class));
    verify(coreDataModel, never()).update(condition, Map.of("status", "deleted"));
  }

  @Test
  public void mismatchedAffectedCountMarksTheActionWriteSetInvalid() {
    DataModel coreDataModel = mock(DataModel.class);
    when(coreDataModel.getFullName()).thenReturn("demo.TestModel");
    when(coreDataModel.getRawName()).thenReturn("t_demo_test_model");
    when(coreDataModel.getPrimaryKeys()).thenReturn(Collections.singletonList(primaryKeyField()));
    when(coreDataModel.queryForUpdate(any(QueryStatement.class))).thenReturn(Try.success(RecordList.of(List.of(
        Map.of("id", "A1"), Map.of("id", "A2")))));
    when(coreDataModel.update(any(SExpression.class), org.mockito.ArgumentMatchers.eq(Map.of("status", "deleted"))))
        .thenReturn(Try.success(1L));
    DataModel proxy = new EnhancedDataModelProxy(coreDataModel, Collections.emptyList());
    ArrayList<DataModelMutation> mutations = new ArrayList<>();

    assertThrows(DataModelException.class, () -> DataModelExecutionContext.withExecution(false, mutations::add, () -> {
      Try<Long> result = proxy.update(SExpression.empty(Boolean.class), Map.of("status", "deleted"));
      assertTrue(result.isFailure());
      DataModelExecutionContext.assertMutationTrackingHealthy();
      return null;
    }));

    assertTrue(mutations.isEmpty());
  }

  @Test
  public void lockFailurePreventsTheWriteAndIsVisibleAtActionCommitBoundary() {
    DataModel coreDataModel = mock(DataModel.class);
    when(coreDataModel.getFullName()).thenReturn("demo.TestModel");
    when(coreDataModel.getRawName()).thenReturn("t_demo_test_model");
    when(coreDataModel.getPrimaryKeys()).thenReturn(Collections.singletonList(primaryKeyField()));
    when(coreDataModel.queryForUpdate(any(QueryStatement.class)))
        .thenReturn(Try.failure(new DataModelException("row lock unavailable")));
    DataModel proxy = new EnhancedDataModelProxy(coreDataModel, Collections.emptyList());

    assertThrows(DataModelException.class, () -> DataModelExecutionContext.withExecution(false, ignored -> {}, () -> {
      Try<Long> result = proxy.delete(SExpression.empty(Boolean.class));
      assertTrue(result.isFailure());
      DataModelExecutionContext.assertMutationTrackingHealthy();
      return null;
    }));

    verify(coreDataModel, never()).delete(any(SExpression.class));
  }

  private DataModelField primaryKeyField() {
    return new DataModelField() {
      private final StringValue valueType = new StringValue();

      @Override
      public String getName() {
        return "id";
      }

      @Override
      public String getLabel() {
        return "id";
      }

      @Override
      public String getDescription() {
        return "";
      }

      @Override
      public String getType() {
        return "String";
      }

      @Override
      public String getRawName() {
        return "id";
      }

      @Override
      public String getRawType() {
        return "String";
      }

      @Override
      public ValueType<?> getValueType() {
        return valueType;
      }

      @Override
      public Object getDefaultValue(Map<String, Object> context) {
        return null;
      }

      @Override
      public List<Rule> getRules() {
        return Collections.emptyList();
      }

      @Override
      public Integer getDecimalDigits() {
        return null;
      }

      @Override
      public Integer getSize() {
        return null;
      }

      @Override
      public Boolean getIsNullable() {
        return false;
      }

      @Override
      public Boolean getIsUnsigned() {
        return false;
      }

      @Override
      public Boolean getIsAutoIncrement() {
        return false;
      }

      @Override
      public Boolean getIsUnique() {
        return true;
      }

      @Override
      public Map<String, Object> getExtraProps() {
        return Collections.emptyMap();
      }

      @Override
      public Optional<Object> getExtraProp(String name) {
        return Optional.empty();
      }

      @Override
      public DataModel getDataModel() {
        return null;
      }
    };
  }
}
