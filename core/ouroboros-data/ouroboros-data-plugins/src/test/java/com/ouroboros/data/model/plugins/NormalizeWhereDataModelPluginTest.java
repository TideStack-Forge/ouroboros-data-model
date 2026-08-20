package com.ouroboros.data.model.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.DataModelPluginBuilder;
import com.ouroboros.data.model.DataModelPluginContext;
import com.ouroboros.data.model.ValueType;
import com.ouroboros.data.record.RecordList;

class NormalizeWhereDataModelPluginTest {

  @Test
  void normalizeWhereShouldFilterNullEmptyAndRecurseForMapCollection() {
    var pluginDataModel = mock(DataModel.class);
    var nameField = mock(DataModelField.class);
    var physical = mock(ValueType.class);
    when(physical.isPhysical()).thenReturn(true);
    when(nameField.getName()).thenReturn("name");
    when(nameField.getValueType()).thenReturn((ValueType) physical);
    when(pluginDataModel.getFields()).thenReturn(Arrays.asList(nameField));

    var builder = new NormalizeWhereDataModelPlugin.Builder();
    assertTrue(builder.support("NormalizeWhere"));

    var config = new HashMap<String, Object>();
    config.put("removeEmptyValue", true);
    config.put("removeNullValue", true);

    var plugin = builder.build(pluginDataModel, config).get();
    var tail = new TailPlugin();
    var context = PluginTestContexts.withNext(tail);

    SExpression<Boolean> where = SExpression.create(
        Operators.AND,
        SExpression.create(Operators.EQ, SExpression.field("name"), constant("")),
        SExpression.create(Operators.EQ, SExpression.field("unknown"), constant("drop")),
        SExpression.create(Operators.OR,
            SExpression.create(Operators.EQ, SExpression.field("name"), constant("alice")),
            SExpression.create(Operators.EQ, SExpression.field("name"), constant(null))),
        SExpression.create(Operators.EQ, SExpression.field("name"), constant("bob")));

    var updateResult = plugin.update(where, Collections.<String, Object>singletonMap("x", 1), context);
    assertTrue(updateResult.isSuccess());

    var normalizedWhere = tail.lastUpdateWhere;
    assertTrue(!containsField(normalizedWhere, "unknown"));
    assertTrue(!containsConstant(normalizedWhere, ""));
    assertTrue(!containsConstant(normalizedWhere, null));
    assertTrue(containsConstant(normalizedWhere, "alice"));
    assertTrue(containsConstant(normalizedWhere, "bob"));
  }

  @Test
  void queryDeleteCountShouldPassNormalizedWhereAndBuilderDefaults() {
    var pluginDataModel = mock(DataModel.class);
    var idField = mock(DataModelField.class);
    var physical = mock(ValueType.class);
    when(physical.isPhysical()).thenReturn(true);
    when(idField.getName()).thenReturn("id");
    when(idField.getValueType()).thenReturn((ValueType) physical);
    when(pluginDataModel.getFields()).thenReturn(Arrays.asList(idField));

    var plugin = new NormalizeWhereDataModelPlugin.Builder()
        .build(pluginDataModel, Collections.<String, Object>emptyMap())
        .get();
    var tail = new TailPlugin();
    var context = PluginTestContexts.withNext(tail);

    SExpression<Boolean> where = SExpression.create(
        Operators.AND,
        SExpression.create(Operators.EQ, SExpression.field("id"), constant(1L)),
        SExpression.create(Operators.EQ, SExpression.field("drop"), constant("")));

    QueryStatement queryStatement = QueryStatement.builder()
        .where(where)
        .limit(1)
        .build();

    assertTrue(plugin.delete(where, context).isSuccess());
    assertTrue(plugin.count(QueryStatement.builder().where(where).build(), context).isSuccess());
    assertTrue(plugin.query(queryStatement, context).isSuccess());

    assertTrue(containsField(tail.lastDeleteWhere, "id"));
    assertTrue(containsConstant(tail.lastDeleteWhere, 1L));
    assertTrue(containsField(tail.lastCountStatement.getWhere(), "id"));
    assertTrue(containsConstant(tail.lastCountStatement.getWhere(), 1L));
    assertTrue(containsField(tail.lastQueryStatement.getWhere(), "id"));
    assertTrue(!containsField(tail.lastQueryStatement.getWhere(), "drop"));
  }

