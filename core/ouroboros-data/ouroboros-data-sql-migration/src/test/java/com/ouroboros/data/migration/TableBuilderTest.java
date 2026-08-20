package com.ouroboros.data.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.adapter.DataAdapter;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.DataModelUniqueConstraintMeta;
import com.ouroboros.data.model.MigrationStrategy;
import com.ouroboros.data.model.PrimaryKeyGenerator;
import com.ouroboros.data.model.UniquenessScope;
import com.ouroboros.data.model.valuetypes.IntegerValue;
import com.ouroboros.data.model.valuetypes.StringValue;
import com.ouroboros.data.model.ValueType;
import com.ouroboros.data.record.Record;
import com.ouroboros.data.record.RecordList;
import com.ouroboros.data.station.DataStation;

import io.vavr.control.Try;
import liquibase.CatalogAndSchema;
import liquibase.database.core.H2Database;

class TableBuilderTest {

  @Test
  void shouldBuildTableWithPrimaryKeyUniqueAndAutoIncrement() {
    var id = field("id", "id", new IntegerValue(), null, null, false, true, false, true, null, "id");
    var name = field("name", "name", new StringValue(), null, 128, true, false, true, true, "display name", "name");
    var model = model("demo.user", "user", Arrays.asList(id, name), Arrays.asList(id));
    bindModel(id, model);
    bindModel(name, model);

    var table = TableBuilder.build(new H2Database(), model, new CatalogAndSchema(null, null));

    assertEquals("user", table.getName());
    assertNotNull(table.getColumn("id").getAutoIncrementInformation());
    assertEquals("display name", table.getColumn("name").getRemarks());
    assertNotNull(table.getPrimaryKey());
    assertTrue(table.getUniqueConstraints().stream().anyMatch(u -> u.getName().contains("__name")));
  }

  @Test
  void activeRecordsUniqueFieldShouldNotBuildOrdinaryUniqueConstraint() {
    var id = field("id", "id", new IntegerValue(), null, null, false, true, false, true, null, "id");
    var name = field("name", "name", new StringValue(), null, 128, true, false, true, true, "display name", "name");
    var model = model(
        "demo.user",
        "user",
        Arrays.asList(id, name),
        Arrays.asList(id),
        UniquenessScope.ACTIVE_RECORDS
    );
    bindModel(id, model);
    bindModel(name, model);

    var table = TableBuilder.build(new H2Database(), model, new CatalogAndSchema(null, null));

    assertTrue(table.getUniqueConstraints().isEmpty());
  }

  @Test
  void fieldLevelAllRecordsScopeShouldOverrideActiveModelDefault() {
    var id = field("id", "id", new IntegerValue(), null, null, false, true, false, true, null, "id");
    var name = field(
        "name",
        "name",
        new StringValue(),
        null,
        128,
        true,
        false,
        true,
        true,
        "display name",
        "name",
        UniquenessScope.ALL_RECORDS
    );
    var model = model(
        "demo.user",
        "user",
        Arrays.asList(id, name),
        Arrays.asList(id),
        UniquenessScope.ACTIVE_RECORDS
    );
    bindModel(id, model);
    bindModel(name, model);

    var table = TableBuilder.build(new H2Database(), model, new CatalogAndSchema(null, null));

    assertTrue(table.getUniqueConstraints().stream().anyMatch(u -> u.getName().contains("__name")));
  }

  @Test
  void fieldLevelActiveRecordsScopeShouldOverrideAllRecordsModelDefault() {
    var id = field("id", "id", new IntegerValue(), null, null, false, true, false, true, null, "id");
    var name = field(
        "name",
        "name",
        new StringValue(),
        null,
        128,
        true,
        false,
        true,
        true,
        "display name",
        "name",
        UniquenessScope.ACTIVE_RECORDS
    );
    var model = model("demo.user", "user", Arrays.asList(id, name), Arrays.asList(id));
    bindModel(id, model);
    bindModel(name, model);

    var table = TableBuilder.build(new H2Database(), model, new CatalogAndSchema(null, null));

    assertTrue(table.getUniqueConstraints().isEmpty());
  }

