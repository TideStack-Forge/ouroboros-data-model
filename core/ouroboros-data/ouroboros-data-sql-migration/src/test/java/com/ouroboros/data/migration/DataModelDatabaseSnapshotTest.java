package com.ouroboros.data.migration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Optional;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import com.ouroboros.data.adapter.DataAdapter;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.MigrationStrategy;
import com.ouroboros.data.model.PrimaryKeyGenerator;
import com.ouroboros.data.model.UniquenessScope;
import com.ouroboros.data.model.ValueType;
import com.ouroboros.data.model.valuetypes.StringValue;
import com.ouroboros.data.record.Record;
import com.ouroboros.data.record.RecordList;
import com.ouroboros.data.station.DataStation;

import io.vavr.control.Try;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.structure.core.Column;
import liquibase.structure.core.DataType;
import liquibase.structure.core.Table;
import liquibase.structure.core.UniqueConstraint;
import liquibase.structure.DatabaseObjectCollection;

class DataModelDatabaseSnapshotTest {

  @Test
  void shouldEvaluateTypeCompatibilityAndUpgradeRules() throws Exception {
    var snapshot = newSnapshot();

    var compatible = DataModelDatabaseSnapshot.class.getDeclaredMethod("isDataTypeCompatible", DataType.class, DataType.class);
    compatible.setAccessible(true);
    var upgradable = DataModelDatabaseSnapshot.class.getDeclaredMethod("isDataTypeUpgradable", DataType.class, DataType.class);
    upgradable.setAccessible(true);

    assertTrue((Boolean) compatible.invoke(snapshot, new DataType("VARCHAR"), new DataType("VARCHAR")));
    assertTrue((Boolean) compatible.invoke(snapshot, new DataType("NVARCHAR"), new DataType("VARCHAR")));
    assertFalse((Boolean) compatible.invoke(snapshot, new DataType("INTEGER"), new DataType("BIGINT")));

    var oldType = new DataType("DECIMAL");
    oldType.setColumnSize(10);
    oldType.setDecimalDigits(2);
    var newType = new DataType("DECIMAL");
    newType.setColumnSize(18);
    newType.setDecimalDigits(2);
    assertTrue((Boolean) upgradable.invoke(snapshot, newType, oldType));

    var notUpgradable = new DataType("DECIMAL");
    notUpgradable.setColumnSize(8);
    notUpgradable.setDecimalDigits(1);
    assertFalse((Boolean) upgradable.invoke(snapshot, notUpgradable, oldType));
  }

  @Test
  void shouldHandleAddOrMergeForColumnAndUniqueConstraintBranches() throws Exception {
    var snapshot = newSnapshot();

    var addOrMerge = DataModelDatabaseSnapshot.class.getDeclaredMethod("addOrMergeObject", liquibase.structure.DatabaseObject.class, liquibase.structure.DatabaseObject.class);
    addOrMerge.setAccessible(true);

    var table = new Table("", "", "orders");
    var removedNotNull = new Column();
    removedNotNull.setName("code");
    removedNotNull.setRelation(table);
    removedNotNull.setType(new DataType("VARCHAR"));
    removedNotNull.setNullable(false);

    addOrMerge.invoke(snapshot, removedNotNull, null);

    var uc = new UniqueConstraint();
    uc.setName("uk_orders_code");
    uc.setRelation(table);
    uc.addColumn(0, removedNotNull);
    addOrMerge.invoke(snapshot, uc, null);
  }

  @Test
  void activeRecordsSnapshotShouldDropOnlyGeneratedSingleColumnUniqueConstraint() throws Exception {
    var snapshot = newSnapshot(stubModel(true, UniquenessScope.ACTIVE_RECORDS));
    var table = new Table("", "", "orders");
    var code = column(table, "code");

    addOrMerge(snapshot, unique("un_demo_orders__code", table, code));

    assertFalse(hasUnique(snapshot, "un_demo_orders__code"));
  }

  @Test
  void activeRecordsStringScopeShouldDropGeneratedSingleColumnUniqueConstraint() throws Exception {
    var snapshot = newSnapshot(stubModel(true, "ACTIVE_records"));
    var table = new Table("", "", "orders");
    var code = column(table, "code");

    addOrMerge(snapshot, unique("un_demo_orders__code", table, code));

    assertFalse(hasUnique(snapshot, "un_demo_orders__code"));
  }

  @Test
  void activeRecordsSnapshotShouldRetainManualAndIncompleteUniqueConstraints() throws Exception {
    var snapshot = newSnapshot(stubModel(true, UniquenessScope.ACTIVE_RECORDS));
    var table = new Table("", "", "orders");
    var code = column(table, "code");

    addOrMerge(snapshot, unique("uk_orders_code_manual", table, code));
    assertDoesNotThrow(() -> addOrMerge(snapshot, unique("uk_orders_empty_columns", table)));

    assertTrue(hasUnique(snapshot, "uk_orders_code_manual"));
    assertTrue(hasUnique(snapshot, "uk_orders_empty_columns"));
  }

