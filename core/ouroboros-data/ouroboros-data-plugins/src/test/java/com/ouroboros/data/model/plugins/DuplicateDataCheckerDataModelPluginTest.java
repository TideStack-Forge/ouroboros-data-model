package com.ouroboros.data.model.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.exception.UniqueConstraintViolationException;
import com.ouroboros.data.exception.InvalidStatementException;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.DataModelPlugin;
import com.ouroboros.data.model.DataModelPluginContext;
import com.ouroboros.data.model.DataModelUniqueConstraintMeta;
import com.ouroboros.data.model.PluginDescriptor;
import com.ouroboros.data.model.ValueType;
import com.ouroboros.data.model.valuetypes.BooleanValue;
import com.ouroboros.data.model.valuetypes.LongValue;
import com.ouroboros.data.model.valuetypes.StringValue;
import com.ouroboros.data.record.Record;
import com.ouroboros.data.record.RecordList;

class DuplicateDataCheckerDataModelPluginTest {

  @Test
  void insertChecksDuplicateThroughModelWithoutOnlyDuplicateChecker() {
    var fixture = new Fixture(List.of("id"));
    DataModelPlugin plugin = fixture.buildPlugin(false);
    fixture.count(where -> linkedMap("email", "alice@example.com").equals(where) ? 0L : 1L);

    Try<Record> result = plugin.insert(linkedMap("email", "alice@example.com"), fixture.context);

    assertTrue(result.isSuccess());
    assertEquals(linkedMap("email", "alice@example.com"), fixture.lastCountWhere);
    verify(fixture.source).withoutPlugins();
    verify(fixture.stripped, never()).withPlugins(org.mockito.ArgumentMatchers.anyCollection());
  }

  @Test
  void activeRecordsCanReapplyExistingLogicalDeletePluginForDuplicateCheck() {
    var fixture = new Fixture(List.of("id"));
    fixture.hasPlugin("LogicalDelete");
    when(fixture.source.getExtraProp("uniquenessScope")).thenReturn(Optional.of("ACTIVE_RECORDS"));
    DataModelPlugin plugin = fixture.buildPlugin(false);
    fixture.count(where -> 0L);

    Try<Record> result = plugin.insert(linkedMap("email", "alice@example.com"), fixture.context);

    assertTrue(result.isSuccess());
    verify(fixture.source).withoutPlugins();
    verify(fixture.stripped).withPlugins(pluginDescriptorCollection("LogicalDelete"));
  }

  @Test
  void activeRecordsCanReapplyExistingLegacySoftDeletePluginForDuplicateCheck() {
    var fixture = new Fixture(List.of("id"));
    fixture.hasPlugin("SoftDelete");
    when(fixture.source.getExtraProp("uniquenessScope")).thenReturn(Optional.of("ACTIVE_RECORDS"));
    DataModelPlugin plugin = fixture.buildPlugin(false);
    fixture.count(where -> 0L);

    Try<Record> result = plugin.insert(linkedMap("email", "alice@example.com"), fixture.context);

    assertTrue(result.isSuccess());
    verify(fixture.source).withoutPlugins();
    verify(fixture.stripped).withPlugins(pluginDescriptorCollection("SoftDelete"));
  }

  @Test
  void duplicateDetailUsesSameScopedWhereAsCount() {
    var fixture = new Fixture(List.of("id"));
    DataModelPlugin plugin = fixture.buildPlugin(true);
    fixture.count(where -> 1L);
    fixture.query(RecordList.of(List.of(
        linkedMap("email", "alice@example.com", "code", "other", "isDeleted", false)
    )));

    Try<Record> result = plugin.insert(linkedMap("email", "alice@example.com"), fixture.context);

    assertFalse(result.isSuccess());
    assertTrue(result.getCause() instanceof UniqueConstraintViolationException);
    assertEquals(
        List.of("email"),
        ((UniqueConstraintViolationException) result.getCause()).getConflictFields()
    );
    assertEquals(fixture.lastCountWhere, fixture.lastQueryWhere);
  }

  @Test
  void insertReturnsCountFailureAndDoesNotCallTailInsert() {
    var fixture = new Fixture(List.of("id"));
    DataModelPlugin plugin = fixture.buildPlugin(false);
    var failure = new IllegalStateException("count failed");
    fixture.countFailure(failure);

    Try<Record> result = plugin.insert(linkedMap("email", "alice@example.com"), fixture.context);

    assertFalse(result.isSuccess());
    assertEquals(failure, result.getCause());
    assertEquals(0, fixture.tail.insertCalls);
  }