  @Test
  void modelLevelUniqueConstraintsShouldNotBuildOrdinaryUniqueConstraint() {
    var id = field("id", "id", new IntegerValue(), null, null, false, true, false, true, null, "id");
    var name = field("name", "name", new StringValue(), null, 128, true, false, false, true, "display name", "name");
    var model = model(
        "demo.user",
        "user",
        Arrays.asList(id, name),
        Arrays.asList(id),
        UniquenessScope.ALL_RECORDS,
        Collections.singletonList(uniqueConstraint("model_name", "name"))
    );
    bindModel(id, model);
    bindModel(name, model);

    var table = TableBuilder.build(new H2Database(), model, new CatalogAndSchema(null, null));

    assertTrue(table.getUniqueConstraints().isEmpty());
  }

  @Test
  void stringUniquenessScopeShouldBeAcceptedCaseInsensitivelyAndFallbackToAllRecords() {
    var id = field("id", "id", new IntegerValue(), null, null, false, true, false, true, null, "id");
    var name = field("name", "name", new StringValue(), null, 128, true, false, true, true, "display name", "name");
    var activeModel = model("demo.user", "user", Arrays.asList(id, name), Arrays.asList(id), "active_records");
    bindModel(id, activeModel);
    bindModel(name, activeModel);

    var activeTable = TableBuilder.build(new H2Database(), activeModel, new CatalogAndSchema(null, null));

    assertTrue(activeTable.getUniqueConstraints().isEmpty());

    var fallbackId = field("id", "id", new IntegerValue(), null, null, false, true, false, true, null, "id");
    var fallbackName = field("name", "name", new StringValue(), null, 128, true, false, true, true, "display name", "name");
    var fallbackModel = model("demo.user", "user", Arrays.asList(fallbackId, fallbackName), Arrays.asList(fallbackId), "unknown");
    bindModel(fallbackId, fallbackModel);
    bindModel(fallbackName, fallbackModel);

    var fallbackTable = TableBuilder.build(new H2Database(), fallbackModel, new CatalogAndSchema(null, null));

    assertTrue(fallbackTable.getUniqueConstraints().stream().anyMatch(u -> u.getName().contains("__name")));
  }

  private static DataModelField field(String name, String rawName, com.ouroboros.data.model.ValueType<?> valueType, Integer decimal, Integer size,
                                      boolean nullable, boolean autoInc, boolean unique, boolean physical,
                                      String description, String label) {
    return field(name, rawName, valueType, decimal, size, nullable, autoInc, unique, physical, description, label, null);
  }

  private static DataModelField field(String name, String rawName, com.ouroboros.data.model.ValueType<?> valueType, Integer decimal, Integer size,
                                      boolean nullable, boolean autoInc, boolean unique, boolean physical,
                                      String description, String label, UniquenessScope uniquenessScope) {
    return new StubField(name, rawName, valueType, decimal, size, nullable, autoInc, unique, physical, description, label,
        uniquenessScope);
  }

  private static DataModel model(String fullName, String rawName, List<DataModelField> fields, List<DataModelField> pks) {
    return model(fullName, rawName, fields, pks, UniquenessScope.ALL_RECORDS);
  }

  private static DataModel model(String fullName, String rawName, List<DataModelField> fields, List<DataModelField> pks,
                                 Object uniquenessScope) {
    return model(fullName, rawName, fields, pks, uniquenessScope, Collections.emptyList());
  }

  private static DataModel model(String fullName, String rawName, List<DataModelField> fields, List<DataModelField> pks,
                                 Object uniquenessScope, List<DataModelUniqueConstraintMeta> uniqueConstraints) {
    return new StubModel(fullName, rawName, fields, pks, uniquenessScope, uniqueConstraints);
  }

  private static DataModelUniqueConstraintMeta uniqueConstraint(String name, String... fields) {
    var constraint = new DataModelUniqueConstraintMeta();
    constraint.setName(name);
    constraint.setFields(Arrays.asList(fields));
    return constraint;
  }

  private static void bindModel(DataModelField field, DataModel model) {
    ((StubField) field).dataModel = model;
  }

  private static class StubField implements DataModelField {
    private final String name;
    private final String rawName;
    private final ValueType<?> valueType;
    private final Integer decimal;
    private final Integer size;
    private final boolean nullable;
    private final boolean autoInc;
    private final boolean unique;
    private final boolean physical;
    private final String description;
    private final String label;
    private final UniquenessScope uniquenessScope;
    private DataModel dataModel;

