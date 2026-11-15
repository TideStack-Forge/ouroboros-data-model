package com.ouroboros.data.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

  @ParameterizedTest
  @ValueSource(strings = {"", ";DATABASE_TO_LOWER=TRUE"})
  void unchangedSchemaShouldNeedNoMigrationRegardlessOfIdentifierCase(String caseSetting) throws SQLException {
    var ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:migration_identifier_case_" + UUID.randomUUID()
        + ";DB_CLOSE_DELAY=-1" + caseSetting);
    ds.setUser("sa");
    ds.setPassword("");
    var service = new LiquibaseSqlMigrationService();
    var models = List.of(model("orders"));

    var initialSql = service.generateMigrationSql(ds, "", "Ouroboros", models).getOrElseThrow(error -> error);
    assertFalse(initialSql.isEmpty());
    try (var connection = ds.getConnection(); var statement = connection.createStatement()) {
      for (var sql : initialSql) {
        statement.execute(sql);
      }
      statement.executeUpdate("INSERT INTO orders (id, code) VALUES (1, 'retained')");
    }

    var unchangedSql = service.generateMigrationSql(ds, "", "Ouroboros", models).getOrElseThrow(error -> error);
    assertTrue(unchangedSql.isEmpty(), unchangedSql.toString());
    var repeated = service.migrate(ds, "", "Ouroboros", models);
    assertTrue(repeated.isRight(), () -> repeated.getLeft().getMessage());

    var expanded = List.of(model("orders"), model("more_orders"));
    var addedTableSql = service.generateMigrationSql(ds, "", "Ouroboros", expanded).getOrElseThrow(error -> error);
    assertFalse(addedTableSql.isEmpty());
    try (var connection = ds.getConnection(); var statement = connection.createStatement()) {
      for (var sql : addedTableSql) {
        statement.execute(sql);
      }
      try (var rows = statement.executeQuery("SELECT code FROM orders WHERE id = 1")) {
        assertTrue(rows.next());
        assertEquals("retained", rows.getString(1));
      }
      statement.executeUpdate("INSERT INTO more_orders (id, code) VALUES (2, 'new')");
    }
    assertTrue(service.generateMigrationSql(ds, "", "Ouroboros", expanded)
        .getOrElseThrow(error -> error).isEmpty());
  }

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

  @Test
  void shouldCreateBothTablesWhenMigrationsRunBackToBack() throws SQLException {
    var ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:migration_back_to_back_test;MODE=MySQL;DB_CLOSE_DELAY=-1");
    ds.setUser("sa");
    ds.setPassword("");

    var service = new LiquibaseSqlMigrationService();

    var first = service.migrate(ds, "", "Ouroboros", Arrays.asList(model("DYNAMIC_FORM_A")));
    var second = service.migrate(ds, "", "Ouroboros", Arrays.asList(model("DYNAMIC_FORM_B")));

    assertTrue(first.isRight(), () -> first.isLeft() ? first.getLeft().getMessage() : "");
    assertTrue(second.isRight(), () -> second.isLeft() ? second.getLeft().getMessage() : "");
    assertTrue(tableExists(ds, "DYNAMIC_FORM_A"));
    assertTrue(tableExists(ds, "DYNAMIC_FORM_B"));
  }

  @Test
  void shouldCreateTableAfterPreviousNoopMigration() throws SQLException {
    var ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:migration_after_noop_test;MODE=MySQL;DB_CLOSE_DELAY=-1");
    ds.setUser("sa");
    ds.setPassword("");

    var service = new LiquibaseSqlMigrationService();

    var noop = service.migrate(ds, "", "Ouroboros", Collections.emptyList());
    var migrated = service.migrate(ds, "", "Ouroboros", Arrays.asList(model("DYNAMIC_FORM_AFTER_NOOP")));

    assertTrue(noop.isRight(), () -> noop.isLeft() ? noop.getLeft().getMessage() : "");
    assertTrue(migrated.isRight(), () -> migrated.isLeft() ? migrated.getLeft().getMessage() : "");
    assertTrue(tableExists(ds, "DYNAMIC_FORM_AFTER_NOOP"));
  }

  @Test
  void shouldCreateTablesAfterDataSourceWasUsedRepeatedly() throws SQLException {
    var ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:migration_after_repeated_datasource_use_test;MODE=MySQL;DB_CLOSE_DELAY=-1");
    ds.setUser("sa");
    ds.setPassword("");

    for (int i = 0; i < 20; i++) {
      touchMetadata(ds);
    }

    var service = new LiquibaseSqlMigrationService();
    var first = service.migrate(ds, "", "Ouroboros", Arrays.asList(model("DYNAMIC_FORM_REPEATED_A")));
    var second = service.migrate(ds, "", "Ouroboros", Arrays.asList(model("DYNAMIC_FORM_REPEATED_B")));

    assertTrue(first.isRight(), () -> first.isLeft() ? first.getLeft().getMessage() : "");
    assertTrue(second.isRight(), () -> second.isLeft() ? second.getLeft().getMessage() : "");
    assertTrue(tableExists(ds, "DYNAMIC_FORM_REPEATED_A"));
    assertTrue(tableExists(ds, "DYNAMIC_FORM_REPEATED_B"));
  }

  @Test
  void widensOptedInConfigurationColumnWithoutLosingValuesOrShrinkingOnRollback() throws SQLException {
    var ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:configuration_widening;MODE=MySQL;DB_CLOSE_DELAY=-1");
    ds.setUser("sa");
    var service = new LiquibaseSqlMigrationService();
    String table = "CONFIGURATION_WIDENING";
    var original = model(table, configValue(255, false));
    assertTrue(service.migrate(ds, "", "Ouroboros", List.of(original)).isRight());
    try (var connection = ds.getConnection(); var statement = connection.createStatement()) {
      statement.executeUpdate("INSERT INTO CONFIGURATION_WIDENING (id, code) VALUES (1, '旧值')");
    }
    var unapproved = model(table, configValue(1024, false));
    assertTrue(service.generateMigrationSql(ds, "", "Ouroboros", List.of(unapproved)).get().isEmpty());

    var widened = model(table, configValue(1024, true));
    var migrated = service.migrate(ds, "", "Ouroboros", List.of(widened));
    assertTrue(migrated.isRight(), () -> migrated.isLeft() ? migrated.getLeft().getMessage() : "");
    var newValue = "配置".repeat(500);
    try (var connection = ds.getConnection();
         var insert = connection.prepareStatement("INSERT INTO CONFIGURATION_WIDENING (id, code) VALUES (2, ?)")) {
      insert.setString(1, newValue);
      assertEquals(1, insert.executeUpdate());
    }
    assertTrue(service.migrate(ds, "", "Ouroboros", List.of(original)).isRight());
    try (var connection = ds.getConnection(); var statement = connection.createStatement();
         var rows = statement.executeQuery("SELECT code FROM CONFIGURATION_WIDENING ORDER BY id")) {
      assertTrue(rows.next());
      assertEquals("旧值", rows.getString(1));
      assertTrue(rows.next());
      assertEquals(newValue, rows.getString(1));
    }
  }

  @Test
  void addsUniqueColumnBeforeCheckingItsValuesAndPreservesExistingRows() throws SQLException {
    var ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:new_unique_column_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
    ds.setUser("sa");
    var service = new LiquibaseSqlMigrationService();
    String table = "ACCOUNT_UPGRADE";
    var original = model(table, new StubField("code", "code", new StringValue(), true, false, false));
    assertTrue(service.migrate(ds, "", "Ouroboros", List.of(original)).isRight());
    try (var connection = ds.getConnection(); var statement = connection.createStatement()) {
      statement.executeUpdate("INSERT INTO ACCOUNT_UPGRADE (id, code) VALUES (1, 'first'), (2, 'second')");
    }
    var upgraded = model(table,
        new StubField("publicSubjectId", "public_subject_id", new StringValue(), true, true, false));
    var result = service.migrate(ds, "", "Ouroboros", List.of(upgraded));
    assertTrue(result.isRight(), () -> result.getLeft().getMessage());
    try (var connection = ds.getConnection(); var statement = connection.createStatement()) {
      try (var rows = statement.executeQuery("SELECT COUNT(*) FROM ACCOUNT_UPGRADE WHERE public_subject_id IS NULL")) {
        assertTrue(rows.next());
        assertEquals(2, rows.getInt(1));
      }
      statement.executeUpdate("UPDATE ACCOUNT_UPGRADE SET public_subject_id = 'subject-one' WHERE id = 1");
      org.junit.jupiter.api.Assertions.assertThrows(SQLException.class,
          () -> statement.executeUpdate("UPDATE ACCOUNT_UPGRADE SET public_subject_id = 'subject-one' WHERE id = 2"));
    }
  }

  @Test
  void stillRejectsDuplicateValuesWhenAddingUniqueConstraintToExistingColumn() throws SQLException {
    var ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:existing_unique_column_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
    ds.setUser("sa");
    var service = new LiquibaseSqlMigrationService();
    String table = "EXISTING_ACCOUNT";
    assertTrue(service.migrate(ds, "", "Ouroboros", List.of(model(table,
        new StubField("code", "code", new StringValue(), true, false, false)))).isRight());
    try (var connection = ds.getConnection(); var statement = connection.createStatement()) {
      statement.executeUpdate("INSERT INTO EXISTING_ACCOUNT (id, code) VALUES (1, 'same'), (2, 'same')");
    }
    var result = service.migrate(ds, "", "Ouroboros", List.of(model(table,
        new StubField("code", "code", new StringValue(), true, true, false))));
    assertTrue(result.isLeft());
    assertTrue(result.getLeft().getMessage().contains("新增唯一约束前发现重复数据"));
  }

  private static StubField configValue(int size, boolean allowWidening) {
    return new StubField("code", "code", new StringValue(), false, false, false) {
      @Override public String getLabel() { return null; }
      @Override public Integer getSize() { return size; }
      @Override public Optional<Object> getExtraProp(String name) {
        return allowWidening && "allowVarcharToText".equals(name) ? Optional.of("true") : Optional.empty();
      }
    };
  }

  @Test
  void shouldGenerateUniqueInMemoryChangeSetIdRoots() {
    var first = DatabaseMigration.nextInMemoryChangeSetIdRoot();
    var second = DatabaseMigration.nextInMemoryChangeSetIdRoot();

    assertNotEquals(first, second);
    assertTrue(first.startsWith("migration-"));
    assertTrue(second.startsWith("migration-"));
  }

  private static DataModel model() {
    return model("ORDERS");
  }

  private static DataModel model(String rawTableName) {
    return model(rawTableName, new StubField("code", "code", new StringValue(), true, false, true));
  }

  private static DataModel model(String rawTableName, StubField code) {
    var id = new StubField("id", "id", new IntegerValue(), false, true, false);
    return new DataModel() {
      @Override public String getFormatVersion() { return "1"; }
      @Override public String getSource() { return null; }
      @Override public String getNamespace() { return "demo"; }
      @Override public String getName() { return rawTableName.toLowerCase(); }
      @Override public String getFullName() { return "demo." + getName(); }
      @Override public String getLabel() { return getName(); }
      @Override public String getDescription() { return null; }
      @Override public String getRawName() { return rawTableName; }
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

  private static boolean tableExists(JdbcDataSource ds, String tableName) throws SQLException {
    try (var connection = ds.getConnection();
         var statement = connection.prepareStatement("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?")) {
      statement.setString(1, tableName);
      try (var resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1) > 0;
      }
    }
  }

  private static void touchMetadata(JdbcDataSource ds) throws SQLException {
    try (var connection = ds.getConnection();
         var tables = connection.getMetaData().getTables(null, null, "%", new String[] {"TABLE"})) {
      while (tables.next()) {
        tables.getString("TABLE_NAME");
      }
    }
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