  private static DataModelDatabaseSnapshot newSnapshot() throws Exception {
    return newSnapshot(stubModel());
  }

  private static DataModelDatabaseSnapshot newSnapshot(DataModel model) throws Exception {
    var ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:snapshot_test;DB_CLOSE_DELAY=-1");
    ds.setUser("sa");
    ds.setPassword("");
    var conn = ds.getConnection();
    var db = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn));
    return new DataModelDatabaseSnapshot(db, Arrays.asList(model));
  }

  private static DataModel stubModel() {
    return stubModel(false, UniquenessScope.ALL_RECORDS);
  }

  private static DataModel stubModel(boolean unique, Object uniquenessScope) {
    final DataModel[] owner = new DataModel[1];
    Map<String, Object> extraProps = new LinkedHashMap<>();
    if (!UniquenessScope.ALL_RECORDS.equals(uniquenessScope)) {
      extraProps.put(UniquenessScope.EXTRA_PROP_NAME, uniquenessScope);
    }
    var field = new DataModelField() {
      @Override public String getName() { return "code"; }
      @Override public String getLabel() { return "code"; }
      @Override public String getDescription() { return null; }
      @Override public String getType() { return null; }
      @Override public String getRawName() { return "code"; }
      @Override public String getRawType() { return null; }
      @Override public ValueType<?> getValueType() { return new StringValue(); }
      @Override public Object getDefaultValue(Map<String, Object> context) { return null; }
      @Override public List<com.ouroboros.data.validation.Rule> getRules() { return Collections.emptyList(); }
      @Override public Integer getDecimalDigits() { return null; }
      @Override public Integer getSize() { return 64; }
      @Override public Boolean getIsNullable() { return false; }
      @Override public Boolean getIsUnsigned() { return false; }
      @Override public Boolean getIsAutoIncrement() { return false; }
      @Override public Boolean getIsUnique() { return unique; }
      @Override public Map<String, Object> getExtraProps() { return Collections.emptyMap(); }
      @Override public Optional<Object> getExtraProp(String name) { return Optional.empty(); }
      @Override public DataModel getDataModel() { return owner[0]; }
    };

    DataModel model = new DataModel() {
      @Override public String getFormatVersion() { return "1"; }
      @Override public String getSource() { return null; }
      @Override public String getNamespace() { return "demo"; }
      @Override public String getName() { return "orders"; }
      @Override public String getFullName() { return "demo.orders"; }
      @Override public String getLabel() { return "orders"; }
      @Override public String getDescription() { return null; }
      @Override public String getRawName() { return "orders"; }
      @Override public MigrationStrategy getMigrationStrategy() { return MigrationStrategy.AUTO; }
      @Override public Map<String, Object> getExtraProps() { return extraProps; }
      @Override public Optional<Object> getExtraProp(String name) { return Optional.ofNullable(extraProps.get(name)); }
      @Override public List<DataModelField> getFields() { return Arrays.asList(field); }
      @Override public Optional<DataModelField> getField(String name) { return "code".equals(name) ? Optional.of(field) : Optional.empty(); }
      @Override public List<DataModelField> getPrimaryKeys() { return Arrays.asList(field); }
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
    owner[0] = model;
    return model;
  }

  private static Column column(Table table, String name) {
    var column = new Column();
    column.setName(name);
    column.setRelation(table);
    column.setType(new DataType("VARCHAR"));
    return column;
  }

  private static UniqueConstraint unique(String name, Table table, Column... columns) {
    var uniqueConstraint = new UniqueConstraint();
    uniqueConstraint.setName(name);
    uniqueConstraint.setRelation(table);
    for (int i = 0; i < columns.length; i++) {
      uniqueConstraint.addColumn(i, columns[i]);
    }
    return uniqueConstraint;
  }

  private static void addOrMerge(DataModelDatabaseSnapshot snapshot, UniqueConstraint uniqueConstraint) throws Exception {
    var addOrMerge = DataModelDatabaseSnapshot.class.getDeclaredMethod(
        "addOrMergeObject",
        liquibase.structure.DatabaseObject.class,
        liquibase.structure.DatabaseObject.class
    );
    addOrMerge.setAccessible(true);
    addOrMerge.invoke(snapshot, uniqueConstraint, null);
  }

  private static boolean hasUnique(DataModelDatabaseSnapshot snapshot, String name) {
    var objects = (DatabaseObjectCollection) snapshot.getSerializableFieldValue("objects");
    return objects.get(UniqueConstraint.class).stream().anyMatch(uniqueConstraint -> name.equals(uniqueConstraint.getName()));
  }
}
