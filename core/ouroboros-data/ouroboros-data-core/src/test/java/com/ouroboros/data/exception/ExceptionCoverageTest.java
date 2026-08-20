package com.ouroboros.data.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class ExceptionCoverageTest {

  @Test
  void baseExceptionsExposeStandardConstructors() {
    var cause = new IllegalStateException("boom");

    var dataModelException = new DataModelException();
    assertEquals(null, dataModelException.getMessage());

    assertEquals("msg", new DataModelException("msg").getMessage());
    assertSame(cause, new DataModelException("msg", cause).getCause());
    assertSame(cause, new DataModelException(cause).getCause());
    assertEquals("msg", new DataModelException("msg", cause, true, false).getMessage());

    var dataValidationException = new DataValidationException();
    assertEquals(null, dataValidationException.getMessage());
    assertEquals("msg", new DataValidationException("msg").getMessage());
    assertSame(cause, new DataValidationException("msg", cause).getCause());
    assertSame(cause, new DataValidationException(cause).getCause());
    assertEquals("msg", new DataValidationException("msg", cause, true, false).getMessage());

    var statementException = new StatementException();
    assertEquals(null, statementException.getMessage());
    assertEquals("msg", new StatementException("msg").getMessage());
    assertSame(cause, new StatementException("msg", cause).getCause());
    assertSame(cause, new StatementException(cause).getCause());
    assertEquals("msg", new StatementException("msg", cause, true, false).getMessage());

    var normalizeException = new NormalizeException();
    assertEquals(null, normalizeException.getMessage());
    assertEquals("msg", new NormalizeException("msg").getMessage());
    assertSame(cause, new NormalizeException("msg", cause).getCause());
    assertSame(cause, new NormalizeException(cause).getCause());
    assertEquals("msg", new NormalizeException("msg", cause, true, false).getMessage());

    var transpileException = new TranspileException();
    assertEquals(null, transpileException.getMessage());
    assertEquals("msg", new TranspileException("msg").getMessage());
    assertSame(cause, new TranspileException("msg", cause).getCause());
    assertSame(cause, new TranspileException(cause).getCause());
    assertEquals("msg", new TranspileException("msg", cause, true, false).getMessage());

    var metadataException = new MetadataException();
    assertEquals(null, metadataException.getMessage());
    assertEquals("msg", new MetadataException("msg").getMessage());
    assertSame(cause, new MetadataException("msg", cause).getCause());
    assertSame(cause, new MetadataException(cause).getCause());
    assertEquals("msg", new MetadataException("msg", cause, true, false).getMessage());

    var dataAccessException = new DataAccessException();
    assertEquals(null, dataAccessException.getMessage());
    assertEquals("msg", new DataAccessException("msg").getMessage());
    assertSame(cause, new DataAccessException("msg", cause).getCause());
    assertSame(cause, new DataAccessException(cause).getCause());
    assertEquals("msg", new DataAccessException("msg", cause, true, false).getMessage());

    var statementError = new StatementError();
    assertEquals(null, statementError.getMessage());
    assertEquals("msg", new StatementError("msg").getMessage());
    assertSame(cause, new StatementError(cause).getCause());
  }

  @Test
  void leafExceptionsExposePayloads() {
    var cause = new IllegalArgumentException("boom");

    var fieldMetadataException = new FieldMetadataException("msg", "field");
    assertEquals("field", fieldMetadataException.getFieldName());
    assertEquals(null, fieldMetadataException.getModelName());
    assertEquals("msg", fieldMetadataException.getMessage());

    var withModel = new FieldMetadataException("msg", "field", "model", cause);
    assertEquals("field", withModel.getFieldName());
    assertEquals("model", withModel.getModelName());
    assertSame(cause, withModel.getCause());

    var withModelNoCause = new FieldMetadataException("msg", "field", "model");
    assertEquals("model", withModelNoCause.getModelName());

    var mismatch = new TypeMismatchException("field", "String", "Integer");
    assertEquals("field", mismatch.getFieldName());
    assertEquals("String", mismatch.getExpectedType());
    assertEquals("Integer", mismatch.getActualType());
    assertEquals("Type mismatch for field 'field': expected String but got Integer", mismatch.getMessage());

    var customMismatch = new TypeMismatchException("custom", "field", "String", "Long");
    assertEquals("custom", customMismatch.getMessage());

    var fieldNotFoundException = new FieldNotFoundException("field", "entity");
    assertEquals("field", fieldNotFoundException.getFieldName());
    assertEquals("entity", fieldNotFoundException.getEntityName());
    assertEquals("Field 'field' not found in entity 'entity'", fieldNotFoundException.getMessage());

    var entityNotFoundException = new EntityNotFoundException("entity");
    assertEquals("entity", entityNotFoundException.getEntityName());
    assertEquals("Entity not found: entity", entityNotFoundException.getMessage());

    var customEntityNotFoundException = new EntityNotFoundException("msg", "entity");
    assertEquals("msg", customEntityNotFoundException.getMessage());

    var connectionException = new ConnectionException("msg");
    assertEquals("msg", connectionException.getMessage());
    assertSame(cause, new ConnectionException("msg", cause).getCause());

    var executeException = new ExecuteException();
    assertEquals(null, executeException.getMessage());
    assertEquals("msg", new ExecuteException("msg").getMessage());
    assertSame(cause, new ExecuteException("msg", cause).getCause());
    assertSame(cause, new ExecuteException(cause).getCause());
    assertEquals("msg", new ExecuteException("msg", cause, true, false).getMessage());

    var queryExecutionException = new QueryExecutionException("msg");
    assertEquals("msg", queryExecutionException.getMessage());
    assertEquals(null, queryExecutionException.getQuery());
    assertSame(cause, new QueryExecutionException("msg", cause).getCause());
    assertEquals("sql", new QueryExecutionException("msg", "sql", cause).getQuery());

    var invalidStatementException = new InvalidStatementException();
    assertEquals(null, invalidStatementException.getMessage());
    assertEquals("msg", new InvalidStatementException("msg").getMessage());
    assertSame(cause, new InvalidStatementException("msg", cause).getCause());
    assertSame(cause, new InvalidStatementException(cause).getCause());

    var syntaxException = new StatementSyntaxException();
    assertEquals(null, syntaxException.getMessage());
    assertEquals("msg", new StatementSyntaxException("msg").getMessage());
    assertSame(cause, new StatementSyntaxException("msg", cause).getCause());
    assertSame(cause, new StatementSyntaxException(cause).getCause());

    var modelMetadataException = new ModelMetadataException("msg");
    assertEquals("msg", modelMetadataException.getMessage());
    assertEquals(null, modelMetadataException.getModelName());
    assertEquals("model", new ModelMetadataException("msg", "model").getModelName());
    assertSame(cause, new ModelMetadataException("msg", "model", cause).getCause());

    var uniqueConstraintViolationException = new UniqueConstraintViolationException("msg");
    assertEquals(null, uniqueConstraintViolationException.getConflictFields());
    assertEquals(Arrays.asList("code"), new UniqueConstraintViolationException("msg", Arrays.asList("code")).getConflictFields());

    var primaryKeyValidationException = new PrimaryKeyValidationException("msg");
    assertEquals(null, primaryKeyValidationException.getPrimaryKeyFields());
    assertEquals(Arrays.asList("id"), new PrimaryKeyValidationException("msg", Arrays.asList("id")).getPrimaryKeyFields());

    var primaryKeyGenerationException = new PrimaryKeyGenerationException("msg");
    assertEquals(null, primaryKeyGenerationException.getGeneratorName());
    assertEquals("snowflake", new PrimaryKeyGenerationException("msg", "snowflake").getGeneratorName());
    assertEquals("snowflake", new PrimaryKeyGenerationException("msg", "snowflake", cause).getGeneratorName());
    assertSame(cause, new PrimaryKeyGenerationException("msg", cause).getCause());

    var referenceConstraintViolationException = new ReferenceConstraintViolationException("msg");
    assertEquals(null, referenceConstraintViolationException.getReferenceModelName());
    assertEquals(0L, referenceConstraintViolationException.getReferenceCount());
    var referenceConflict = new ReferenceConstraintViolationException("msg", "order", 3L);
    assertEquals("order", referenceConflict.getReferenceModelName());
    assertEquals(3L, referenceConflict.getReferenceCount());

    var singleError = new FieldValidationException("name", "required");
    assertEquals("name", singleError.getFieldName());
    assertEquals(Collections.singletonList("required"), singleError.getErrors());

    var multipleErrors = new FieldValidationException("name", Arrays.asList("required", "too short"));
    assertEquals(Arrays.asList("required", "too short"), multipleErrors.getErrors());
    assertThrows(UnsupportedOperationException.class, () -> multipleErrors.getErrors().add("x"));

    var invalidEntityNameException = new InvalidEntityNameException("msg", "user");
    assertEquals("user", invalidEntityNameException.getEntityName());
    assertEquals("msg", invalidEntityNameException.getMessage());
    assertSame(cause, new InvalidEntityNameException("msg", "user", cause).getCause());

    var invalidEntityName = new InvalidEntityName("msg", Optional.of("user"));
    assertEquals(Optional.of("user"), invalidEntityName.getEntityName());
    assertEquals("msg", invalidEntityName.getMessage());

    var valuesMessageError = new ValuesError("msg", Optional.<Map<String, StatementCheckFailure>>empty());
    assertEquals("msg", valuesMessageError.getMessage());
  }

  @Test
  void recordValidationExceptionsExposeJoinedAndLegacyViews() {
    var errors = new LinkedHashMap<String, List<String>>();
    errors.put("name", Arrays.asList("required", "too short"));
    errors.put("code", Collections.<String>emptyList());

    var recordValidationException = new RecordValidationException(errors);
    assertEquals("Record validation failed: 2 field(s) have errors", recordValidationException.getMessage());
    assertEquals(errors, recordValidationException.getFieldErrors());
    assertEquals("required|too short", recordValidationException.getJoinedErrors("|").get("name"));
    assertEquals("required", recordValidationException.getFirstErrors().get("name"));
    assertFalse(recordValidationException.getJoinedErrors("|").containsKey("code"));

    assertThrows(UnsupportedOperationException.class, () -> recordValidationException.getFieldErrors().put("x", Collections.<String>emptyList()));

    var validationException = new ValidationException(errors);
    assertEquals(errors, validationException.getErrors());
    assertEquals(validationException.getMessage(), validationException.getErrorMessage());
    assertEquals("required,too short", validationException.getJoinedErrors().get("name"));

    var validationMessageOnly = new ValidationException("custom");
    assertEquals("custom", validationMessageOnly.getMessage());
    assertTrue(validationMessageOnly.getErrors().isEmpty());

    var checkFailure = new StatementCheckFailure("field", StatementCheckFailure.FailureType.TYPE_MISMATCH, "bad type");
    assertEquals("field", checkFailure.getFieldName());
    assertEquals(StatementCheckFailure.FailureType.TYPE_MISMATCH, checkFailure.getFailureType());
    assertEquals("bad type", checkFailure.getMessage());
    assertEquals("TYPE_MISMATCH", StatementCheckFailure.FailureType.TYPE_MISMATCH.getTypeName());
    assertEquals("数据类型不匹配", StatementCheckFailure.FailureType.TYPE_MISMATCH.getTypeLabel());

    var detail = new LinkedHashMap<String, StatementCheckFailure>();
    detail.put("name", checkFailure);
    var valuesError = new ValuesError("values", Optional.of(detail));
    assertTrue(valuesError.getDetail().isPresent());
    assertSame(checkFailure, valuesError.getDetail().get().get("name"));

    var lineErrors = new LinkedHashMap<Long, Optional<Map<String, StatementCheckFailure>>>();
    lineErrors.put(1L, Optional.of(detail));
    var valuesListError = new ValuesListError("lines", lineErrors);
    assertEquals("lines", valuesListError.getMessage());
    assertTrue(valuesListError.getErrorLines().containsKey(1L));
  }

  @Test
  void migrationAndDatabaseExceptionsExposeStructuredMetadata() {
    var genericCause = new IllegalStateException("boom");
    var sqlCause = new SQLException("sql boom", "42000", 1064);

    var migrationException = new MigrationException();
    assertEquals(null, migrationException.getMessage());
    assertEquals("msg", new MigrationException("msg").getMessage());
    assertSame(genericCause, new MigrationException("msg", genericCause).getCause());
    assertSame(genericCause, new MigrationException(genericCause).getCause());
    assertEquals("msg", new MigrationException("msg", genericCause, true, false).getMessage());

    var databaseMigrationException = new DatabaseMigrationException("msg");
    assertEquals(null, databaseMigrationException.getDataStationName());
    assertEquals("station", new DatabaseMigrationException("msg", "station").getDataStationName());
    assertEquals("station", new DatabaseMigrationException("msg", "station", genericCause).getDataStationName());
    assertSame(genericCause, new DatabaseMigrationException("msg", genericCause).getCause());

    var sqlDatabaseException = new DatabaseException("msg", sqlCause);
    assertEquals("42000", sqlDatabaseException.getSqlState());
    assertEquals(1064, sqlDatabaseException.getErrorCode());
    assertSame(sqlCause, sqlDatabaseException.getCause());

    var genericDatabaseException = new DatabaseException(genericCause);
    assertEquals(null, genericDatabaseException.getSqlState());
    assertEquals(0, genericDatabaseException.getErrorCode());
    assertSame(genericCause, genericDatabaseException.getCause());

    var explicitDatabaseException = new DatabaseException("msg", "HY000", 1001, genericCause);
    assertEquals("HY000", explicitDatabaseException.getSqlState());
    assertEquals(1001, explicitDatabaseException.getErrorCode());
  }
}