    StubField(String name, String rawName, ValueType<?> valueType, Integer decimal, Integer size,
              boolean nullable, boolean autoInc, boolean unique, boolean physical,
              String description, String label, UniquenessScope uniquenessScope) {
      this.name = name;
      this.rawName = rawName;
      this.valueType = physical ? valueType : wrapPhysical(valueType, false);
      this.decimal = decimal;
      this.size = size;
      this.nullable = nullable;
      this.autoInc = autoInc;
      this.unique = unique;
      this.physical = physical;
      this.description = description;
      this.label = label;
      this.uniquenessScope = uniquenessScope;
    }

    @Override public String getName() { return name; }
    @Override public String getLabel() { return label; }
    @Override public String getDescription() { return description; }
    @Override public String getType() { return null; }
    @Override public String getRawName() { return rawName; }
    @Override public String getRawType() { return null; }
    @Override public ValueType<?> getValueType() { return valueType; }
    @Override public Object getDefaultValue(Map<String, Object> context) { return null; }
    @Override public List<com.ouroboros.data.validation.Rule> getRules() { return Collections.emptyList(); }
    @Override public Integer getDecimalDigits() { return decimal; }
    @Override public Integer getSize() { return size; }
    @Override public Boolean getIsNullable() { return nullable; }
    @Override public Boolean getIsUnsigned() { return false; }
    @Override public Boolean getIsAutoIncrement() { return autoInc; }
    @Override public Boolean getIsUnique() { return unique; }
    @Override public UniquenessScope getUniquenessScope() { return uniquenessScope; }
    @Override public Map<String, Object> getExtraProps() { return Collections.emptyMap(); }
    @Override public Optional<Object> getExtraProp(String name) { return Optional.empty(); }
    @Override public DataModel getDataModel() { return dataModel; }
  }

  private static ValueType<?> wrapPhysical(ValueType<?> valueType, boolean physical) {
    if (valueType instanceof IntegerValue) {
      return new IntegerValue() {
        @Override public Boolean isPhysical() { return physical; }
      };
    }
    if (valueType instanceof StringValue) {
      return new StringValue() {
        @Override public Boolean isPhysical() { return physical; }
      };
    }
    return valueType;
  }

  private static class StubModel implements DataModel {
    private final String fullName;
    private final String rawName;
    private final List<DataModelField> fields;
    private final List<DataModelField> pks;
    private final List<DataModelUniqueConstraintMeta> uniqueConstraints;
    private final Map<String, Object> extraProps;

    StubModel(String fullName, String rawName, List<DataModelField> fields, List<DataModelField> pks,
              Object uniquenessScope, List<DataModelUniqueConstraintMeta> uniqueConstraints) {
      this.fullName = fullName;
      this.rawName = rawName;
      this.fields = fields;
      this.pks = pks;
      this.uniqueConstraints = uniqueConstraints;
      this.extraProps = new LinkedHashMap<>();
      if (!UniquenessScope.ALL_RECORDS.equals(uniquenessScope)) {
        this.extraProps.put(UniquenessScope.EXTRA_PROP_NAME, uniquenessScope);
      }
    }

    @Override public String getFormatVersion() { return "1"; }
    @Override public String getSource() { return null; }
    @Override public String getNamespace() { return "demo"; }
    @Override public String getName() { return "user"; }
    @Override public String getFullName() { return fullName; }
    @Override public String getLabel() { return "User"; }
    @Override public String getDescription() { return "User Table"; }
    @Override public String getRawName() { return rawName; }
    @Override public MigrationStrategy getMigrationStrategy() { return MigrationStrategy.AUTO; }
    @Override public Map<String, Object> getExtraProps() { return extraProps; }
    @Override public Optional<Object> getExtraProp(String name) { return Optional.ofNullable(extraProps.get(name)); }
    @Override public List<DataModelField> getFields() { return fields; }
    @Override public Optional<DataModelField> getField(String name) { return fields.stream().filter(f -> f.getName().equals(name)).findFirst(); }
    @Override public List<DataModelField> getPrimaryKeys() { return pks; }
    @Override public List<DataModelUniqueConstraintMeta> getUniqueConstraints() { return uniqueConstraints; }
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
  }
}
