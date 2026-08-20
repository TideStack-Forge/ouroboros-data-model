package com.ouroboros.data.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.statement.BatchInsertStatement;
import com.ouroboros.data.dsl.statement.DMLStatement;
import com.ouroboros.data.dsl.statement.DeleteStatement;
import com.ouroboros.data.dsl.statement.InsertStatement;
import com.ouroboros.data.dsl.statement.UpdateStatement;
import com.ouroboros.data.exception.InvalidEntityName;
import com.ouroboros.data.exception.InvalidEntityNameException;
import com.ouroboros.data.exception.InvalidStatementException;
import com.ouroboros.data.exception.StatementError;
import com.ouroboros.data.exception.ValuesListError;
import com.ouroboros.data.normalize.ClauseNormalizeContext;

class DMLStatementsCoverageTest {

  @Test
  void buildInsertAndBatchInsertStatementsHandleSuccessAndValidationErrors() {
    var values = new LinkedHashMap<String, Object>();
    values.put("name", "alice");

    var insertResult = DMLStatements.buildInsertStatement("user", values);
    assertTrue(insertResult.isSuccess());
    assertEquals("user", insertResult.get().getEntityName());

    var invalidInsertResult = DMLStatements.buildInsertStatement("", values);
    assertTrue(invalidInsertResult.isFailure());
    assertInstanceOf(com.ouroboros.data.exception.InvalidEntityName.class, invalidInsertResult.getCause());

    var first = new LinkedHashMap<String, Object>();
    first.put("name", "alice");
    var second = new LinkedHashMap<String, Object>();
    second.put("age", 18);

    var batchResult = DMLStatements.buildBatchInsertStatement("user", Arrays.<Map<String, ?>>asList(first, second));
    assertTrue(batchResult.isSuccess());
    assertEquals(2, batchResult.get().getValuesList().size());
    assertEquals(new ArrayList<String>(batchResult.get().getValuesList().get(0).keySet()), new ArrayList<String>(batchResult.get().getValuesList().get(1).keySet()));

    var emptyBatchResult = DMLStatements.buildBatchInsertStatement("user", Collections.<Map<String, ?>>emptyList());
    assertTrue(emptyBatchResult.isFailure());
    assertInstanceOf(ValuesListError.class, emptyBatchResult.getCause());

    @SuppressWarnings({"rawtypes", "unchecked"})
    var invalidKeyList = (List) Collections.singletonList(Collections.singletonMap(1, "x"));
    var invalidKeyBatchResult = DMLStatements.buildBatchInsertStatement("user", invalidKeyList);
    assertTrue(invalidKeyBatchResult.isFailure());
    assertInstanceOf(ValuesListError.class, invalidKeyBatchResult.getCause());

    var varargsBatchResult = DMLStatements.buildBatchInsertStatement("user", first, second);
    assertTrue(varargsBatchResult.isSuccess());
  }

  @Test
  void normalizeInsertStatementDispatchesByValuesShape() {
    var singleInsert = new LinkedHashMap<String, Object>();
    singleInsert.put("insert", "user");
    singleInsert.put("values", Collections.singletonMap("name", "alice"));

    var insertResult = DMLStatements.normalizeInsertStatement(singleInsert);
    assertTrue(insertResult.isSuccess());
    assertInstanceOf(InsertStatement.class, insertResult.get());

    var batchInsert = new LinkedHashMap<String, Object>();
    batchInsert.put("insert", "user");
    batchInsert.put("values", Collections.singletonList(Collections.singletonMap("name", "alice")));

    var batchResult = DMLStatements.normalizeInsertStatement(batchInsert);
    assertTrue(batchResult.isSuccess());
    assertInstanceOf(BatchInsertStatement.class, batchResult.get());

    var missingEntity = new LinkedHashMap<String, Object>();
    missingEntity.put("values", Collections.singletonMap("name", "alice"));
    var missingEntityResult = DMLStatements.normalizeInsertStatement(missingEntity);
    assertTrue(missingEntityResult.isFailure());
    assertInstanceOf(InvalidEntityNameException.class, missingEntityResult.getCause());

    var invalidValuesType = new LinkedHashMap<String, Object>();
    invalidValuesType.put("insert", "user");
    invalidValuesType.put("values", 1);
    var invalidValuesResult = DMLStatements.normalizeInsertStatement(invalidValuesType);
    assertTrue(invalidValuesResult.isFailure());
    assertInstanceOf(InvalidStatementException.class, invalidValuesResult.getCause());
  }

