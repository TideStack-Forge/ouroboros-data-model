package com.ouroboros.data.model.query;

import static com.ouroboros.data.dsl.query.Query.field;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.vavr.control.Try;

import com.ouroboros.data.adapter.DataAdapter;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.model.BaseTypedDataModel;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.MigrationStrategy;
import com.ouroboros.data.model.PluginDescriptor;
import com.ouroboros.data.model.PrimaryKeyGenerator;
import com.ouroboros.data.record.Record;
import com.ouroboros.data.record.RecordList;
import com.ouroboros.data.station.DataStation;

class TypedDataModelQueryFacadeTest {

  @Test
  void executeShouldMapRecordListToTypedModels() {
    RecordingDataModel dataModel = new RecordingDataModel(RecordList.of(List.of(
        Map.of("id", "u1", "username", "Ada")
    )));
    BaseTypedDataModel<String, User> users = new BaseTypedDataModel<>(dataModel, User.class);

    Try<List<User>> result = users.query()
        .where(field("status").eq("ENABLED"))
        .execute();

    assertTrue(result.isSuccess());
    assertEquals(1, result.get().size());
    assertEquals("u1", result.get().get(0).getId());
    assertEquals("Ada", result.get().get(0).getUsername());
    assertEquals(Map.of("WHERE", Map.of("status", "ENABLED")), dataModel.lastQueryStatement);
  }

  @Test
  void pluginViewsShouldDelegateThroughBaseTypedDataModelViews() {
    RecordingDataModel base = new RecordingDataModel(RecordList.empty());
    RecordingDataModel withPlugin = new RecordingDataModel(RecordList.empty());
    RecordingDataModel withoutPlugin = new RecordingDataModel(RecordList.empty());
    base.withPluginsResult = withPlugin;
    base.withoutPluginsResult = withoutPlugin;

    BaseTypedDataModel<String, User> users = new BaseTypedDataModel<>(base, User.class);

    users.query()
        .withPlugins(new PluginDescriptor("Probe"))
        .where(field("active").eq(true))
        .execute();
    users.query()
        .withoutPlugins("Probe")
        .where(field("active").eq(false))
        .execute();

    assertEquals(List.of("Probe"), base.withPluginsNames);
    assertEquals(List.of("Probe"), base.withoutPluginNames);
    assertEquals(Map.of("WHERE", Map.of("active", true)), withPlugin.lastQueryStatement);
    assertEquals(Map.of("WHERE", Map.of("active", false)), withoutPlugin.lastQueryStatement);
  }

  public static class User {
    private String id;
    private String username;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }
  }

  private static class RecordingDataModel implements DataModel {
    private final RecordList result;
    private Map<String, Object> lastQueryStatement;
    private RecordingDataModel withPluginsResult;
    private RecordingDataModel withoutPluginsResult;
    private List<String> withPluginsNames = Collections.emptyList();
    private List<String> withoutPluginNames = Collections.emptyList();

    private RecordingDataModel(RecordList result) {
      this.result = result;
    }

    @Override
    public String getFormatVersion() {
      return "test";
    }

    @Override
    public String getSource() {
      return "test";
    }

    @Override
    public String getNamespace() {
      return "test";
    }

    @Override
    public String getName() {
      return "User";
    }

    @Override
    public String getFullName() {
      return "test.User";
    }

    @Override
    public String getLabel() {
      return "User";
    }

    @Override
    public String getDescription() {
      return "";
    }

    @Override
    public String getRawName() {
      return "user";
    }

    @Override
    public MigrationStrategy getMigrationStrategy() {
      return MigrationStrategy.AUTO;
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
    public List<DataModelField> getFields() {
      return Collections.emptyList();
    }

    @Override
    public Optional<DataModelField> getField(String name) {
      return Optional.empty();
    }

    @Override
    public List<DataModelField> getPrimaryKeys() {
      return Collections.emptyList();
    }

    @Override
    public PrimaryKeyGenerator<?> getPrimaryKeyGenerator() {
      return null;
    }

    @Override
    public DataAdapter getAdapter() {
      return null;
    }

    @Override
    public DataStation<?> getDataStation() {
      return null;
    }

    @Override
    public Try<Record> insert(Map<String, Object> data) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Try<Record> insertOrUpdate(Map<String, Object> data) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Try<RecordList> batchInsert(List<Map<String, Object>> dataList) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Try<RecordList> batchInsertOrUpdate(List<Map<String, Object>> dataList) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Try<Long> update(Object id, Map<String, Object> data) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Try<Long> update(List<?> ids, Map<String, Object> data) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Try<Long> update(Map<String, Object> where, Map<String, Object> data) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Try<Long> delete(Object id) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Try<Long> delete(List<?> ids) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Try<Long> delete(Map<String, Object> where) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Try<Long> count(Map<String, Object> where) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Try<Record> get(Object id) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Try<Record> get(Object id, Map<String, Object> statement) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Try<RecordList> query(List<?> ids) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Try<RecordList> query(QueryStatement statement) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Try<RecordList> query(Map<String, Object> statement) {
      lastQueryStatement = statement;
      return Try.success(result);
    }

    @Override
    public Try<RecordList> query(List<String> select, Map<String, Object> where) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Try<RecordList> query(List<String> select, Map<String, Object> where,
                                 Map<String, Object> orderBy) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Try<RecordList> query(List<String> select, Map<String, Object> where,
                                 Map<String, Object> orderBy, Integer offset, Integer limit) {
      throw new UnsupportedOperationException();
    }

    @Override
    public DataModel withPlugins(Collection<PluginDescriptor> pluginDescriptors) {
      withPluginsNames = pluginDescriptors.stream()
          .map(PluginDescriptor::getName)
          .toList();
      return withPluginsResult;
    }

    @Override
    public DataModel withoutPlugins(Collection<String> pluginNames) {
      withoutPluginNames = List.copyOf(pluginNames);
      return withoutPluginsResult;
    }

    @Override
    public DataModel withoutPlugins() {
      return withoutPluginsResult;
    }

    @Override
    public boolean hasPlugin(String name) {
      return false;
    }
  }
}
