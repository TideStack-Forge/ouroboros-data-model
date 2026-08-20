package com.ouroboros.data.model.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.DataModelPluginContext;
import com.ouroboros.data.model.ValueType;
import com.ouroboros.data.record.RecordList;
import com.ouroboros.data.validation.Rule;

class UpdateWhenDataChangeDataModelPluginTest {

  @Test
  void builderSupportAndUpdateAddsQueryFreeChangedPredicate() {
    var physicalType = new StubValueType(true);
    var nameField = new StubDataModelField("name", physicalType);

    var dataModel = mock(DataModel.class);
    when(dataModel.getFields()).thenReturn(Arrays.asList(nameField));
    when(dataModel.getFullName()).thenReturn("demo.User");
    when(dataModel.getRawName()).thenReturn("demo_user");

    var builder = new UpdateWhenDataChangeDataModelPlugin.Builder();
    assertTrue(builder.support("UpdateWhenDataChange"));

    var plugin = builder.build(dataModel, Collections.<String, Object>emptyMap()).get();
    var tail = new TailPlugin();
    var context = PluginTestContexts.withNext(tail);

    SExpression<Boolean> where = SExpression.create(
        Operators.EQ,
        SExpression.field("id"),
        SExpression.constant(1L));

    var changed = plugin.update(where, Collections.<String, Object>singletonMap("name", "Bob"), context);

    assertTrue(changed.isSuccess());
    assertEquals(Long.valueOf(1L), changed.get());
    assertEquals(1, tail.updateCalls);
    assertEquals(0, tail.queryCalls);
    assertTrue(containsConstant(tail.lastUpdateWhere, 1L));
    assertTrue(containsConstant(tail.lastUpdateWhere, "Bob"));
    assertTrue(containsOperator(tail.lastUpdateWhere, Operators.NE));
    assertTrue(containsOperator(tail.lastUpdateWhere, Operators.IS_NULL));
  }

  @Test
  void nonPhysicalOrIrrelevantFieldsShouldNotTriggerUpdate() {
    var nonPhysicalType = new StubValueType(false);
    var virtualField = new StubDataModelField("virtualOnly", nonPhysicalType);

    var dataModel = mock(DataModel.class);
    when(dataModel.getFields()).thenReturn(Arrays.asList(virtualField));
    when(dataModel.getFullName()).thenReturn("demo.User");
    when(dataModel.getRawName()).thenReturn("demo_user");

    var plugin = new UpdateWhenDataChangeDataModelPlugin.Builder()
        .build(dataModel, Collections.<String, Object>emptyMap())
        .get();

    var tail = new TailPlugin();
    var context = PluginTestContexts.withNext(tail);

    SExpression<Boolean> where = SExpression.create(
        Operators.EQ,
        SExpression.field("id"),
        SExpression.constant(1L));
    var result = plugin.update(where, Collections.<String, Object>singletonMap("virtualOnly", "changed"), context);

    assertTrue(result.isSuccess());
    assertEquals(Long.valueOf(0L), result.get());
    assertEquals(0, tail.updateCalls);
    assertEquals(0, tail.queryCalls);
  }

  @Test
  void nullPatchShouldUseNullSafeIsNotNullChangedPredicate() {
    var physicalType = new StubValueType(true);
    var nameField = new StubDataModelField("name", physicalType);

    var dataModel = mock(DataModel.class);
    when(dataModel.getFields()).thenReturn(Arrays.asList(nameField));
    when(dataModel.getFullName()).thenReturn("demo.User");
    when(dataModel.getRawName()).thenReturn("demo_user");

    var plugin = new UpdateWhenDataChangeDataModelPlugin.Builder()
        .build(dataModel, Collections.<String, Object>emptyMap())
        .get();
    var tail = new TailPlugin();
    var context = PluginTestContexts.withNext(tail);

    SExpression<Boolean> where = SExpression.create(
        Operators.EQ,
        SExpression.field("id"),
        SExpression.constant(1L));

    var result = plugin.update(where, Collections.<String, Object>singletonMap("name", null), context);

    assertTrue(result.isSuccess());
    assertEquals(1, tail.updateCalls);
    assertEquals(0, tail.queryCalls);
    assertTrue(containsOperator(tail.lastUpdateWhere, Operators.IS_NOT_NULL));
  }

  private static final class TailPlugin implements com.ouroboros.data.model.DataModelPlugin {
    Try<RecordList> queryResult = Try.success(RecordList.empty());
    private int updateCalls;
    private int queryCalls;
    private SExpression<Boolean> lastUpdateWhere;

    @Override
    public Try<RecordList> query(QueryStatement statement, DataModelPluginContext context) {
      queryCalls++;
      return queryResult;
    }

    @Override
    public Try<Long> update(SExpression<Boolean> where, Map<String, Object> data, DataModelPluginContext context) {
      updateCalls++;
      lastUpdateWhere = where;
      return Try.success(1L);
    }
  }

  private static boolean containsOperator(SExpression<?> expression, com.querydsl.core.types.Operator operator) {
    return contains(expression, node -> node.getOperator() == operator);
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

  private static final class StubValueType implements ValueType<Object> {
    private final boolean physical;

    private StubValueType(boolean physical) {
      this.physical = physical;
    }

    @Override
    public String getName() {
      return "stub";
    }

    @Override
    public String getLabel() {
      return "stub";
    }

    @Override
    public Boolean isPhysical() {
      return physical;
    }

    @Override
    public Class<Object> getType() {
      return Object.class;
    }

    @Override
    public Object convert(Object value) {
      return value;
    }

    @Override
    public Object toPersistentValue(Object value) {
      return value;
    }
  }

  private static final class StubDataModelField implements DataModelField {
    private final String name;
    private final ValueType<?> valueType;

    private StubDataModelField(String name, ValueType<?> valueType) {
      this.name = name;
      this.valueType = valueType;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getLabel() {
      return name;
    }

    @Override
    public String getDescription() {
      return null;
    }

    @Override
    public String getType() {
      return null;
    }

    @Override
    public String getRawName() {
      return name;
    }

    @Override
    public String getRawType() {
      return null;
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
      return null;
    }

    @Override
    public Boolean getIsUnsigned() {
      return null;
    }

    @Override
    public Boolean getIsAutoIncrement() {
      return null;
    }

    @Override
    public Boolean getIsUnique() {
      return null;
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
  }
}
