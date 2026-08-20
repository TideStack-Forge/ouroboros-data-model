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
  void insertWithMultipleUniqueFieldsBuildsExistingOrCondition() {
    var fixture = new Fixture(List.of("id"));
    DataModelPlugin plugin = fixture.buildPlugin(false);
    fixture.count(where -> 0L);

    Try<Record> result = plugin.insert(linkedMap("email", "alice@example.com", "code", "A001"), fixture.context);

    assertTrue(result.isSuccess());
    assertEquals(
        linkedMap(
            "or",
            List.of(
                linkedMap("email", "alice@example.com"),
                linkedMap("code", "A001")
            )
        ),
        fixture.lastCountWhere
    );
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
  void insertKeepsFieldUniqueOrWhenModelLevelUniqueConstraintCoexists() {
    var fixture = new Fixture(List.of("id"));
    fixture.addUniqueConstraint("project_node_code", "projectId", "nodeCode");
    DataModelPlugin plugin = fixture.buildPlugin(false);
    fixture.count(where -> 0L);

    Try<Record> result = plugin.insert(
        linkedMap("email", "alice@example.com", "projectId", 100L, "nodeCode", "N001"),
        fixture.context
    );

    assertTrue(result.isSuccess());
    assertEquals(
        linkedMap(
            "or",
            List.of(
                linkedMap("email", "alice@example.com"),
                linkedMap("projectId", 100L, "nodeCode", "N001")
            )
        ),
        fixture.lastCountWhere
    );
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
  void batchInsertChecksDatabaseOnceThenUsesNextBatchInsert() {
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
    assertEquals(1, fixture.countCalls);
    assertEquals(1, fixture.tail.batchInsertCalls);
    assertEquals(0, fixture.tail.insertCalls);
    assertEquals(2, fixture.tail.batchInsertedRows.size());
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