  @Test
  void insertAcceptsIndependentValuesAndRejectsConflictOnEitherUniqueField() {
    for (var existing : List.of(linkedMap("email", "other@example.com", "code", "OTHER"),
        linkedMap("email", "alice@example.com", "code", "OTHER"),
        linkedMap("email", "other@example.com", "code", "A001"))) {
      var fixture = new Fixture(List.of("id"));
      DataModelPlugin plugin = fixture.buildPlugin(false);
      fixture.count(where -> matches(where, existing) ? 1L : 0L);
      var result = plugin.insert(linkedMap("email", "alice@example.com", "code", "A001"), fixture.context);
      boolean conflict = "alice@example.com".equals(existing.get("email")) || "A001".equals(existing.get("code"));
      assertEquals(!conflict, result.isSuccess());
      assertEquals(conflict ? 0 : 1, fixture.tail.insertCalls);
      if (conflict) assertTrue(result.getCause() instanceof UniqueConstraintViolationException);
    }
  }

  @Test
  void secondUniqueCheckFailureStopsTheInsertAndPreservesItsCause() {
    var fixture = new Fixture(List.of("id"));
    var failure = new IllegalStateException("第二个唯一键查询失败");
    when(fixture.stripped.count(anyMap())).thenReturn(Try.success(0L), Try.failure(failure));
    var result = fixture.buildPlugin(false).insert(linkedMap("email", "alice@example.com", "code", "A001"), fixture.context);
    assertTrue(result.isFailure());
    assertEquals(failure, result.getCause());
    assertEquals(0, fixture.tail.insertCalls);
  }

  @Test
  void updateRejectsEitherIndependentUniqueConflictAndExcludesOnlyItself() {
    var candidate = linkedMap("email", "next@example.com", "code", "NEXT");
    for (var existing : List.of(linkedMap("id", 1L, "email", "next@example.com", "code", "NEXT"),
        linkedMap("id", 2L, "email", "next@example.com", "code", "OTHER"),
        linkedMap("id", 2L, "email", "other@example.com", "code", "NEXT"))) {
      var fixture = new Fixture(List.of("id"));
      fixture.count(where -> matches(where, existing) ? 1L : 0L);
      var result = fixture.buildPlugin(false).update(and(eq("id", 1L)), candidate, fixture.context);
      boolean conflict = !Long.valueOf(1L).equals(existing.get("id"));
      assertEquals(!conflict, result.isSuccess());
      assertEquals(conflict ? 0 : 1, fixture.tail.updateCalls);
      if (conflict) assertTrue(result.getCause() instanceof UniqueConstraintViolationException);
    }
  }

  @Test
  void insertWithModelLevelUniqueConstraintBuildsAndCondition() {
    var fixture = new Fixture(List.of("id"));
    fixture.addUniqueConstraint("project_node_code", "projectId", "nodeCode");
    DataModelPlugin plugin = fixture.buildPlugin(false);
    fixture.count(where -> 0L);

    Try<Record> result = plugin.insert(linkedMap("projectId", 100L, "nodeCode", "N001"), fixture.context);

    assertTrue(result.isSuccess());
    assertEquals(
        linkedMap("projectId", 100L, "nodeCode", "N001"),
        fixture.lastCountWhere
    );
  }

  @Test
  void insertChecksFieldUniquenessAndCompositeUniquenessIndependently() {
    var fixture = new Fixture(List.of("id"));
    fixture.addUniqueConstraint("project_node_code", "projectId", "nodeCode");
    DataModelPlugin plugin = fixture.buildPlugin(false);
    fixture.count(where -> 0L);

    Try<Record> result = plugin.insert(
        linkedMap("email", "alice@example.com", "projectId", 100L, "nodeCode", "N001"),
        fixture.context
    );

    assertTrue(result.isSuccess());
    assertEquals(1, fixture.tail.insertCalls);
    for (var existing : List.of(linkedMap("email", "alice@example.com", "projectId", 200L, "nodeCode", "OTHER"),
        linkedMap("email", "other@example.com", "projectId", 100L, "nodeCode", "N001"))) {
      var conflict = new Fixture(List.of("id"));
      conflict.addUniqueConstraint("project_node_code", "projectId", "nodeCode");
      conflict.count(where -> matches(where, existing) ? 1L : 0L);
      var rejected = conflict.buildPlugin(false).insert(
          linkedMap("email", "alice@example.com", "projectId", 100L, "nodeCode", "N001"), conflict.context);
      assertTrue(rejected.isFailure());
      assertTrue(rejected.getCause() instanceof UniqueConstraintViolationException);
      assertEquals(0, conflict.tail.insertCalls);
    }
  }