  @Test
  void normalizeWhereShouldDropFieldWhenNestedOperatorsBecomeEmpty() {
    var pluginDataModel = mock(DataModel.class);
    var createdAtField = mock(DataModelField.class);
    var physical = mock(ValueType.class);
    when(physical.isPhysical()).thenReturn(true);
    when(createdAtField.getName()).thenReturn("createdAt");
    when(createdAtField.getValueType()).thenReturn((ValueType) physical);
    when(pluginDataModel.getFields()).thenReturn(Arrays.asList(createdAtField));

    var config = new HashMap<String, Object>();
    config.put("removeEmptyValue", true);

    var plugin = new NormalizeWhereDataModelPlugin.Builder()
        .build(pluginDataModel, config)
        .get();
    var tail = new TailPlugin();
    var context = PluginTestContexts.withNext(tail);

    SExpression<Boolean> where = SExpression.create(
        Operators.EQ,
        SExpression.field("createdAt"),
        constant(""));

    assertTrue(plugin.count(QueryStatement.builder().where(where).build(), context).isSuccess());
    assertTrue(tail.lastCountStatement.getWhere().isEmpty());
  }

  @Test
  void typedQueryShouldUseNormalizedPluginPath() {
    var plugin = new NormalizeWhereDataModelPlugin(
        Collections.singleton("id"), true, true);
    var tail = new TailPlugin();
    var context = PluginTestContexts.withNext(tail);
    QueryStatement statement = QueryStatement.builder()
        .where(SExpression.create(
            Operators.EQ, SExpression.field("id"), constant(1L)))
        .build();

    Try<RecordList> result = plugin.query(statement, context);

    assertTrue(result.isSuccess());
    assertEquals(statement.getWhere(), tail.lastQueryStatement.getWhere());
  }

  private static SExpression<?> constant(Object value) {
    return SExpression.create(Operators.CONSTANT, value);
  }

  private static boolean containsField(SExpression<?> expression, String fieldName) {
    return contains(expression, node -> node.getOperator() == Operators.FIELD
        && node.getParams().contains(fieldName));
  }

  private static boolean containsConstant(SExpression<?> expression, Object value) {
    return contains(expression, node -> node.getOperator() == Operators.CONSTANT
        && !node.getParams().isEmpty()
        && java.util.Objects.equals(node.getParam(0), value));
  }

  private static boolean contains(SExpression<?> expression, java.util.function.Predicate<SExpression<?>> predicate) {
    if (expression == null || expression.isEmpty()) {
      return false;
    }
    final boolean[] found = new boolean[] {false};
    expression.walk((node, context) -> {
      if (predicate.test(node)) {
        found[0] = true;
      }
    });
    return found[0];
  }

  private static final class TailPlugin implements com.ouroboros.data.model.DataModelPlugin {
    private SExpression<Boolean> lastUpdateWhere;
    private SExpression<Boolean> lastDeleteWhere;
    private QueryStatement lastCountStatement;
    private QueryStatement lastQueryStatement;

    @Override
    public Try<Long> update(SExpression<Boolean> where, Map<String, Object> data, DataModelPluginContext context) {
      this.lastUpdateWhere = where;
      return Try.success(1L);
    }

    @Override
    public Try<Long> delete(SExpression<Boolean> where, DataModelPluginContext context) {
      this.lastDeleteWhere = where;
      return Try.success(1L);
    }

    @Override
    public Try<Long> count(QueryStatement statement, DataModelPluginContext context) {
      this.lastCountStatement = statement;
      return Try.success(1L);
    }

    @Override
    public Try<RecordList> query(QueryStatement statement, DataModelPluginContext context) {
      this.lastQueryStatement = statement;
      return Try.success(RecordList.empty());
    }
  }
}
