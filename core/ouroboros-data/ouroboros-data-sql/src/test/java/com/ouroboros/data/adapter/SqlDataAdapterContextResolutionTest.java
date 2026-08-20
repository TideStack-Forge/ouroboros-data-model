package com.ouroboros.data.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.MigrationStrategy;
import com.ouroboros.data.model.PrimaryKeyGenerator;
import com.ouroboros.data.model.ValueType;
import com.ouroboros.data.model.valuetypes.StringValue;
import com.ouroboros.data.record.Record;
import com.ouroboros.data.record.RecordList;
import com.ouroboros.data.transpile.TranspileContext;

import io.vavr.control.Try;

class SqlDataAdapterContextResolutionTest {

  @Test
  void shouldCoverContextResolutionHelpers() throws Exception {
    var ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:sql_context_test;MODE=MySQL;DB_CLOSE_DELAY=-1");
    ds.setUser("sa");
    ds.setPassword("");
    var adapter = new SqlDataAdapter(ds, "H2");

    var model = model();

    var extractPhysicalName = SqlDataAdapter.class.getDeclaredMethod("extractPhysicalName", String.class);
    extractPhysicalName.setAccessible(true);
    assertEquals("", extractPhysicalName.invoke(adapter, new Object[]{null}));
    assertEquals("orders", extractPhysicalName.invoke(adapter, "demo.orders"));

    var findField = SqlDataAdapter.class.getDeclaredMethod("findField", DataModel.class, String.class);
    findField.setAccessible(true);
    var byLogical = (Optional<?>) findField.invoke(adapter, model, "name");
    var byRaw = (Optional<?>) findField.invoke(adapter, model, "demo_name");
    assertTrue(byLogical.isPresent());
    assertTrue(byRaw.isPresent());

    var createModelBackedContext = SqlDataAdapter.class.getDeclaredMethod("createModelBackedContext", String.class, DataModel.class);
    createModelBackedContext.setAccessible(true);
    var ctx = (TranspileContext) createModelBackedContext.invoke(adapter, "demo.orders", model);
    assertTrue(ctx.resolve("name").isPresent());
    assertTrue(ctx.resolveTable("demo.orders").isPresent());

    var resolveRelationPath = SqlDataAdapter.class.getDeclaredMethod("resolveRelationPath", String.class, TranspileContext.class);
    resolveRelationPath.setAccessible(true);
    var relationPath = resolveRelationPath.invoke(adapter, "demo.orders", ctx);
    assertNotNull(relationPath);
  }

  private static DataModel model() {
    var field = new DataModelField() {
      @Override public String getName() { return "name"; }
      @Override public String getLabel() { return "name"; }
      @Override public String getDescription() { return null; }
      @Override public String getType() { return null; }
      @Override public String getRawName() { return "demo_name"; }
      @Override public String getRawType() { return null; }
      @Override public ValueType<?> getValueType() { return new StringValue(); }
      @Override public Object getDefaultValue(Map<String, Object> context) { return null; }
      @Override public List<com.ouroboros.data.validation.Rule> getRules() { return Collections.emptyList(); }
      @Override public Integer getDecimalDigits() { return null; }
      @Override public Integer getSize() { return 64; }
      @Override public Boolean getIsNullable() { return true; }
      @Override public Boolean getIsUnsigned() { return false; }
      @Override public Boolean getIsAutoIncrement() { return false; }
      @Override public Boolean getIsUnique() { return false; }
      @Override public Map<String, Object> getExtraProps() { return Collections.emptyMap(); }
      @Override public Optional<Object> getExtraProp(String name) { return Optional.empty(); }
      @Override public DataModel getDataModel() { return null; }
    };

    return new DataModel() {
      @Override public String getFormatVersion() { return "1"; }
      @Override public String getSource() { return null; }
      @Override public String getNamespace() { return "demo"; }
      @Override public String getName() { return "orders"; }
      @Override public String getFullName() { return "demo.orders"; }
      @Override public String getLabel() { return "orders"; }
      @Override public String getDescription() { return null; }
      @Override public String getRawName() { return "orders"; }
      @Override public MigrationStrategy getMigrationStrategy() { return MigrationStrategy.AUTO; }
      @Override public Map<String, Object> getExtraProps() { return Collections.emptyMap(); }
      @Override public Optional<Object> getExtraProp(String name) { return Optional.empty(); }
      @Override public List<DataModelField> getFields() { return Arrays.asList(field); }
      @Override public Optional<DataModelField> getField(String name) { return "name".equals(name) ? Optional.of(field) : Optional.empty(); }
      @Override public List<DataModelField> getPrimaryKeys() { return Collections.emptyList(); }
      @Override public PrimaryKeyGenerator<?> getPrimaryKeyGenerator() { return null; }
      @Override public DataAdapter getAdapter() { return null; }
      @Override public com.ouroboros.data.station.DataStation<?> getDataStation() { return null; }
      @Override public Try<Record> insert(Map<String, Object> data) { return null; }
      @Override public Try<Record> insertOrUpdate(Map<String, Object> data) { return null; }
      @Override public Try<RecordList> batchInsert(List<Map<String, Object>> dataList) { return null; }
      @Override public Try<RecordList> batchInsertOrUpdate(List<Map<String, Object>> dataList) { return null; }
      @Override public Try<Long> update(Object id, Map<String, Object> data) { return null; }
      @Override public Try<Long> update(List<?> ids, Map<String, Object> data) { return null; }
      @Override public Try<Long> update(Map<String, Object> where, Map<String, Object> data) { return null; }
      @Override public Try<Long> delete(Object id) { return null; }
      @Override public Try<Long> delete(List<?> ids) { return null; }
      @Override public Try<Long> delete(Map<String, Object> where) { return null; }
      @Override public Try<Long> count(Map<String, Object> where) { return null; }
      @Override public Try<Record> get(Object id) { return null; }
      @Override public Try<Record> get(Object id, Map<String, Object> statement) { return null; }
      @Override public Try<RecordList> query(List<?> ids) { return null; }
      @Override public Try<RecordList> query(com.ouroboros.data.dsl.statement.QueryStatement statement) { return null; }
      @Override public Try<RecordList> query(Map<String, Object> statement) { return null; }
      @Override public Try<RecordList> query(List<String> select, Map<String, Object> where) { return null; }
      @Override public Try<RecordList> query(List<String> select, Map<String, Object> where, Map<String, Object> orderBy) { return null; }
      @Override public Try<RecordList> query(List<String> select, Map<String, Object> where, Map<String, Object> orderBy, Integer offset, Integer limit) { return null; }
      @Override public DataModel withPlugins(java.util.Collection<com.ouroboros.data.model.PluginDescriptor> pluginDescriptors) { return this; }
      @Override public DataModel withoutPlugins(java.util.Collection<String> pluginNames) { return this; }
      @Override public DataModel withoutPlugins() { return this; }
      @Override public boolean hasPlugin(String name) { return false; }
    };
  }
}