  @Test
  void updateAndDeleteNormalizationCoverEmptyWhereAndErrorPaths() {
    var clauseContext = mock(ClauseNormalizeContext.class);
    SExpression<Boolean> where = SExpression.create(Operators.EQ, "id", 1);
    when(clauseContext.normalizeCondition(any(), eq("root"))).thenReturn(Try.success(where));

    var data = new LinkedHashMap<String, Object>();
    data.put("name", "alice");

    var mapWhereUpdate = DMLStatements.buildUpdateStatement("user", data, Collections.singletonMap("id", 1), clauseContext);
    assertTrue(mapWhereUpdate.isSuccess());
    assertSame(where, mapWhereUpdate.get().getWhere());

    var listWhereUpdate = DMLStatements.buildUpdateStatement("user", data, Collections.singletonList(Collections.singletonMap("id", 1)), clauseContext);
    assertTrue(listWhereUpdate.isSuccess());

    var updateStatement = new LinkedHashMap<String, Object>();
    updateStatement.put("update", "user");
    updateStatement.put("set", data);
    var emptyWhereUpdate = DMLStatements.normalizeUpdateStatement(updateStatement, clauseContext);
    assertTrue(emptyWhereUpdate.isSuccess());
    assertTrue(emptyWhereUpdate.get().getWhere().isEmpty());

    var invalidUpdate = new LinkedHashMap<String, Object>();
    invalidUpdate.put("update", "user");
    invalidUpdate.put("set", 1);
    var invalidUpdateResult = DMLStatements.normalizeUpdateStatement(invalidUpdate, clauseContext);
    assertTrue(invalidUpdateResult.isFailure());
    assertInstanceOf(InvalidStatementException.class, invalidUpdateResult.getCause());

    var invalidWhereUpdate = new LinkedHashMap<String, Object>();
    invalidWhereUpdate.put("update", "user");
    invalidWhereUpdate.put("set", data);
    invalidWhereUpdate.put("where", 1);
    var invalidWhereUpdateResult = DMLStatements.normalizeUpdateStatement(invalidWhereUpdate, clauseContext);
    assertTrue(invalidWhereUpdateResult.isFailure());
    assertInstanceOf(InvalidStatementException.class, invalidWhereUpdateResult.getCause());

    var failingContext = mock(ClauseNormalizeContext.class);
    when(failingContext.normalizeCondition(any(), eq("root"))).thenReturn(Try.failure(new StatementError("bad where")));
    var wrappedUpdateFailure = DMLStatements.buildUpdateStatement("user", data, Collections.singletonMap("id", 1), failingContext);
    assertTrue(wrappedUpdateFailure.isFailure());
    assertInstanceOf(StatementError.class, wrappedUpdateFailure.getCause());

    var mapWhereDelete = DMLStatements.buildDeleteStatement("user", Collections.singletonMap("id", 1), clauseContext);
    assertTrue(mapWhereDelete.isSuccess());
    assertSame(where, mapWhereDelete.get().getWhere());

    var listWhereDelete = DMLStatements.buildDeleteStatement("user", Collections.singletonList(Collections.singletonMap("id", 1)), clauseContext);
    assertTrue(listWhereDelete.isSuccess());

    var deleteAll = DMLStatements.buildDeleteAllStatement("user");
    assertTrue(deleteAll.isSuccess());
    assertNull(deleteAll.get().getWhere());

    var invalidDeleteAll = DMLStatements.buildDeleteAllStatement("");
    assertTrue(invalidDeleteAll.isFailure());
    assertInstanceOf(InvalidEntityName.class, invalidDeleteAll.getCause());

    var deleteStatement = new LinkedHashMap<String, Object>();
    deleteStatement.put("delete", "user");
    var emptyWhereDelete = DMLStatements.normalizeDeleteStatement(deleteStatement, clauseContext);
    assertTrue(emptyWhereDelete.isSuccess());
    assertNull(emptyWhereDelete.get().getWhere());

    var invalidDelete = new LinkedHashMap<String, Object>();
    invalidDelete.put("delete", "user");
    invalidDelete.put("where", 1);
    var invalidDeleteResult = DMLStatements.normalizeDeleteStatement(invalidDelete, clauseContext);
    assertTrue(invalidDeleteResult.isFailure());
    assertInstanceOf(InvalidStatementException.class, invalidDeleteResult.getCause());
  }

  @Test
  void normalizeStatementDispatchesAndWrapsErrors() {
    var clauseContext = mock(ClauseNormalizeContext.class);
    when(clauseContext.normalizeCondition(any(), eq("root"))).thenReturn(Try.success(SExpression.create(Operators.EQ, "id", 1)));

    var existing = InsertStatement.of("user", Collections.<String, Object>singletonMap("name", "alice"));
    var existingResult = DMLStatements.normalizeStatement(existing, clauseContext);
    assertTrue(existingResult.isSuccess());
    assertSame(existing, existingResult.get());

    var emptyStatement = DMLStatements.normalizeStatement(Collections.<String, Object>emptyMap(), clauseContext);
    assertTrue(emptyStatement.isFailure());
    assertInstanceOf(StatementError.class, emptyStatement.getCause());

    var insert = new LinkedHashMap<String, Object>();
    insert.put("insert", "user");
    insert.put("values", Collections.singletonMap("name", "alice"));
    assertInstanceOf(InsertStatement.class, DMLStatements.normalizeStatement(insert, clauseContext).get());

    var update = new LinkedHashMap<String, Object>();
    update.put("update", "user");
    update.put("set", Collections.singletonMap("name", "alice"));
    assertInstanceOf(UpdateStatement.class, DMLStatements.normalizeStatement(update, clauseContext).get());

    var delete = new LinkedHashMap<String, Object>();
    delete.put("delete", "user");
    assertInstanceOf(DeleteStatement.class, DMLStatements.normalizeStatement(delete, clauseContext).get());

    var invalid = new LinkedHashMap<String, Object>();
    invalid.put("noop", true);
    var invalidResult = DMLStatements.normalizeStatement(invalid, clauseContext);
    assertTrue(invalidResult.isFailure());
    assertInstanceOf(StatementError.class, invalidResult.getCause());
  }
}
