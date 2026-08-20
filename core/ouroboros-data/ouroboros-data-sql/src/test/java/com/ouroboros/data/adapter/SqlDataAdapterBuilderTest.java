package com.ouroboros.data.adapter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Optional;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelMeta;
import com.ouroboros.data.model.MigrationStrategy;
import com.ouroboros.data.station.DataStation;
import com.ouroboros.data.station.SqlDataStation;

class SqlDataAdapterBuilderTest {

  @Test
  void shouldBuildAdapterForSqlDataSource() {
    var ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:adapter_builder_test;DB_CLOSE_DELAY=-1");

    var station = new SqlDataStation("s", ds, Collections.<DataModelMeta>emptyList(), MigrationStrategy.AUTO, "H2");
    var builder = new SqlDataAdapterBuilder();
    var adapter = builder.apply(station);

    assertTrue(adapter.isPresent());
    assertTrue(adapter.get() instanceof SqlDataAdapter);
  }

  @Test
  void shouldReturnEmptyForNonDataSourceStation() {
    var builder = new SqlDataAdapterBuilder();
    var adapter = builder.apply(new DataStation<Object>() {
      @Override public String getName() { return "x"; }
      @Override public DataAdapter getDataAdapter() { return null; }
      @Override public Optional<DataModel> getDataModel(String modelName) { return Optional.empty(); }
      @Override public java.util.List<DataModel> getDataModelList() { return Collections.emptyList(); }
      @Override public Object getDataSource() { return new Object(); }
    });

    assertFalse(adapter.isPresent());
  }
}
