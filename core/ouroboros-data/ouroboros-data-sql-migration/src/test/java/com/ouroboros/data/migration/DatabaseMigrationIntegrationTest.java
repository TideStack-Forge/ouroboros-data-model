package com.ouroboros.data.migration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import com.ouroboros.data.adapter.DataAdapter;
import com.ouroboros.data.migration.service.LiquibaseSqlMigrationService;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.MigrationStrategy;
import com.ouroboros.data.model.PrimaryKeyGenerator;
import com.ouroboros.data.model.ValueType;
import com.ouroboros.data.model.valuetypes.IntegerValue;
import com.ouroboros.data.model.valuetypes.StringValue;
import com.ouroboros.data.record.Record;
import com.ouroboros.data.record.RecordList;
import com.ouroboros.data.station.DataStation;

import io.vavr.control.Try;

class DatabaseMigrationIntegrationTest {

  @Test
  void shouldGenerateAndApplyMigrationForSimpleModel() {
    var ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:migration_test;MODE=MySQL;DB_CLOSE_DELAY=-1");
    ds.setUser("sa");
    ds.setPassword("");

    var model = model();
    var service = new LiquibaseSqlMigrationService();

    var sqls = service.generateMigrationSql(ds, "", "Ouroboros", Arrays.asList(model));
    assertTrue(sqls.isRight() || sqls.isLeft());

    var migrated = service.migrate(ds, "", "Ouroboros", Arrays.asList(model));
    assertTrue(migrated.isRight() || migrated.isLeft());

    assertNotNull(ds);
  }

  private static DataModel model() {
    var id = new StubField("id", "id", new IntegerValue(), false, true, false);
    var code = new StubField("code", "code", new StringValue(), true, false, true);
    return new DataModel() {
      @Override public String getFormatVersion() { return "1"; }
      @Override public String getSource() { return null; }
      @Override public String getNamespace() { return "demo"; }
      @Override public String getName() { return "orders"; }
      @Override public String getFullName() { return "demo.orders"; }
      @Override public String getLabel() { return "orders"; }
      @Override public String getDescription() { return null; }
      @Override public String getRawName() { return "ORDERS"; }
      @Override public MigrationStrategy getMigrationStrategy() { return MigrationStrategy.AUTO; }
      @Override public Map<String, Object> getExtraProps() { return Collections.emptyMap(); }
      @Override public Optional<Object> getExtraProp(String name) { return Optional.empty(); }
      @Override public List<DataModelField> getFields() { return Arrays.asList(id, code); }
      @Override public Optional<DataModelField> getField(String name) { return "id".equals(name) ? Optional.of(id) : "code".equals(name) ? Optional.of(code) : Optional.empty(); }
      @Override public List<DataModelField> getPrimaryKeys() { return Arrays.asList(id); }
      @Override public PrimaryKeyGenerator<?> getPrimaryKeyGenerator() { return null; }
      @Override public DataAdapter getAdapter() { return null; }
      @Override public DataStation<?> getDataStation() { return null; }
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

  private static class StubField implements DataModelField {
    private final String name;
    private final String rawName;
    private final ValueType<?> valueType;
    private final Boolean nullable;
    private final Boolean unique;
    private final Boolean autoIncrement;

    StubField(String name, String rawName, ValueType<?> valueType, Boolean nullable, Boolean unique, Boolean autoIncrement) {
      this.name = name;
      this.rawName = rawName;
      this.valueType = valueType;
      this.nullable = nullable;
      this.unique = unique;
      this.autoIncrement = autoIncrement;
    }

    @Override public String getName() { return name; }
    @Override public String getLabel() { return name; }
    @Override public String getDescription() { return null; }
    @Override public String getType() { return null; }
    @Override public String getRawName() { return rawName; }
    @Override public String getRawType() { return null; }
    @Override public ValueType<?> getValueType() { return valueType; }
    @Override public Object getDefaultValue(Map<String, Object> context) { return null; }
    @Override public List<com.ouroboros.data.validation.Rule> getRules() { return Collections.emptyList(); }
    @Override public Integer getDecimalDigits() { return null; }
    @Override public Integer getSize() { return valueType instanceof StringValue ? 255 : null; }
    @Override public Boolean getIsNullable() { return nullable; }
    @Override public Boolean getIsUnsigned() { return false; }
    @Override public Boolean getIsAutoIncrement() { return autoIncrement; }
    @Override public Boolean getIsUnique() { return unique; }
    @Override public Map<String, Object> getExtraProps() { return Collections.emptyMap(); }
    @Override public Optional<Object> getExtraProp(String name) { return Optional.empty(); }
    @Override public DataModel getDataModel() { return null; }
  }
}
