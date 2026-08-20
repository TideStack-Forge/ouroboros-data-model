package com.ouroboros.data.migration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.adapter.DataAdapter;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.MigrationStrategy;
import com.ouroboros.data.model.PrimaryKeyGenerator;
import com.ouroboros.data.model.ValueType;
import com.ouroboros.data.model.valuetypes.StringValue;
import com.ouroboros.data.record.Record;
import com.ouroboros.data.record.RecordList;
import com.ouroboros.data.station.DataStation;

import io.vavr.control.Try;
import liquibase.diff.ObjectDifferences;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Column;
import liquibase.structure.core.Table;
import liquibase.structure.core.UniqueConstraint;

class DatabaseMigrationInnerClassesTest {

  @Test
  void shouldCoverSnapshotAndDiffFilters() throws Exception {
    var snapshotFilterClass = Class.forName("com.ouroboros.data.migration.DatabaseMigration$SnapshotObjectFilter");
    var ctor = snapshotFilterClass.getDeclaredConstructor(List.class, Class[].class);
    ctor.setAccessible(true);
    var snapshotFilter = ctor.newInstance(Arrays.asList(model("orders")), new Class[]{Table.class, Column.class});

    var include = snapshotFilterClass.getDeclaredMethod("include", DatabaseObject.class);
    include.setAccessible(true);
    var includeMissing = snapshotFilterClass.getDeclaredMethod("includeMissing", DatabaseObject.class, liquibase.database.Database.class, liquibase.database.Database.class);
    includeMissing.setAccessible(true);

    var matching = new Table("", "", "orders");
    var nonMatching = new Table("", "", "other");
    assertTrue((Boolean) include.invoke(snapshotFilter, matching));
    assertFalse((Boolean) include.invoke(snapshotFilter, nonMatching));
    assertTrue((Boolean) includeMissing.invoke(snapshotFilter, matching, null, null));

    var diffFilterClass = Class.forName("com.ouroboros.data.migration.DatabaseMigration$DiffToChangeLogFilter");
    var diffFilterCtor = diffFilterClass.getDeclaredConstructor();
    diffFilterCtor.setAccessible(true);
    var diffFilter = diffFilterCtor.newInstance();

    var includeMissingDiff = diffFilterClass.getDeclaredMethod("includeMissing", DatabaseObject.class, liquibase.database.Database.class, liquibase.database.Database.class);
    includeMissingDiff.setAccessible(true);
    var includeChangedDiff = diffFilterClass.getDeclaredMethod("includeChanged", DatabaseObject.class, ObjectDifferences.class, liquibase.database.Database.class, liquibase.database.Database.class);
    includeChangedDiff.setAccessible(true);
    var includeUnexpectedDiff = diffFilterClass.getDeclaredMethod("includeUnexpected", DatabaseObject.class, liquibase.database.Database.class, liquibase.database.Database.class);
    includeUnexpectedDiff.setAccessible(true);
    var includeAnyDiff = diffFilterClass.getDeclaredMethod("include", DatabaseObject.class);
    includeAnyDiff.setAccessible(true);

    var c = new Column();
    c.setName("name");
    c.setRelation(matching);
    assertTrue((Boolean) includeMissingDiff.invoke(diffFilter, matching, null, null));
    assertTrue((Boolean) includeMissingDiff.invoke(diffFilter, c, null, null));

    var uc = new UniqueConstraint();
    uc.setRelation(matching);
    uc.addColumn(0, c);
    assertTrue((Boolean) includeMissingDiff.invoke(diffFilter, uc, null, null));

    var existingColumn = new Column();
    existingColumn.setName("email");
    existingColumn.setRelation(matching);
    var missingUniqueOnExistingColumn = new UniqueConstraint();
    missingUniqueOnExistingColumn.setRelation(matching);
    missingUniqueOnExistingColumn.addColumn(0, existingColumn);
    assertTrue((Boolean) includeMissingDiff.invoke(diffFilter, missingUniqueOnExistingColumn, null, null));

    var diffs = new ObjectDifferences(new liquibase.diff.compare.CompareControl());
    diffs.addDifference("nullable", Boolean.FALSE, Boolean.TRUE);
    assertFalse((Boolean) includeChangedDiff.invoke(diffFilter, c, diffs, null, null));

    assertTrue((Boolean) includeChangedDiff.invoke(diffFilter, c, new ObjectDifferences(new liquibase.diff.compare.CompareControl()), null, null));
    assertTrue((Boolean) includeChangedDiff.invoke(diffFilter, matching, new ObjectDifferences(new liquibase.diff.compare.CompareControl()), null, null));
    assertTrue((Boolean) includeUnexpectedDiff.invoke(diffFilter, matching, null, null));
    assertTrue((Boolean) includeAnyDiff.invoke(diffFilter, matching));
  }