  @Test
  void batchInsertRejectsSameBatchModelLevelUniqueConstraintConflictBeforeDbCheck() {
    var fixture = new Fixture(List.of("id"));
    fixture.addUniqueConstraint("project_node_code", "projectId", "nodeCode");
    DataModelPlugin plugin = fixture.buildPlugin(false);

    Try<RecordList> result = plugin.batchInsert(
        List.of(
            linkedMap("projectId", 100L, "nodeCode", "N001"),
            linkedMap("projectId", 100L, "nodeCode", "N001")
        ),
        fixture.context
    );

    assertFalse(result.isSuccess());
    assertTrue(result.getCause() instanceof UniqueConstraintViolationException);
    assertEquals(
        List.of("projectId", "nodeCode"),
        ((UniqueConstraintViolationException) result.getCause()).getConflictFields()
    );
    verify(fixture.source, never()).withoutPlugins();
  }

  @Test
  void batchInsertPreservesDistinctRows() {
    var fixture = new Fixture(List.of("id"));
    DataModelPlugin plugin = fixture.buildPlugin(false);
    fixture.count(where -> 0L);

    Try<RecordList> result = plugin.batchInsert(
        List.of(
            linkedMap("email", "alice@example.com"),
            linkedMap("email", "bob@example.com")
        ),
        fixture.context
    );

    assertTrue(result.isSuccess());
    assertEquals(1, fixture.tail.batchInsertCalls);
    assertEquals(0, fixture.tail.insertCalls);
    assertEquals(2, fixture.tail.batchInsertedRows.size());
    assertEquals(List.of(linkedMap("email", "alice@example.com"), linkedMap("email", "bob@example.com")),
        result.get().stream().map(record -> record.get("email")).map(email -> linkedMap("email", email)).toList());
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void mixedScopesKeepDeletedGlobalKeysButReleaseActiveKeys(boolean batch) {
    var fixture = new Fixture(List.of("id"));
    fixture.hasPlugin("LogicalDelete");
    when(fixture.source.getField("code").orElseThrow().getUniquenessScope())
        .thenReturn(com.ouroboros.data.model.UniquenessScope.ACTIVE_RECORDS);
    fixture.count(where -> "old@example.com".equals(where.get("email")) ? 1L : 0L);
    when(fixture.search.count(anyMap())).thenReturn(Try.success(0L));
    fixture.query(RecordList.of(List.of(linkedMap("email", "old@example.com", "code", "old", "isDeleted", true))));
    var plugin = fixture.buildPlugin(true);
    var allowed = linkedMap("email", "new@example.com", "code", "old");
    var rejected = linkedMap("email", "old@example.com", "code", "new");

    var success = batch ? plugin.batchInsert(List.of(allowed), fixture.context) : plugin.insert(allowed, fixture.context);
    var conflict = batch ? plugin.batchInsert(List.of(rejected), fixture.context) : plugin.insert(rejected, fixture.context);

    assertTrue(success.isSuccess());
    assertTrue(conflict.isFailure());
    assertEquals(List.of("email"), org.junit.jupiter.api.Assertions.assertInstanceOf(
        UniqueConstraintViolationException.class, conflict.getCause()).getConflictFields());
  }

  @Test
  void batchInsertReturnsCountFailureAndDoesNotCallTailBatchInsert() {
    var fixture = new Fixture(List.of("id"));
    DataModelPlugin plugin = fixture.buildPlugin(false);
    var failure = new IllegalStateException("count failed");
    fixture.countFailure(failure);

    Try<RecordList> result = plugin.batchInsert(
        List.of(
            linkedMap("email", "alice@example.com"),
            linkedMap("email", "bob@example.com")
        ),
        fixture.context
    );

    assertFalse(result.isSuccess());
    assertEquals(failure, result.getCause());
    assertEquals(0, fixture.tail.batchInsertCalls);
  }

  @Test
  void updateWithSinglePrimaryKeyKeepsExistingExclusionCondition() {
    var fixture = new Fixture(List.of("id"));
    DataModelPlugin plugin = fixture.buildPlugin(false);
    fixture.count(where -> 0L);

    Try<Long> result = plugin.update(
        and(eq("id", 1L)),
        linkedMap("email", "bob@example.com"),
        fixture.context
    );

    assertTrue(result.isSuccess());
    assertEquals(
        linkedMap(
            "and",
            linkedMap(
                "id", linkedMap("!=", 1L),
                "email", "bob@example.com"
            )
        ),
        fixture.lastCountWhere
    );
  }

  @Test
  void updateReturnsCountFailureAndDoesNotCallTailUpdate() {
    var fixture = new Fixture(List.of("id"));
    DataModelPlugin plugin = fixture.buildPlugin(false);
    var failure = new IllegalStateException("count failed");
    fixture.countFailure(failure);

    Try<Long> result = plugin.update(
        and(eq("id", 1L)),
        linkedMap("email", "bob@example.com"),
        fixture.context
    );

    assertFalse(result.isSuccess());
    assertEquals(failure, result.getCause());
    assertEquals(0, fixture.tail.updateCalls);
  }

  @Test
  void updateWithCompositePrimaryKeyExcludesOnlyTheSamePrimaryKeyTuple() {
    var fixture = new Fixture(List.of("id", "tenant"));
    DataModelPlugin plugin = fixture.buildPlugin(false);
    fixture.count(where -> 0L);

    Try<Long> result = plugin.update(
        and(eq("id", 1L), eq("tenant", "T1")),
        linkedMap("email", "bob@example.com"),
        fixture.context
    );

    assertTrue(result.isSuccess());
    assertEquals(
        linkedMap(
            "and",
            linkedMap(
                "or", List.of(
                    linkedMap("id", linkedMap("!=", 1L)),
                    linkedMap("tenant", linkedMap("!=", "T1"))
                ),
                "email", "bob@example.com"
            )
        ),
        fixture.lastCountWhere
    );
  }

  @Test
  void updateCanClearNullableValueInCompositeUniqueConstraint() {
    var fixture = new Fixture(List.of("id"));
    fixture.addUniqueConstraint("template_name", "tenantId", "activeTemplateKey");
    DataModelPlugin plugin = fixture.buildPlugin(false);
    fixture.count(where -> 0L);

    Try<Long> result = plugin.update(
        and(eq("id", 1L)), linkedMap("tenantId", 1L, "activeTemplateKey", null), fixture.context);

    assertTrue(result.isSuccess());
    assertEquals(1, fixture.tail.updateCalls);
    assertEquals(linkedMap("and", linkedMap("id", linkedMap("!=", 1L),
        "tenantId", 1L, "activeTemplateKey", null)), fixture.lastCountWhere);
  }

  @Test
  void partialUpdateLoadsNullableUniqueValuesWithoutLosingNull() {
    var fixture = new Fixture(List.of("id"));
    fixture.addUniqueConstraint("tenant_email", "tenant", "email");
    fixture.addUniqueConstraint("tenant_code", "tenant", "code");
    var plugin = fixture.buildPlugin(false);
    fixture.query(RecordList.of(List.of(linkedMap("tenant", "A", "email", "first@example.com", "code", null))));
    fixture.count(where -> 0L);

    var result = plugin.update(and(eq("id", 1L)), linkedMap("email", "next@example.com"), fixture.context);

    assertTrue(result.isSuccess(), () -> result.isFailure() ? result.getCause().toString() : "");
    assertEquals(1, fixture.tail.updateCalls);
    assertEquals(linkedMap("id", 1L), fixture.lastQueryWhere);
    assertEquals(2, fixture.countCalls);
  }

  @Test
  void declaredDistinctNullsAllowIndependentRowsAndBatchRows() {
    var fixture = new Fixture(List.of("id"));
    fixture.addUniqueConstraint("active_claim", "projectId", "nodeCode");
    fixture.uniqueConstraints.getLast().setExtraProp("nullsDistinct", true);
    var plugin = fixture.buildPlugin(false);
    fixture.count(where -> 1L);
    assertTrue(plugin.insert(linkedMap("projectId", 1L, "nodeCode", null), fixture.context).isSuccess());
    assertTrue(plugin.batchInsert(List.of(linkedMap("projectId", 1L, "nodeCode", null),
        linkedMap("projectId", 1L, "nodeCode", null)), fixture.context).isSuccess());
  }

  @Test
  void declaredDistinctNullsAllowReleaseAndPartialUpdateOfAnAlreadyReleasedRow() {
    var fixture = new Fixture(List.of("id"));
    fixture.addUniqueConstraint("active_claim", "projectId", "nodeCode");
    fixture.uniqueConstraints.getLast().setExtraProp("nullsDistinct", true);
    var plugin = fixture.buildPlugin(false);
    fixture.count(where -> 1L);
    assertTrue(plugin.update(and(eq("id", 1L)), linkedMap("projectId", 1L, "nodeCode", null), fixture.context).isSuccess());
    fixture.query(RecordList.of(List.of(linkedMap("projectId", 1L, "nodeCode", null))));
    assertTrue(plugin.update(and(eq("id", 1L)), linkedMap("projectId", 2L), fixture.context).isSuccess());
    assertEquals(2, fixture.tail.updateCalls);
  }

  @Test
  void declaredDistinctNullsStillRejectRealClaimsAndKeepDefaultNullEquality() {
    var fixture = new Fixture(List.of("id"));
    fixture.addUniqueConstraint("active_claim", "projectId", "nodeCode");
    fixture.uniqueConstraints.getLast().setExtraProp("nullsDistinct", true);
    fixture.count(where -> 1L);
    var distinct = fixture.buildPlugin(false);
    assertTrue(distinct.insert(linkedMap("projectId", 1L, "nodeCode", "claimed"), fixture.context).isFailure());
    assertTrue(distinct.update(and(eq("id", 1L)), linkedMap("projectId", 1L, "nodeCode", "claimed"), fixture.context).isFailure());
    fixture.uniqueConstraints.getLast().setExtraProp("nullsDistinct", false);
    var existingSemantics = fixture.buildPlugin(false);
    assertTrue(existingSemantics.insert(linkedMap("projectId", 1L, "nodeCode", null), fixture.context).isFailure());
  }

  @Test
  void typedDistinctNullDeclarationControlsRuntimeInsertBehavior() {
    var fixture = new Fixture(List.of("id"));
    var annotation = NullableClaimModel.class.getAnnotation(com.ouroboros.data.annotation.UniqueConstraint.class);
    var decorated = new com.ouroboros.data.model.decorators.typed.UniqueConstraintAnnotationDecorator()
        .decorate(NullableClaimModel.class, annotation, new com.ouroboros.data.model.DataModelMeta(), null);
    fixture.uniqueConstraints.addAll(decorated.getUniqueConstraints());
    fixture.count(where -> 1L);
    var plugin = fixture.buildPlugin(false);

    assertTrue(plugin.insert(linkedMap("projectId", 1L, "nodeCode", null), fixture.context).isSuccess());
    assertTrue(plugin.insert(linkedMap("projectId", 1L, "nodeCode", "claimed"), fixture.context).isFailure());
    assertEquals(1, fixture.tail.insertCalls);
    assertEquals(1, fixture.countCalls);
  }

  @Test
  void distinctNullDoesNotHideAnIncompleteUpdatedConstraint() {
    var fixture = new Fixture(List.of("id"));
    fixture.addUniqueConstraint("active_claim", "projectId", "nodeCode");
    fixture.uniqueConstraints.getLast().setExtraProp("nullsDistinct", true);
    fixture.query(RecordList.of(List.of(linkedMap("nodeCode", "claimed"))));
    var result = fixture.buildPlugin(false).update(and(eq("id", 1L)), linkedMap("nodeCode", null), fixture.context);

    assertTrue(result.isFailure());
    assertTrue(result.getCause() instanceof InvalidStatementException);
    assertEquals(0, fixture.tail.updateCalls);
  }

  @Test
  void distinctNullRowsCannotIdentifyAnUpdateWithoutPrimaryKey() {
    var fixture = new Fixture(List.of("id"));
    fixture.addUniqueConstraint("active_claim", "projectId", "nodeCode");
    fixture.uniqueConstraints.getLast().setExtraProp("nullsDistinct", true);
    var nullCode = SExpression.<Boolean>create(Operators.EQ, SExpression.field("nodeCode"),
        SExpression.create(Operators.CONSTANT, (Object) null));
    var result = fixture.buildPlugin(false).update(and(eq("projectId", 1L), nullCode),
        linkedMap("nodeCode", "claimed"), fixture.context);

    assertTrue(result.isFailure());
    assertTrue(result.getCause() instanceof InvalidStatementException);
    assertEquals(0, fixture.tail.updateCalls);
  }

  @Test
  void distinctNullSkipsOnlyItsOwnConstraintInMixedBatchAndDiagnostics() {
    var fixture = new Fixture(List.of("id"));
    fixture.addUniqueConstraint("active_claim", "projectId", "nodeCode");
    fixture.uniqueConstraints.getLast().setExtraProp("nullsDistinct", true);
    fixture.count(where -> 1L);
    fixture.query(RecordList.of(List.of(linkedMap("projectId", 1L, "nodeCode", null, "email", "a@example.com"))));
    var plugin = fixture.buildPlugin(true);
    var result = plugin.insert(linkedMap("projectId", 1L, "nodeCode", null, "email", "a@example.com"), fixture.context);
    assertTrue(result.isFailure());
    assertEquals("字段email存在重复值，请检查!", result.getCause().getMessage());
    assertTrue(plugin.batchInsert(List.of(linkedMap("projectId", 1L, "nodeCode", null, "email", "a@example.com"),
        linkedMap("projectId", 1L, "nodeCode", null, "email", "a@example.com")), fixture.context).isFailure());
    assertTrue(plugin.batchInsert(List.of(linkedMap("projectId", 1L, "nodeCode", "claimed"),
        linkedMap("projectId", 1L, "nodeCode", "claimed")), fixture.context).isFailure());
  }

  @com.ouroboros.data.annotation.UniqueConstraint(fields = {"projectId", "nodeCode"}, nullsDistinct = true)
  private static class NullableClaimModel {
  }

  @Test
  void updateWithModelLevelUniqueConstraintUsesPrimaryKeyToExcludeSelf() {
    var fixture = new Fixture(List.of("id"));
    fixture.addUniqueConstraint("project_node_code", "projectId", "nodeCode");
    DataModelPlugin plugin = fixture.buildPlugin(false);
    fixture.count(where -> 0L);

    Try<Long> result = plugin.update(
        and(eq("id", 1L)),
        linkedMap("projectId", 100L, "nodeCode", "N002"),
        fixture.context
    );

    assertTrue(result.isSuccess());
    assertEquals(
        linkedMap(
            "and",
            linkedMap(
                "id", linkedMap("!=", 1L),
                "projectId", 100L,
                "nodeCode", "N002"
            )
        ),
        fixture.lastCountWhere
    );
  }

  @Test
  void updateWithModelLevelUniqueConstraintUsesCurrentRecordWhenPrimaryKeyIdentifiesPartialUpdate() {
    var fixture = new Fixture(List.of("id"));
    fixture.addUniqueConstraint("project_node_code", "projectId", "nodeCode");
    DataModelPlugin plugin = fixture.buildPlugin(false);
    fixture.query(RecordList.of(List.of(
        linkedMap("projectId", 100L, "nodeCode", "N001")
    )));
    fixture.count(where -> 0L);

    Try<Long> result = plugin.update(
        and(eq("id", 1L)),
        linkedMap("nodeCode", "N002"),
        fixture.context
    );

    assertTrue(result.isSuccess());
    assertEquals(linkedMap("id", 1L), fixture.lastQueryWhere);
    assertEquals(
        linkedMap(
            "and",
            linkedMap(
                "id", linkedMap("!=", 1L),
                "projectId", 100L,
                "nodeCode", "N002"
            )
        ),
        fixture.lastCountWhere
    );
  }

  @Test
  void updateWithModelLevelUniqueConstraintCanUseCompleteOldUniqueWhereAsIdentity() {
    var fixture = new Fixture(List.of("id"));
    fixture.addUniqueConstraint("project_node_code", "projectId", "nodeCode");
    DataModelPlugin plugin = fixture.buildPlugin(false);
    fixture.count(where -> 0L);

    Try<Long> result = plugin.update(
        and(eq("projectId", 100L), eq("nodeCode", "N001")),
        linkedMap("nodeCode", "N002"),
        fixture.context
    );

    assertTrue(result.isSuccess());
    assertEquals(
        linkedMap(
            "and",
            linkedMap(
                "or", List.of(
                    linkedMap("projectId", linkedMap("!=", 100L)),
                    linkedMap("nodeCode", linkedMap("!=", "N001"))
                ),
                "projectId", 100L,
                "nodeCode", "N002"
            )
        ),
        fixture.lastCountWhere
    );
  }

  @Test
  void updateWithModelLevelUniqueConstraintRejectsIncompleteIdentityWithoutPrimaryKey() {
    var fixture = new Fixture(List.of("id"));
    fixture.addUniqueConstraint("project_node_code", "projectId", "nodeCode");
    DataModelPlugin plugin = fixture.buildPlugin(false);

    Try<Long> result = plugin.update(
        and(eq("projectId", 100L)),
        linkedMap("nodeCode", "N002"),
        fixture.context
    );

    assertFalse(result.isSuccess());
    assertTrue(result.getCause() instanceof InvalidStatementException);
    verify(fixture.source, never()).withoutPlugins();
  }

  @Test
  void updateRejectsNonEqualityPrimaryKeyWhenUniqueFieldChanges() {
    var fixture = new Fixture(List.of("id"));
    DataModelPlugin plugin = fixture.buildPlugin(false);

    Try<Long> result = plugin.update(
        SExpression.create(Operators.IN, SExpression.field("id"), SExpression.constant(List.of(1L, 2L))),
        linkedMap("email", "bob@example.com"),
        fixture.context
    );

    assertFalse(result.isSuccess());
    assertTrue(result.getCause() instanceof InvalidStatementException);
    verify(fixture.source, never()).withoutPlugins();
  }

  @Test
  void allRecordsScopeKeepsExistingUpdateConditionUnwrapped() {
    var fixture = new Fixture(List.of("id"));
    DataModelPlugin plugin = fixture.buildPlugin(false);
    fixture.count(where -> 0L);

    Try<Long> result = plugin.update(
        and(eq("id", 1L)),
        linkedMap("email", "bob@example.com"),
        fixture.context
    );

    assertTrue(result.isSuccess());
    assertEquals(
        linkedMap(
            "and",
            linkedMap(
                "id", linkedMap("!=", 1L),
                "email", "bob@example.com"
            )
        ),
        fixture.lastCountWhere
    );
  }

  private static DataModelField field(String name, boolean unique, ValueType<?> valueType) {
    DataModelField field = mock(DataModelField.class);
    when(field.getName()).thenReturn(name);
    when(field.getLabel()).thenReturn(name);
    when(field.getIsUnique()).thenReturn(unique);
    when(field.getValueType()).thenReturn((ValueType) valueType);
    return field;
  }

  private static Map<String, Object> linkedMap(Object... pairs) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      map.put((String) pairs[i], pairs[i + 1]);
    }
    return map;
  }

  private static boolean matches(Object condition, Map<String, Object> record) {
    if (condition instanceof List<?> conjunction) return conjunction.stream().allMatch(part -> matches(part, record));
    if (!(condition instanceof Map<?, ?> fields)) throw new IllegalArgumentException("测试条件无效");
    for (var entry : fields.entrySet()) {
      if ("or".equals(entry.getKey()) && entry.getValue() instanceof List<?> branches) {
        if (branches.stream().noneMatch(part -> matches(part, record))) return false;
      } else if ("and".equals(entry.getKey())) {
        if (!matches(entry.getValue(), record)) return false;
      } else if (entry.getValue() instanceof Map<?, ?> comparison && comparison.containsKey("!=")) {
        if (java.util.Objects.equals(record.get(entry.getKey()), comparison.get("!="))) return false;
      } else if (!java.util.Objects.equals(record.get(entry.getKey()), entry.getValue())) return false;
    }
    return true;
  }

  private static SExpression<Boolean> eq(String field, Object value) {
    return SExpression.create(Operators.EQ, SExpression.field(field), SExpression.constant(value));
  }

  private static SExpression<Boolean> and(SExpression<?>... expressions) {
    return SExpression.create(Operators.AND, (Object[]) expressions);
  }

  private static Collection<PluginDescriptor> pluginDescriptorCollection(String pluginName) {
    return argThat(descriptors -> {
      if (descriptors == null || descriptors.size() != 1) {
        return false;
      }
      PluginDescriptor descriptor = descriptors.iterator().next();
      return pluginName.equals(descriptor.getName())
          && descriptor.getConfig().isEmpty();
    });
  }

  private static Map<String, Object> copyStringObjectMap(Object value) {
    Map<String, Object> copy = new LinkedHashMap<>();
    if (value instanceof Map<?, ?> raw) {
      for (Map.Entry<?, ?> entry : raw.entrySet()) {
        if (entry.getKey() instanceof String key) {
          copy.put(key, entry.getValue());
        }
      }
    }
    return copy;
  }

  private static final class Fixture {
    private final DataModel source = mock(DataModel.class);
    private final DataModel stripped = mock(DataModel.class);
    private final DataModel search = mock(DataModel.class);
    private final RecordingTailPlugin tail = new RecordingTailPlugin();
    private final DataModelPluginContext context = PluginTestContexts.withNext(tail);
    private final List<DataModelUniqueConstraintMeta> uniqueConstraints = new ArrayList<>();
    private Map<String, Object> lastCountWhere;
    private Map<String, Object> lastQueryWhere;
    private int countCalls;
    private int queryCalls;

    private Fixture(List<String> primaryKeys) {
      var id = field("id", false, new LongValue());
      var tenant = field("tenant", false, new StringValue());
      var email = field("email", true, new StringValue());
      var code = field("code", true, new StringValue());
      var projectId = field("projectId", false, new LongValue());
      var nodeCode = field("nodeCode", false, new StringValue());
      var isDeleted = field("isDeleted", false, new BooleanValue());
      var fields = List.of(id, tenant, email, code, projectId, nodeCode, isDeleted);
      Map<String, DataModelField> fieldsByName = new LinkedHashMap<>();
      fieldsByName.put("id", id);
      fieldsByName.put("tenant", tenant);
      fieldsByName.put("email", email);
      fieldsByName.put("code", code);
      fieldsByName.put("projectId", projectId);
      fieldsByName.put("nodeCode", nodeCode);
      fieldsByName.put("isDeleted", isDeleted);

      when(source.getFullName()).thenReturn("demo.User");
      when(source.getFields()).thenReturn(fields);
      when(source.getUniqueConstraints()).thenReturn(uniqueConstraints);
      when(source.getExtraProp(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
      when(source.getPrimaryKeys()).thenReturn(
          primaryKeys.stream().map(fieldsByName::get).toList()
      );
      when(source.withoutPlugins()).thenReturn(stripped);
      when(stripped.withPlugins(org.mockito.ArgumentMatchers.anyCollection())).thenReturn(search);
      when(source.getField(org.mockito.ArgumentMatchers.anyString())).thenAnswer(invocation ->
          Optional.ofNullable(fieldsByName.get(invocation.getArgument(0)))
      );
    }

    private void addUniqueConstraint(String name, String... fields) {
      DataModelUniqueConstraintMeta constraint = new DataModelUniqueConstraintMeta();
      constraint.setName(name);
      constraint.setFields(List.of(fields));
      uniqueConstraints.add(constraint);
    }

    private void hasPlugin(String pluginName) {
      when(source.hasPlugin(pluginName)).thenReturn(true);
    }

    private DataModelPlugin buildPlugin(boolean notifyDuplicateFields) {
      return new DuplicateDataCheckerDataModelPlugin.Builder()
          .build(source, Collections.singletonMap("notifyDuplicateFields", notifyDuplicateFields))
          .get();
    }

    private void count(Function<Map<String, Object>, Long> counter) {
      var answer = (org.mockito.stubbing.Answer<Try<Long>>) invocation -> {
        countCalls++;
        lastCountWhere = new LinkedHashMap<>(invocation.getArgument(0));
        return Try.success(counter.apply(lastCountWhere));
      };
      when(stripped.count(anyMap())).thenAnswer(answer);
      when(search.count(anyMap())).thenAnswer(answer);
    }

    private void countFailure(Throwable failure) {
      var answer = (org.mockito.stubbing.Answer<Try<Long>>) invocation -> {
        countCalls++;
        lastCountWhere = new LinkedHashMap<>(invocation.getArgument(0));
        return Try.failure(failure);
      };
      when(stripped.count(anyMap())).thenAnswer(answer);
      when(search.count(anyMap())).thenAnswer(answer);
    }

    private void query(RecordList records) {
      var answer = (org.mockito.stubbing.Answer<Try<RecordList>>) invocation -> {
        queryCalls++;
        Map<String, Object> statement = copyStringObjectMap(invocation.getArgument(0));
        lastQueryWhere = copyStringObjectMap(statement.get("WHERE"));
        return Try.success(records);
      };
      when(stripped.query(anyMap())).thenAnswer(answer);
      when(search.query(anyMap())).thenAnswer(answer);
    }
  }

  private static final class RecordingTailPlugin implements DataModelPlugin {
    private int insertCalls;
    private int batchInsertCalls;
    private int updateCalls;
    private List<Map<String, Object>> batchInsertedRows = Collections.emptyList();

    @Override
    public Try<Record> insert(Map<String, Object> data, DataModelPluginContext context) {
      insertCalls++;
      return Try.success(Record.of(data));
    }

    @Override
    public Try<RecordList> batchInsert(List<Map<String, Object>> dataList, DataModelPluginContext context) {
      batchInsertCalls++;
      batchInsertedRows = dataList.stream()
          .map(LinkedHashMap<String, Object>::new)
          .collect(java.util.stream.Collectors.toList());
      return Try.success(RecordList.of(batchInsertedRows));
    }

    @Override
    public Try<Long> update(SExpression<Boolean> where, Map<String, Object> data, DataModelPluginContext context) {
      updateCalls++;
      return Try.success(1L);
    }

    @Override
    public Try<RecordList> query(QueryStatement statement, DataModelPluginContext context) {
      return Try.success(RecordList.empty());
    }
  }
}