  @Test
  void shouldCoverInMemoryAccessorBasics() throws Exception {
    var log = new liquibase.changelog.DatabaseChangeLog();
    log.setLogicalFilePath("in-memory-changelog");
    log.setPhysicalFilePath("in-memory-changelog.xml");
    var changeSet = new liquibase.changelog.ChangeSet(log);
    changeSet.setContextFilter(new liquibase.ContextExpression(""));
    log.addChangeSet(changeSet);

    var accessorClass = Class.forName("com.ouroboros.data.migration.DatabaseMigration$InMemoryResourceAccessor");
    var ctor = accessorClass.getDeclaredConstructor(liquibase.changelog.DatabaseChangeLog.class);
    ctor.setAccessible(true);
    var accessor = ctor.newInstance(log);

    var getAll = accessorClass.getDeclaredMethod("getAll", String.class);
    getAll.setAccessible(true);
    var search = accessorClass.getDeclaredMethod("search", String.class, boolean.class);
    search.setAccessible(true);
    var describeLocations = accessorClass.getDeclaredMethod("describeLocations");
    describeLocations.setAccessible(true);
    var getExisting = accessorClass.getDeclaredMethod("getExisting", String.class);
    getExisting.setAccessible(true);
    var get = accessorClass.getDeclaredMethod("get", String.class);
    get.setAccessible(true);
    var close = accessorClass.getDeclaredMethod("close");
    close.setAccessible(true);

    var resources = (List<?>) getAll.invoke(accessor, "in-memory-changelog.xml");
    assertFalse(resources.isEmpty());
    var resource = resources.get(0);
    assertNotNull(resource);

    var searched = (List<?>) search.invoke(accessor, "any", true);
    assertFalse(searched.isEmpty());
    var locations = (List<?>) describeLocations.invoke(accessor);
    assertTrue(locations.isEmpty());

    var resourceField = accessorClass.getDeclaredField("resource");
    resourceField.setAccessible(true);
    var actualResource = resourceField.get(accessor);
    var validPath = (String) actualResource.getClass().getMethod("getPath").invoke(actualResource);
    assertNotNull(getExisting.invoke(accessor, validPath));
    assertThrows(Exception.class, () -> getExisting.invoke(accessor, "missing.xml"));
    assertNotNull(get.invoke(accessor, "whatever.xml"));
    close.invoke(accessor);

    var resourceClass = Class.forName("com.ouroboros.data.migration.DatabaseMigration$InMemoryResourceAccessor$InMemoryResource");
    var openInputStream = resourceClass.getDeclaredMethod("openInputStream");
    openInputStream.setAccessible(true);
    var exists = resourceClass.getDeclaredMethod("exists");
    exists.setAccessible(true);
    var resolve = resourceClass.getDeclaredMethod("resolve", String.class);
    resolve.setAccessible(true);
    var resolveSibling = resourceClass.getDeclaredMethod("resolveSibling", String.class);
    resolveSibling.setAccessible(true);

    var in = (InputStream) openInputStream.invoke(actualResource);
    assertNotNull(in);
    assertTrue((Boolean) exists.invoke(actualResource));
    assertTrue(resolve.invoke(actualResource, "x") == null);
    assertTrue(resolveSibling.invoke(actualResource, "x") == null);
  }

  private static DataModel model(String rawName) {
    var field = new DataModelField() {
      @Override public String getName() { return "id"; }
      @Override public String getLabel() { return "id"; }
      @Override public String getDescription() { return null; }
      @Override public String getType() { return null; }
      @Override public String getRawName() { return "id"; }
      @Override public String getRawType() { return null; }
      @Override public ValueType<?> getValueType() { return new StringValue(); }
      @Override public Object getDefaultValue(Map<String, Object> context) { return null; }
      @Override public List<com.ouroboros.data.validation.Rule> getRules() { return Collections.emptyList(); }
      @Override public Integer getDecimalDigits() { return null; }
      @Override public Integer getSize() { return 32; }
      @Override public Boolean getIsNullable() { return false; }
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
      @Override public String getName() { return rawName; }
      @Override public String getFullName() { return "demo." + rawName; }
      @Override public String getLabel() { return rawName; }
      @Override public String getDescription() { return null; }
      @Override public String getRawName() { return rawName; }
      @Override public MigrationStrategy getMigrationStrategy() { return MigrationStrategy.AUTO; }
      @Override public Map<String, Object> getExtraProps() { return Collections.emptyMap(); }
      @Override public Optional<Object> getExtraProp(String name) { return Optional.empty(); }
      @Override public List<DataModelField> getFields() { return Arrays.asList(field); }
      @Override public Optional<DataModelField> getField(String name) { return Optional.of(field); }
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
  }
}
